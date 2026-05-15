package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;
import com.reservaplus.reserva_plus.dto.reserva.AgendaDiaResponse;
import com.reservaplus.reserva_plus.dto.reserva.ReservaCreateRequest;
import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.exception.ForbiddenException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaHistorico;
import com.reservaplus.reserva_plus.model.ReservaHistoricoOrigem;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.BloqueioHorarioRepository;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.ReservaHistoricoRepository;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import com.reservaplus.reserva_plus.support.EmailAddressSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservaServiceImpl implements ReservaService {

    private static final int MAX_DIAS_ANTECEDENCIA_RESERVA = 7;

    private final ReservaRepository reservaRepository;
    private final ReservaHistoricoRepository reservaHistoricoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacoRepository espacoRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;

    public ReservaServiceImpl(
            ReservaRepository reservaRepository,
            ReservaHistoricoRepository reservaHistoricoRepository,
            UsuarioRepository usuarioRepository,
            EspacoRepository espacoRepository,
            BloqueioHorarioRepository bloqueioHorarioRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.reservaHistoricoRepository = reservaHistoricoRepository;
        this.usuarioRepository = usuarioRepository;
        this.espacoRepository = espacoRepository;
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
    }

    @Override
    @Transactional
    public ReservaResponse create(ReservaCreateRequest request, String userEmail) {
        LocalDate hoje = nowDate();
        LocalTime agora = nowTime();

        HorarioFuncionamentoSupport.validateHoraCheia(
                request.getHorarioInicio(),
                request.getHorarioFim(),
                "As reservas devem ser criadas em hora cheia.",
                "Horario final deve ser maior que o horario inicial."
        );
        validateDataReserva(request.getData(), request.getHorarioInicio(), hoje, agora);

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(EmailAddressSupport.normalize(userEmail))
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        Espaco espaco = espacoRepository.findByIdForUpdate(request.getEspacoId())
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));

        if (!espaco.isAtivo()) {
            throw new ConflictException("Espaco inativo. Nao e possivel reservar.");
        }

        HorarioFuncionamentoSupport.validateDentroDoHorarioFuncionamento(
                espaco,
                request.getHorarioInicio(),
                request.getHorarioFim(),
                "A reserva precisa estar dentro do horario de funcionamento do espaco: %s as %s."
        );

        boolean blocked = bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espaco.getId(),
                request.getData(),
                request.getHorarioFim(),
                request.getHorarioInicio()
        );
        if (blocked) {
            throw new ConflictException("Horario bloqueado pela administracao.");
        }

        boolean conflict = reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espaco.getId(),
                request.getData(),
                ReservaStatus.ATIVA,
                request.getHorarioFim(),
                request.getHorarioInicio()
        );
        if (conflict) {
            throw new ConflictException("Conflito de horario. Ja existe reserva nesse intervalo.");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setEspaco(espaco);
        reserva.setData(request.getData());
        reserva.setHorarioInicio(request.getHorarioInicio());
        reserva.setHorarioFim(request.getHorarioFim());
        reserva.setStatus(ReservaStatus.ATIVA);

        Reserva savedReserva = reservaRepository.save(reserva);
        registrarHistorico(savedReserva, null, ReservaStatus.ATIVA, ReservaHistoricoOrigem.CRIACAO);
        return ReservaBloqueioResponseSupport.toReservaResponse(savedReserva);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public ReservaResponse cancel(Long reservaId, String userEmail, boolean isAdmin) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new NotFoundException("Reserva nao encontrada."));

        if (!isAdmin && !reserva.getUsuario().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenException("Voce nao tem permissao para cancelar esta reserva.");
        }

        if (reserva.getStatus() == ReservaStatus.CANCELADA) {
            throw new BadRequestException("Reserva ja esta cancelada.");
        }

        ReservaHistorico historicoConclusao = concluirReservaSeNecessario(reserva, nowDate(), nowTime());
        if (historicoConclusao != null) {
            reservaRepository.save(reserva);
            reservaHistoricoRepository.save(historicoConclusao);
        }

        if (reserva.getStatus() == ReservaStatus.CONCLUIDA) {
            throw new BadRequestException("Reservas concluidas nao podem ser canceladas.");
        }

        ReservaStatus statusAnterior = reserva.getStatus();
        reserva.setStatus(ReservaStatus.CANCELADA);
        Reserva savedReserva = reservaRepository.save(reserva);
        registrarHistorico(savedReserva, statusAnterior, ReservaStatus.CANCELADA, ReservaHistoricoOrigem.CANCELAMENTO);
        return ReservaBloqueioResponseSupport.toReservaResponse(savedReserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> historico(String userEmail, boolean isAdmin, LocalDate dataInicial, LocalDate dataFinal) {
        DateRange intervalo = normalizeDateRange(dataInicial, dataFinal);

        if (isAdmin) {
            return findHistoricoAdmin(intervalo)
                    .stream()
                    .map(ReservaBloqueioResponseSupport::toReservaResponse)
                    .toList();
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(EmailAddressSupport.normalize(userEmail))
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        return findHistoricoDoUsuario(usuario.getId(), intervalo)
                .stream()
                .map(ReservaBloqueioResponseSupport::toReservaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgendaDiaResponse agendaDoDia(Long espacoId, LocalDate data) {
        AgendaDiaResponse response = new AgendaDiaResponse();
        response.setReservasAtivas(
                reservaRepository.findByEspacoIdAndDataAndStatusOrderByHorarioInicio(espacoId, data, ReservaStatus.ATIVA)
                        .stream()
                        .map(ReservaBloqueioResponseSupport::toAgendaReservaResponse)
                        .toList()
        );
        response.setBloqueios(
                bloqueioHorarioRepository.findByEspacoIdAndDataOrderByHorarioInicio(espacoId, data)
                        .stream()
                        .map(ReservaBloqueioResponseSupport::toBloqueioResponse)
                        .toList()
        );
        return response;
    }

    @Override
    @Transactional
    public int concluirReservasPendentes() {
        LocalDate hoje = nowDate();
        LocalTime agora = nowTime();

        List<Reserva> reservasPendentes = reservaRepository.findPendentesDeConclusao(ReservaStatus.ATIVA, hoje, agora);
        if (reservasPendentes.isEmpty()) {
            return 0;
        }

        List<ReservaHistorico> historicos = new ArrayList<>();
        for (Reserva reserva : reservasPendentes) {
            ReservaHistorico historico = concluirReservaSeNecessario(reserva, hoje, agora);
            if (historico != null) {
                historicos.add(historico);
            }
        }

        if (!historicos.isEmpty()) {
            reservaHistoricoRepository.saveAll(historicos);
        }

        return historicos.size();
    }

    private void validateDataReserva(LocalDate data, LocalTime inicio, LocalDate hoje, LocalTime agora) {
        if (data.isBefore(hoje)) {
            throw new BadRequestException("A data da reserva deve ser hoje ou futura.");
        }

        if (data.isAfter(hoje.plusDays(MAX_DIAS_ANTECEDENCIA_RESERVA))) {
            throw new BadRequestException(
                    String.format(
                            "As reservas podem ser feitas com no maximo %d dias de antecedencia.",
                            MAX_DIAS_ANTECEDENCIA_RESERVA
                    )
            );
        }

        if (data.isEqual(hoje) && !inicio.isAfter(agora)) {
            throw new BadRequestException("Para reservas de hoje, selecione um horario de inicio posterior ao horario atual.");
        }
    }

    private ReservaHistorico concluirReservaSeNecessario(Reserva reserva, LocalDate hoje, LocalTime agora) {
        if (reserva.getStatus() != ReservaStatus.ATIVA) {
            return null;
        }

        if (!shouldConclude(reserva, hoje, agora)) {
            return null;
        }

        ReservaStatus statusAnterior = reserva.getStatus();
        reserva.setStatus(ReservaStatus.CONCLUIDA);
        return buildHistorico(reserva, statusAnterior, ReservaStatus.CONCLUIDA, ReservaHistoricoOrigem.CONCLUSAO_AUTOMATICA);
    }

    private boolean shouldConclude(Reserva reserva, LocalDate hoje, LocalTime agora) {
        if (reserva.getData().isBefore(hoje)) {
            return true;
        }

        return reserva.getData().isEqual(hoje) && !reserva.getHorarioFim().isAfter(agora);
    }

    private void registrarHistorico(
            Reserva reserva,
            ReservaStatus statusAnterior,
            ReservaStatus statusNovo,
            ReservaHistoricoOrigem origem
    ) {
        reservaHistoricoRepository.save(buildHistorico(reserva, statusAnterior, statusNovo, origem));
    }

    private ReservaHistorico buildHistorico(
            Reserva reserva,
            ReservaStatus statusAnterior,
            ReservaStatus statusNovo,
            ReservaHistoricoOrigem origem
    ) {
        ReservaHistorico historico = new ReservaHistorico();
        historico.setReserva(reserva);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setOrigem(origem);
        return historico;
    }

    private List<Reserva> findHistoricoAdmin(DateRange intervalo) {
        if (intervalo == null) {
            return reservaRepository.findAllByOrderByDataDescHorarioInicioDesc();
        }

        return reservaRepository.findByDataBetweenOrderByDataDescHorarioInicioDesc(intervalo.start(), intervalo.end());
    }

    private List<Reserva> findHistoricoDoUsuario(Long usuarioId, DateRange intervalo) {
        if (intervalo == null) {
            return reservaRepository.findByUsuarioIdOrderByDataDescHorarioInicioDesc(usuarioId);
        }

        return reservaRepository.findByUsuarioIdAndDataBetweenOrderByDataDescHorarioInicioDesc(
                usuarioId,
                intervalo.start(),
                intervalo.end()
        );
    }

    private DateRange normalizeDateRange(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null && dataFinal == null) {
            return null;
        }

        LocalDate inicio = dataInicial != null ? dataInicial : dataFinal;
        LocalDate fim = dataFinal != null ? dataFinal : dataInicial;

        if (inicio.isAfter(fim)) {
            throw new BadRequestException("Data inicial deve ser menor ou igual a data final.");
        }

        return new DateRange(inicio, fim);
    }

    private LocalDate nowDate() {
        return AppClockSupport.nowDate();
    }

    private LocalTime nowTime() {
        return AppClockSupport.nowTime();
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
