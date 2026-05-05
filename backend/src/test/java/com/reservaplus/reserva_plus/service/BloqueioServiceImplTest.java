package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioCreateRequest;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.EspacoTipo;
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
        bloqueioService = new BloqueioServiceImpl(
                bloqueioHorarioRepository,
                espacoRepository,
                reservaRepository
        );
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
    void createShouldRejectWeeklySeriesWithoutEndDate() {
        BloqueioCreateRequest request = buildRequest(LocalDate.of(2026, 5, 5), true, null);
        given(espacoRepository.findById(1L)).willReturn(Optional.of(buildEspaco()));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> bloqueioService.create(request));

        assertEquals("Informe a data final da recorrencia semanal.", exception.getMessage());
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

        assertTrue(exception.getMessage().contains("Conflito na recorrencia em 12/05/2026."));
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

    private Espaco buildEspaco() {
        Espaco espaco = new Espaco();
        espaco.setId(1L);
        espaco.setNome("Quadra Central");
        espaco.setTipo(EspacoTipo.QUADRA);
        espaco.setAtivo(true);
        espaco.setHorarioFuncionamentoInicio(LocalTime.of(6, 0));
        espaco.setHorarioFuncionamentoFim(LocalTime.of(23, 0));
        return espaco;
    }
}
