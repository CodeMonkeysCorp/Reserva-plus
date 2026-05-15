package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.model.UserRole;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.PainelEspacoRankingProjection;
import com.reservaplus.reserva_plus.repository.PainelTipoResumoProjection;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PainelServiceImplTest {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private EspacoRepository espacoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private PainelServiceImpl painelService;

    @BeforeEach
    void setUp() {
        painelService = new PainelServiceImpl(espacoRepository, reservaRepository, usuarioRepository);
    }

    @Test
    void resumoShouldReturnAggregatedMetricsAndPagedAgenda() {
        LocalDate hoje = LocalDate.now(APP_ZONE);
        Usuario usuario = buildUsuario();
        Espaco espaco = buildEspaco();
        Reserva reserva = buildReserva(usuario, espaco, hoje.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));

        given(espacoRepository.count()).willReturn(5L);
        given(espacoRepository.countByAtivoTrue()).willReturn(4L);
        given(espacoRepository.countByDestaqueTrue()).willReturn(1L);
        given(espacoRepository.countSemImagem()).willReturn(2L);
        given(espacoRepository.countSemDescricao()).willReturn(1L);
        given(espacoRepository.summarizeByTipo()).willReturn(List.of(tipoResumo("QUADRA", 3L, 2L)));

        given(reservaRepository.countByStatus(ReservaStatus.ATIVA)).willReturn(6L);
        given(reservaRepository.countByStatus(ReservaStatus.CANCELADA)).willReturn(2L);
        given(reservaRepository.countByStatus(ReservaStatus.CONCLUIDA)).willReturn(8L);
        given(reservaRepository.countByStatusAndData(ReservaStatus.ATIVA, hoje)).willReturn(2L);
        given(reservaRepository.countReservasEmAndamento(eq(ReservaStatus.ATIVA), eq(hoje), any(LocalTime.class))).willReturn(1L);
        given(reservaRepository.countReservasFuturas(eq(ReservaStatus.ATIVA), eq(hoje), any(LocalTime.class))).willReturn(5L);
        given(reservaRepository.countDistinctUsuariosComReserva()).willReturn(3L);
        given(reservaRepository.findAgendaGeralPaginada(eq(ReservaStatus.ATIVA), eq(hoje), any(LocalTime.class), eq(PageRequest.of(0, 5))))
                .willReturn(new PageImpl<>(List.of(reserva), PageRequest.of(0, 5), 11));
        given(reservaRepository.findTopEspacosPainel(
                eq(ReservaStatus.ATIVA),
                eq(ReservaStatus.CANCELADA),
                eq(hoje),
                any(LocalTime.class),
                eq(PageRequest.of(0, 5))
        )).willReturn(List.of(ranking("Quadra Central", 7L, 4L, 4L, true)));
        given(reservaRepository.findEspacosConcorridosPainel(
                eq(ReservaStatus.ATIVA),
                eq(ReservaStatus.CANCELADA),
                eq(hoje),
                any(LocalTime.class),
                eq(PageRequest.of(0, 5))
        )).willReturn(List.of(ranking("Salao de Festas", 5L, 2L, 2L, false)));

        given(usuarioRepository.count()).willReturn(4L);
        given(usuarioRepository.countByRole(UserRole.ADMIN)).willReturn(1L);

        var response = painelService.resumo(0, 5);

        assertEquals(5L, response.espacos().totalEspacos());
        assertEquals(4L, response.espacos().espacosAtivos());
        assertEquals(1L, response.espacos().espacosInativos());
        assertEquals(6L, response.reservas().reservasAtivas());
        assertEquals(2L, response.reservas().reservasCanceladas());
        assertEquals(1L, response.usuarios().totalAdmins());
        assertEquals(1, response.agenda().items().size());
        assertEquals(11L, response.agenda().totalElements());
        assertEquals(3, response.agenda().totalPages());
        assertEquals("Quadra Central", response.topEspacos().get(0).nome());
        assertEquals("Salao de Festas", response.espacosConcorridos().get(0).nome());
        assertEquals(60L, response.tiposResumo().get(0).percentual());
    }

    @Test
    void resumoShouldRejectInvalidPageSize() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> painelService.resumo(0, 0)
        );

        assertEquals("Tamanho de página inválido.", exception.getMessage());
    }

    private Usuario buildUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNome("Administrador");
        usuario.setEmail("admin@teste.com");
        return usuario;
    }

    private Espaco buildEspaco() {
        Espaco espaco = new Espaco();
        espaco.setId(10L);
        espaco.setNome("Quadra Central");
        espaco.setTipo("QUADRA");
        espaco.setDestaque(true);
        espaco.setAtivo(true);
        return espaco;
    }

    private Reserva buildReserva(
            Usuario usuario,
            Espaco espaco,
            LocalDate data,
            LocalTime horarioInicio,
            LocalTime horarioFim
    ) {
        Reserva reserva = new Reserva();
        reserva.setId(77L);
        reserva.setUsuario(usuario);
        reserva.setEspaco(espaco);
        reserva.setData(data);
        reserva.setHorarioInicio(horarioInicio);
        reserva.setHorarioFim(horarioFim);
        reserva.setStatus(ReservaStatus.ATIVA);
        reserva.setCriadoEm(LocalDateTime.of(2026, 5, 10, 9, 0));
        return reserva;
    }

    private PainelTipoResumoProjection tipoResumo(String tipo, Long total, Long ativos) {
        return new PainelTipoResumoProjection() {
            @Override
            public String getTipo() {
                return tipo;
            }

            @Override
            public Long getTotal() {
                return total;
            }

            @Override
            public Long getAtivos() {
                return ativos;
            }
        };
    }

    private PainelEspacoRankingProjection ranking(
            String nome,
            Long totalReservas,
            Long futuras,
            Long agendaAtiva,
            Boolean destaque
    ) {
        return new PainelEspacoRankingProjection() {
            @Override
            public Long getEspacoId() {
                return 1L;
            }

            @Override
            public String getNome() {
                return nome;
            }

            @Override
            public String getTipo() {
                return "QUADRA";
            }

            @Override
            public Boolean getDestaque() {
                return destaque;
            }

            @Override
            public Long getTotalReservas() {
                return totalReservas;
            }

            @Override
            public Long getFuturas() {
                return futuras;
            }

            @Override
            public Long getAgendaAtiva() {
                return agendaAtiva;
            }
        };
    }
}
