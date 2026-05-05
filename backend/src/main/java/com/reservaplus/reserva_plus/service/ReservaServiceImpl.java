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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservaServiceImpl implements ReservaService {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_DIAS_ANTECEDENCIA_RESERVA = 7;
    private static final LocalTime HORARIO_PADRAO_INICIO = LocalTime.of(6, 0);
    private static final LocalTime HORARIO_PADRAO_FIM = LocalTime.of(23, 0);

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

        validateHorario(request.getHorarioInicio(), request.getHorarioFim());
        validateDataReserva(request.getData(), request.getHorarioInicio(), hoje, agora);

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        Espaco espaco = espacoRepository.findByIdForUpdate(request.getEspacoId())
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));

        if (!espaco.isAtivo()) {
            throw new ConflictException("Espaco inativo. Nao e possivel reservar.");
        }

        validateHorarioFuncionamento(espaco, request.getHorarioInicio(), request.getHorarioFim());

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
        return toResponse(savedReserva);
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
        return toResponse(savedReserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> historico(String userEmail, boolean isAdmin, LocalDate data) {
        if (isAdmin) {
            if (data != null) {
                return reservaRepository.findByData(data)
                        .stream()
                        .sorted(historicoDoDiaComparator())
                        .map(this::toResponse)
                        .toList();
            }

            return reservaRepository.findAllByOrderByDataDescHorarioInicioDesc()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        if (data != null) {
            return reservaRepository.findByUsuarioIdAndData(usuario.getId(), data)
                    .stream()
                    .sorted(historicoDoDiaComparator())
                    .map(this::toResponse)
                    .toList();
        }

        return reservaRepository.findByUsuarioIdOrderByDataDescHorarioInicioDesc(usuario.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgendaDiaResponse agendaDoDia(Long espacoId, LocalDate data) {
        AgendaDiaResponse response = new AgendaDiaResponse();
        response.setReservasAtivas(
                reservaRepository.findByEspacoIdAndDataAndStatusOrderByHorarioInicio(espacoId, data, ReservaStatus.ATIVA)
                        .stream()
                        .map(this::toAgendaResponse)
                        .toList()
        );
        response.setBloqueios(
                bloqueioHorarioRepository.findByEspacoIdAndDataOrderByHorarioInicio(espacoId, data)
                        .stream()
                        .map(this::toBloqueioResponse)
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

    private ReservaResponse toResponse(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setId(reserva.getId());
        response.setUsuarioId(reserva.getUsuario().getId());
        response.setUsuarioNome(reserva.getUsuario().getNome());
        response.setEspacoId(reserva.getEspaco().getId());
        response.setEspacoNome(reserva.getEspaco().getNome());
        response.setData(reserva.getData());
        response.setHorarioInicio(reserva.getHorarioInicio());
        response.setHorarioFim(reserva.getHorarioFim());
        response.setStatus(reserva.getStatus());
        response.setCriadoEm(reserva.getCriadoEm());
        return response;
    }

    private ReservaResponse toAgendaResponse(Reserva reserva) {
        ReservaResponse response = toResponse(reserva);
        response.setUsuarioId(null);
        response.setUsuarioNome(null);
        return response;
    }

    private BloqueioResponse toBloqueioResponse(BloqueioHorario bloqueio) {
        BloqueioResponse response = new BloqueioResponse();
        response.setId(bloqueio.getId());
        response.setEspacoId(bloqueio.getEspaco().getId());
        response.setEspacoNome(bloqueio.getEspaco().getNome());
        response.setData(bloqueio.getData());
        response.setHorarioInicio(bloqueio.getHorarioInicio());
        response.setHorarioFim(bloqueio.getHorarioFim());
        response.setMotivo(bloqueio.getMotivo());
        return response;
    }

    private void validateHorario(LocalTime inicio, LocalTime fim) {
        if (inicio.getMinute() != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0
                || fim.getMinute() != 0 || fim.getSecond() != 0 || fim.getNano() != 0) {
            throw new BadRequestException("As reservas devem ser criadas em hora cheia.");
        }

        if (!fim.isAfter(inicio)) {
            throw new BadRequestException("Horario final deve ser maior que o horario inicial.");
        }
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

    private void validateHorarioFuncionamento(Espaco espaco, LocalTime inicio, LocalTime fim) {
        LocalTime funcionamentoInicio = resolveHorarioInicio(espaco);
        LocalTime funcionamentoFim = resolveHorarioFim(espaco);

        if (inicio.isBefore(funcionamentoInicio) || fim.isAfter(funcionamentoFim)) {
            throw new BadRequestException(
                    String.format(
                            "A reserva precisa estar dentro do horario de funcionamento do espaco: %s as %s.",
                            funcionamentoInicio,
                            funcionamentoFim
                    )
            );
        }
    }

    private Comparator<Reserva> historicoDoDiaComparator() {
        return Comparator.comparing(Reserva::getHorarioInicio)
                .thenComparing((Reserva reserva) -> reserva.getEspaco().getNome(), String.CASE_INSENSITIVE_ORDER);
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

    private LocalDate nowDate() {
        return LocalDate.now(APP_ZONE);
    }

    private LocalTime nowTime() {
        return LocalTime.now(APP_ZONE).withSecond(0).withNano(0);
    }

    private LocalTime resolveHorarioInicio(Espaco espaco) {
        return espaco.getHorarioFuncionamentoInicio() != null ? espaco.getHorarioFuncionamentoInicio() : HORARIO_PADRAO_INICIO;
    }

    private LocalTime resolveHorarioFim(Espaco espaco) {
        return espaco.getHorarioFuncionamentoFim() != null ? espaco.getHorarioFuncionamentoFim() : HORARIO_PADRAO_FIM;
    }
}
