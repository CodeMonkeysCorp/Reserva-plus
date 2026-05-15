package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioCreateRequest;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioRecorrenteResponse;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.repository.BloqueioHorarioRepository;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BloqueioServiceImplTest {

    @Mock
    private BloqueioHorarioRepository bloqueioHorarioRepository;

    @Mock
    private EspacoRepository espacoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    private BloqueioServiceImpl bloqueioService;

    @BeforeEach
    void setUp() {
        bloqueioService = buildServiceAt(LocalDate.of(2026, 5, 1), LocalTime.of(10, 0));
    }

    @Test
    void createShouldCreateWeeklySeries() {
        LocalDate inicio = LocalDate.of(2026, 5, 5);
        BloqueioCreateRequest request = buildRequest(inicio, true, LocalDate.of(2026, 5, 19));
        Espaco espaco = buildEspaco();

        given(espacoRepository.findById(1L)).willReturn(Optional.of(espaco));
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 5)), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 12)), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 19)), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 5)), eq(ReservaStatus.ATIVA), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 12)), eq(ReservaStatus.ATIVA), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 19)), eq(ReservaStatus.ATIVA), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(bloqueioHorarioRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<BloqueioHorario> bloqueios = invocation.getArgument(0);
            long id = 100L;
            for (BloqueioHorario bloqueio : bloqueios) {
                bloqueio.setId(id++);
            }
            return bloqueios;
        });

        BloqueioResponse response = bloqueioService.create(request);

        assertEquals(LocalDate.of(2026, 5, 5), response.getData());
        assertNotNull(response.getSerieRecorrenciaId());
        verify(bloqueioHorarioRepository).saveAll(anyList());
    }

    @Test
    void createShouldRejectPastTimeForToday() {
        LocalDate hoje = LocalDate.of(2026, 5, 13);
        bloqueioService = buildServiceAt(hoje, LocalTime.of(13, 30));

        BloqueioCreateRequest request = buildRequest(hoje, false, null);
        request.setHorarioInicio(LocalTime.of(12, 0));
        request.setHorarioFim(LocalTime.of(13, 0));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> bloqueioService.create(request));

        assertEquals("Para bloqueios de hoje, selecione um horario que ainda nao tenha terminado.", exception.getMessage());
        verifyNoInteractions(espacoRepository, bloqueioHorarioRepository, reservaRepository);
    }

    @Test
    void createShouldAllowCurrentTimeSlotForToday() {
        LocalDate hoje = LocalDate.of(2026, 5, 13);
        bloqueioService = buildServiceAt(hoje, LocalTime.of(13, 30));

        BloqueioCreateRequest request = buildRequest(hoje, false, null);
        request.setHorarioInicio(LocalTime.of(13, 0));
        request.setHorarioFim(LocalTime.of(14, 0));

        Espaco espaco = buildEspaco();
        given(espacoRepository.findById(1L)).willReturn(Optional.of(espaco));
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(hoje), eq(LocalTime.of(14, 0)), eq(LocalTime.of(13, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(hoje), eq(ReservaStatus.ATIVA), eq(LocalTime.of(14, 0)), eq(LocalTime.of(13, 0))
        )).willReturn(false);
        given(bloqueioHorarioRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<BloqueioHorario> bloqueios = invocation.getArgument(0);
            bloqueios.get(0).setId(100L);
            return bloqueios;
        });

        BloqueioResponse response = bloqueioService.create(request);

        assertEquals(100L, response.getId());
        assertEquals(hoje, response.getData());
        assertEquals(LocalTime.of(13, 0), response.getHorarioInicio());
        assertEquals(LocalTime.of(14, 0), response.getHorarioFim());
        verify(bloqueioHorarioRepository).saveAll(anyList());
    }

    @Test
    void createShouldRejectWeeklySeriesWithoutEndDate() {
        BloqueioCreateRequest request = buildRequest(LocalDate.of(2026, 5, 5), true, null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> bloqueioService.create(request));

        assertEquals("Informe a data final da recorrência semanal.", exception.getMessage());
    }

    @Test
    void createShouldRejectSeriesWhenAnyOccurrenceConflictsWithReservation() {
        LocalDate inicio = LocalDate.of(2026, 5, 5);
        BloqueioCreateRequest request = buildRequest(inicio, true, LocalDate.of(2026, 5, 19));
        Espaco espaco = buildEspaco();

        given(espacoRepository.findById(1L)).willReturn(Optional.of(espaco));
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 5)), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 12)), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 5)), eq(ReservaStatus.ATIVA), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(false);
        given(reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                eq(1L), eq(LocalDate.of(2026, 5, 12)), eq(ReservaStatus.ATIVA), eq(LocalTime.of(10, 0)), eq(LocalTime.of(8, 0))
        )).willReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> bloqueioService.create(request));

        assertTrue(exception.getMessage().contains("Conflito na recorrência em 12/05/2026."));
        verify(bloqueioHorarioRepository, never()).saveAll(anyList());
    }

    @Test
    void deleteSerieShouldRemoveAllOccurrencesWhenSeriesExists() {
        BloqueioHorario origem = new BloqueioHorario();
        origem.setId(10L);
        origem.setSerieRecorrenciaId("serie-123");

        BloqueioHorario outro = new BloqueioHorario();
        outro.setId(11L);
        outro.setSerieRecorrenciaId("serie-123");

        given(bloqueioHorarioRepository.findById(10L)).willReturn(Optional.of(origem));
        given(bloqueioHorarioRepository.findBySerieRecorrenciaId("serie-123")).willReturn(List.of(origem, outro));

        bloqueioService.deleteSerie(10L);

        verify(bloqueioHorarioRepository).deleteAll(List.of(origem, outro));
    }

    @Test
    void findRecurringByEspacoShouldGroupSeries() {
        Espaco espaco = buildEspaco();

        BloqueioHorario serieA1 = buildRecurringBloqueio(100L, espaco, LocalDate.of(2026, 5, 5), "08:00", "10:00", "Limpeza", "serie-a");
        BloqueioHorario serieA2 = buildRecurringBloqueio(101L, espaco, LocalDate.of(2026, 5, 12), "08:00", "10:00", "Limpeza", "serie-a");
        BloqueioHorario serieB1 = buildRecurringBloqueio(200L, espaco, LocalDate.of(2026, 5, 7), "18:00", "20:00", "Evento interno", "serie-b");
        BloqueioHorario serieB2 = buildRecurringBloqueio(201L, espaco, LocalDate.of(2026, 5, 14), "18:00", "20:00", "Evento interno", "serie-b");

        given(bloqueioHorarioRepository.findByEspacoIdAndSerieRecorrenciaIdIsNotNullOrderByDataAscHorarioInicioAsc(1L))
                .willReturn(List.of(serieA1, serieB1, serieA2, serieB2));

        List<BloqueioRecorrenteResponse> response = bloqueioService.findRecurringByEspaco(1L);

        assertEquals(2, response.size());
        assertEquals("serie-a", response.get(0).getSerieRecorrenciaId());
        assertEquals(LocalDate.of(2026, 5, 5), response.get(0).getDataInicio());
        assertEquals(LocalDate.of(2026, 5, 12), response.get(0).getDataFim());
        assertEquals(2, response.get(0).getTotalOcorrencias());
        assertEquals("serie-b", response.get(1).getSerieRecorrenciaId());
        assertEquals(LocalDate.of(2026, 5, 7), response.get(1).getDataInicio());
        assertEquals(LocalDate.of(2026, 5, 14), response.get(1).getDataFim());
    }

    private BloqueioCreateRequest buildRequest(LocalDate data, boolean recorrenteSemanal, LocalDate dataFimRecorrencia) {
        BloqueioCreateRequest request = new BloqueioCreateRequest();
        request.setEspacoId(1L);
        request.setData(data);
        request.setHorarioInicio(LocalTime.of(8, 0));
        request.setHorarioFim(LocalTime.of(10, 0));
        request.setMotivo("Limpeza");
        request.setRecorrenteSemanal(recorrenteSemanal);
        request.setDataFimRecorrencia(dataFimRecorrencia);
        return request;
    }

    private BloqueioServiceImpl buildServiceAt(LocalDate hoje, LocalTime agora) {
        return new BloqueioServiceImpl(
                bloqueioHorarioRepository,
                espacoRepository,
                reservaRepository
        ) {
            @Override
            LocalDate nowDate() {
                return hoje;
            }

            @Override
            LocalTime nowTime() {
                return agora;
            }
        };
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

    private BloqueioHorario buildRecurringBloqueio(
            Long id,
            Espaco espaco,
            LocalDate data,
            String horarioInicio,
            String horarioFim,
            String motivo,
            String serieRecorrenciaId
    ) {
        BloqueioHorario bloqueio = new BloqueioHorario();
        bloqueio.setId(id);
        bloqueio.setEspaco(espaco);
        bloqueio.setData(data);
        bloqueio.setHorarioInicio(LocalTime.parse(horarioInicio));
        bloqueio.setHorarioFim(LocalTime.parse(horarioFim));
        bloqueio.setMotivo(motivo);
        bloqueio.setSerieRecorrenciaId(serieRecorrenciaId);
        return bloqueio;
    }
}
