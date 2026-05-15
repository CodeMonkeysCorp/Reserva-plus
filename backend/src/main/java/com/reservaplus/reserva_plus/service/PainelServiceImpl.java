package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.painel.PainelAgendaItemResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelAgendaPageResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelEspacoMetricasResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelRankingEspacoResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelReservaMetricasResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelResumoResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelTipoResumoResponse;
import com.reservaplus.reserva_plus.dto.painel.PainelUsuarioMetricasResponse;
import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.model.UserRole;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.PainelEspacoRankingProjection;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class PainelServiceImpl implements PainelService {

    private static final int MAX_PAGE_SIZE = 20;
    private static final int RANKING_PAGE_SIZE = 5;

    private final EspacoRepository espacoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    public PainelServiceImpl(
            EspacoRepository espacoRepository,
            ReservaRepository reservaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.espacoRepository = espacoRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PainelResumoResponse resumo(int page, int size) {
        validatePagination(page, size);

        LocalDate hoje = AppClockSupport.nowDate();
        LocalTime agora = AppClockSupport.nowTime();

        long totalEspacos = espacoRepository.count();
        long espacosAtivos = espacoRepository.countByAtivoTrue();
        long espacosInativos = Math.max(totalEspacos - espacosAtivos, 0);
        long espacosDestacados = espacoRepository.countByDestaqueTrue();
        long espacosSemImagem = espacoRepository.countSemImagem();
        long espacosSemDescricao = espacoRepository.countSemDescricao();

        long reservasAtivas = reservaRepository.countByStatus(ReservaStatus.ATIVA);
        long reservasCanceladas = reservaRepository.countByStatus(ReservaStatus.CANCELADA);
        long reservasConcluidas = reservaRepository.countByStatus(ReservaStatus.CONCLUIDA);
        long reservasHoje = reservaRepository.countByStatusAndData(ReservaStatus.ATIVA, hoje);
        long reservasEmAndamento = reservaRepository.countReservasEmAndamento(ReservaStatus.ATIVA, hoje, agora);
        long reservasFuturas = reservaRepository.countReservasFuturas(ReservaStatus.ATIVA, hoje, agora);

        long totalUsuarios = usuarioRepository.count();
        long totalAdmins = usuarioRepository.countByRole(UserRole.ADMIN);
        long usuariosComReserva = reservaRepository.countDistinctUsuariosComReserva();
        long usuariosSemReserva = Math.max(totalUsuarios - usuariosComReserva, 0);

        List<PainelTipoResumoResponse> tiposResumo = mapTiposResumo(totalEspacos);
        PainelAgendaPageResponse agenda = mapAgenda(page, size, hoje, agora);
        List<PainelRankingEspacoResponse> topEspacos = mapRankingTopEspacos(hoje, agora);
        List<PainelRankingEspacoResponse> espacosConcorridos = mapEspacosConcorridos(hoje, agora);

        return new PainelResumoResponse(
                new PainelEspacoMetricasResponse(
                        totalEspacos,
                        espacosAtivos,
                        espacosInativos,
                        espacosDestacados,
                        espacosSemImagem,
                        espacosSemDescricao
                ),
                new PainelReservaMetricasResponse(
                        reservasAtivas,
                        reservasCanceladas,
                        reservasConcluidas,
                        reservasHoje,
                        reservasEmAndamento,
                        reservasFuturas
                ),
                new PainelUsuarioMetricasResponse(
                        totalUsuarios,
                        totalAdmins,
                        usuariosComReserva,
                        usuariosSemReserva
                ),
                agenda,
                tiposResumo,
                topEspacos,
                espacosConcorridos
        );
    }

    private List<PainelTipoResumoResponse> mapTiposResumo(long totalEspacos) {
        return espacoRepository.summarizeByTipo()
                .stream()
                .map((item) -> new PainelTipoResumoResponse(
                        item.getTipo(),
                        zeroIfNull(item.getTotal()),
                        zeroIfNull(item.getAtivos()),
                        totalEspacos > 0 ? Math.round((zeroIfNull(item.getTotal()) * 100.0) / totalEspacos) : 0
                ))
                .toList();
    }

    private PainelAgendaPageResponse mapAgenda(int page, int size, LocalDate hoje, LocalTime agora) {
        Page<Reserva> agendaPage = reservaRepository.findAgendaGeralPaginada(
                ReservaStatus.ATIVA,
                hoje,
                agora,
                PageRequest.of(page, size)
        );

        List<PainelAgendaItemResponse> items = agendaPage.getContent()
                .stream()
                .map((reserva) -> new PainelAgendaItemResponse(
                        ReservaBloqueioResponseSupport.toReservaResponse(reserva),
                        isReservaEmAndamento(reserva, hoje, agora)
                ))
                .toList();

        return new PainelAgendaPageResponse(
                items,
                agendaPage.getNumber(),
                agendaPage.getSize(),
                agendaPage.getTotalElements(),
                agendaPage.getTotalPages()
        );
    }

    private List<PainelRankingEspacoResponse> mapRankingTopEspacos(LocalDate hoje, LocalTime agora) {
        return reservaRepository.findTopEspacosPainel(
                ReservaStatus.ATIVA,
                ReservaStatus.CANCELADA,
                hoje,
                agora,
                PageRequest.of(0, RANKING_PAGE_SIZE)
        )
                .stream()
                .map(this::toRankingResponse)
                .toList();
    }

    private List<PainelRankingEspacoResponse> mapEspacosConcorridos(LocalDate hoje, LocalTime agora) {
        return reservaRepository.findEspacosConcorridosPainel(
                ReservaStatus.ATIVA,
                ReservaStatus.CANCELADA,
                hoje,
                agora,
                PageRequest.of(0, RANKING_PAGE_SIZE)
        )
                .stream()
                .map(this::toRankingResponse)
                .toList();
    }

    private PainelRankingEspacoResponse toRankingResponse(PainelEspacoRankingProjection projection) {
        return new PainelRankingEspacoResponse(
                zeroIfNull(projection.getEspacoId()),
                projection.getNome(),
                projection.getTipo(),
                zeroIfNull(projection.getTotalReservas()),
                zeroIfNull(projection.getFuturas()),
                zeroIfNull(projection.getAgendaAtiva()),
                Boolean.TRUE.equals(projection.getDestaque())
        );
    }

    private boolean isReservaEmAndamento(Reserva reserva, LocalDate hoje, LocalTime agora) {
        return reserva.getData().isEqual(hoje)
                && !reserva.getHorarioInicio().isAfter(agora)
                && reserva.getHorarioFim().isAfter(agora);
    }

    private long zeroIfNull(Long value) {
        return value != null ? value : 0;
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Página deve ser maior ou igual a zero.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Tamanho de página inválido.");
        }
    }
}
