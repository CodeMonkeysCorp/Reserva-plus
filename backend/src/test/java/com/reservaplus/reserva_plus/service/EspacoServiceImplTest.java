package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.config.R2StorageProperties;
import com.reservaplus.reserva_plus.dto.espaco.EspacoRequest;
import com.reservaplus.reserva_plus.dto.espaco.EspacoResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EspacoServiceImplTest {

    @Mock
    private EspacoRepository espacoRepository;

    @Mock
    private StorageService storageService;

    private EspacoServiceImpl espacoService;

    @BeforeEach
    void setUp() {
        R2StorageProperties properties = new R2StorageProperties();
        properties.setObjectKeyPrefix("reserva-plus");
        properties.setMaxFileSize(DataSize.ofMegabytes(10));

        espacoService = new EspacoServiceImpl(espacoRepository, storageService, properties);
    }

    @Test
    void createShouldPersistImageObjectKeyAndExposePublicUrl() {
        EspacoRequest request = buildRequest();
        request.setImagemObjectKey("reserva-plus/espacos/imagem-1.png");
        request.setDestaque(true);

        given(espacoRepository.save(any(Espaco.class))).willAnswer(invocation -> {
            Espaco espaco = invocation.getArgument(0);
            espaco.setId(5L);
            return espaco;
        });
        given(storageService.resolvePublicUrl("reserva-plus/espacos/imagem-1.png"))
                .willReturn("https://cdn.exemplo.com/reserva-plus/espacos/imagem-1.png");

        EspacoResponse response = espacoService.create(request);

        ArgumentCaptor<Espaco> captor = ArgumentCaptor.forClass(Espaco.class);
        verify(espacoRepository).save(captor.capture());
        assertEquals("reserva-plus/espacos/imagem-1.png", captor.getValue().getImagemObjectKey());
        assertEquals(true, captor.getValue().isDestaque());
        assertEquals("reserva-plus/espacos/imagem-1.png", response.getImagemObjectKey());
        assertEquals("https://cdn.exemplo.com/reserva-plus/espacos/imagem-1.png", response.getImagemUrl());
        assertEquals(true, response.isDestaque());
    }

    @Test
    void updateShouldDeletePreviousImageWhenObjectKeyChanges() {
        EspacoRequest request = buildRequest();
        request.setImagemObjectKey("reserva-plus/espacos/imagem-nova.png");

        Espaco espaco = buildEspaco();
        espaco.setImagemObjectKey("reserva-plus/espacos/imagem-antiga.png");

        given(espacoRepository.findById(7L)).willReturn(Optional.of(espaco));
        given(espacoRepository.save(any(Espaco.class))).willAnswer(invocation -> invocation.getArgument(0));

        espacoService.update(7L, request);

        verify(storageService).delete("reserva-plus/espacos/imagem-antiga.png");
    }

    @Test
    void updateShouldAllowRemovingCommittedImage() {
        EspacoRequest request = buildRequest();
        request.setImagemObjectKey(null);

        Espaco espaco = buildEspaco();
        espaco.setImagemObjectKey("reserva-plus/espacos/imagem-antiga.png");

        given(espacoRepository.findById(7L)).willReturn(Optional.of(espaco));
        given(espacoRepository.save(any(Espaco.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(storageService.resolvePublicUrl(null)).willReturn(null);

        EspacoResponse response = espacoService.update(7L, request);

        assertNull(response.getImagemObjectKey());
        verify(storageService).delete("reserva-plus/espacos/imagem-antiga.png");
    }

    @Test
    void createShouldRejectImageOutsideEspacosPrefix() {
        EspacoRequest request = buildRequest();
        request.setImagemObjectKey("reserva-plus/outro/arquivo.png");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> espacoService.create(request));

        assertEquals("A imagem informada nao pertence ao armazenamento de espacos.", exception.getMessage());
        verify(espacoRepository, never()).save(any(Espaco.class));
    }

    private EspacoRequest buildRequest() {
        EspacoRequest request = new EspacoRequest();
        request.setNome("Quadra Coberta");
        request.setTipo("QUADRA");
        request.setDescricao("Descricao");
        request.setAtivo(true);
        request.setDestaque(false);
        request.setHorarioFuncionamentoInicio(LocalTime.of(8, 0));
        request.setHorarioFuncionamentoFim(LocalTime.of(22, 0));
        return request;
    }

    private Espaco buildEspaco() {
        Espaco espaco = new Espaco();
        espaco.setId(7L);
        espaco.setNome("Quadra Coberta");
        espaco.setTipo("QUADRA");
        espaco.setDescricao("Descricao");
        espaco.setAtivo(true);
        espaco.setDestaque(false);
        espaco.setHorarioFuncionamentoInicio(LocalTime.of(8, 0));
        espaco.setHorarioFuncionamentoFim(LocalTime.of(22, 0));
        return espaco;
    }
}
