package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.reserva.ReservaCreateRequest;
import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaHistorico;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.BloqueioHorarioRepository;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.ReservaHistoricoRepository;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ReservaHistoricoRepository reservaHistoricoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EspacoRepository espacoRepository;

    @Mock
    private BloqueioHorarioRepository bloqueioHorarioRepository;

    private ReservaServiceImpl reservaService;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaServiceImpl(
                reservaRepository,
                reservaHistoricoRepository,
                usuarioRepository,
                espacoRepository,
                bloqueioHorarioRepository
        );
    }

    @Test
    void createShouldRejectPastTimeForToday() {
        LocalDate hoje = LocalDate.now(APP_ZONE);
        LocalTime agora = LocalTime.now(APP_ZONE).withSecond(0).withNano(0);
        int inicioHora = agora.getHour() == 23 ? 22 : agora.getHour();

        ReservaCreateRequest request = buildRequest(
                1L,
                hoje,
                LocalTime.of(inicioHora, 0),
                LocalTime.of(inicioHora + 1, 0)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reservaService.create(request, "morador@teste.com")
        );

        assertEquals("Para reservas de hoje, selecione um horario de inicio posterior ao horario atual.", exception.getMessage());
        verifyNoInteractions(usuarioRepository, espacoRepository, bloqueioHorarioRepository, reservaRepository, reservaHistoricoRepository);
    }

    @Test
    void createShouldRejectDatesBeyondSevenDays() {
        ReservaCreateRequest request = buildRequest(
                1L,
                LocalDate.now(APP_ZONE).plusDays(8),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reservaService.create(request, "morador@teste.com")
        );

        assertEquals("As reservas podem ser feitas com no maximo 7 dias de antecedencia.", exception.getMessage());
        verifyNoInteractions(usuarioRepository, espacoRepository, bloqueioHorarioRepository, reservaRepository, reservaHistoricoRepository);
    }

    @Test
    void createShouldAllowReservationsWithinSevenDayWindow() {
        LocalDate dataReserva = LocalDate.now(APP_ZONE).plusDays(7);
        ReservaCreateRequest request = buildRequest(1L, dataReserva, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Usuario usuario = buildUsuario();
        Espaco espaco = buildEspaco();

        given(usuarioRepository.findByEmailIgnoreCase("morador@teste.com")).willReturn(Optional.of(usuario));
        given(espacoRepository.findByIdForUpdate(1L)).willReturn(Optional.of(espaco));
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L),
                eq(dataReserva),
                eq(LocalTime.of(11, 0)),
                eq(LocalTime.of(10, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L),
                eq(dataReserva),
                eq(ReservaStatus.ATIVA),
                eq(LocalTime.of(11, 0)),
                eq(LocalTime.of(10, 0))
        )).willReturn(false);
        given(reservaRepository.save(any(Reserva.class))).willAnswer(invocation -> {
            Reserva reserva = invocation.getArgument(0);
            reserva.setId(99L);
            reserva.setCriadoEm(LocalDateTime.of(2026, 5, 5, 10, 0));
            return reserva;
        });
        given(reservaHistoricoRepository.save(any(ReservaHistorico.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReservaResponse response = reservaService.create(request, "morador@teste.com");

        assertEquals(99L, response.getId());
        assertEquals(dataReserva, response.getData());
        assertEquals(LocalTime.of(10, 0), response.getHorarioInicio());
        assertEquals(LocalTime.of(11, 0), response.getHorarioFim());
        assertEquals(ReservaStatus.ATIVA, response.getStatus());
        verify(reservaRepository).save(any(Reserva.class));
        verify(reservaHistoricoRepository).save(any(ReservaHistorico.class));
    }

    @Test
    void agendaDoDiaShouldPreserveRecurringSeriesIdInBlocks() {
        LocalDate dataAgenda = LocalDate.of(2026, 5, 12);
        Espaco espaco = buildEspaco();

        BloqueioHorario bloqueio = new BloqueioHorario();
        bloqueio.setId(55L);
        bloqueio.setEspaco(espaco);
        bloqueio.setData(dataAgenda);
        bloqueio.setHorarioInicio(LocalTime.of(19, 0));
        bloqueio.setHorarioFim(LocalTime.of(20, 0));
        bloqueio.setMotivo("Manutencao");
        bloqueio.setSerieRecorrenciaId("serie-123");

        given(reservaRepository.findByEspacoIdAndDataAndStatusOrderByHorarioInicio(1L, dataAgenda, ReservaStatus.ATIVA))
                .willReturn(List.of());
        given(bloqueioHorarioRepository.findByEspacoIdAndDataOrderByHorarioInicio(1L, dataAgenda))
                .willReturn(List.of(bloqueio));

        var response = reservaService.agendaDoDia(1L, dataAgenda);

        assertEquals(1, response.getBloqueios().size());
        assertEquals("serie-123", response.getBloqueios().get(0).getSerieRecorrenciaId());
    }

    @Test
    void historicoShouldFilterByDateRangeForUser() {
        LocalDate dataInicial = LocalDate.of(2026, 5, 13);
        LocalDate dataFinal = LocalDate.of(2026, 5, 14);
        Usuario usuario = buildUsuario();
        Espaco espaco = buildEspaco();
        Reserva reserva = buildReserva(usuario, espaco, LocalDate.of(2026, 5, 14), LocalTime.of(18, 0), LocalTime.of(19, 0), ReservaStatus.ATIVA);

        given(usuarioRepository.findByEmailIgnoreCase("morador@teste.com")).willReturn(Optional.of(usuario));
        given(reservaRepository.findByUsuarioIdAndDataBetweenOrderByDataDescHorarioInicioDesc(usuario.getId(), dataInicial, dataFinal))
                .willReturn(List.of(reserva));

        List<ReservaResponse> response = reservaService.historico("morador@teste.com", false, dataInicial, dataFinal);

        assertEquals(1, response.size());
        assertEquals(reserva.getData(), response.get(0).getData());
        assertEquals(reserva.getHorarioInicio(), response.get(0).getHorarioInicio());
        verify(reservaRepository).findByUsuarioIdAndDataBetweenOrderByDataDescHorarioInicioDesc(usuario.getId(), dataInicial, dataFinal);
    }

    @Test
    void historicoShouldRejectInvertedDateRange() {
        LocalDate dataInicial = LocalDate.of(2026, 5, 14);
        LocalDate dataFinal = LocalDate.of(2026, 5, 13);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reservaService.historico("admin@teste.com", true, dataInicial, dataFinal)
        );

        assertEquals("Data inicial deve ser menor ou igual a data final.", exception.getMessage());
        verifyNoInteractions(usuarioRepository, reservaRepository);
    }

    private ReservaCreateRequest buildRequest(Long espacoId, LocalDate data, LocalTime horarioInicio, LocalTime horarioFim) {
        ReservaCreateRequest request = new ReservaCreateRequest();
        request.setEspacoId(espacoId);
        request.setData(data);
        request.setHorarioInicio(horarioInicio);
        request.setHorarioFim(horarioFim);
        return request;
    }

    private Usuario buildUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Morador");
        usuario.setEmail("morador@teste.com");
        return usuario;
    }

    private Espaco buildEspaco() {
        Espaco espaco = new Espaco();
        espaco.setId(1L);
        espaco.setNome("Quadra Central");
        espaco.setTipo("QUADRA");
        espaco.setAtivo(true);
        espaco.setHorarioFuncionamentoInicio(LocalTime.of(6, 0));
        espaco.setHorarioFuncionamentoFim(LocalTime.of(23, 0));
        return espaco;
    }

    private Reserva buildReserva(
            Usuario usuario,
            Espaco espaco,
            LocalDate data,
            LocalTime horarioInicio,
            LocalTime horarioFim,
            ReservaStatus status
    ) {
        Reserva reserva = new Reserva();
        reserva.setId(15L);
        reserva.setUsuario(usuario);
        reserva.setEspaco(espaco);
        reserva.setData(data);
        reserva.setHorarioInicio(horarioInicio);
        reserva.setHorarioFim(horarioFim);
        reserva.setStatus(status);
        reserva.setCriadoEm(LocalDateTime.of(2026, 5, 10, 9, 0));
        return reserva;
    }
}
