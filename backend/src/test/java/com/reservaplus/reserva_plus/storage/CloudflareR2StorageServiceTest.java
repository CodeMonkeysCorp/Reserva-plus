package com.reservaplus.reserva_plus.storage;

import com.reservaplus.reserva_plus.config.R2StorageProperties;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CloudflareR2StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private CloudflareR2StorageService storageService;

    @BeforeEach
    void setUp() {
        R2StorageProperties properties = new R2StorageProperties();
        properties.setBucket("reserva-imagens");
        properties.setObjectKeyPrefix("reserva-plus");
        properties.setPublicBaseUrl("https://cdn.exemplo.com");
        properties.setMaxFileSize(DataSize.ofMegabytes(5));

        storageService = new CloudflareR2StorageService(s3Client, properties);
    }

    @Test
    void uploadShouldSendObjectToR2AndReturnMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "arquivo",
                "quadra-principal.png",
                " IMAGE/PNG ",
                "conteudo".getBytes(StandardCharsets.UTF_8)
        );

        StoredObject storedObject = storageService.upload("espacos", file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest request = requestCaptor.getValue();
        RequestBody requestBody = bodyCaptor.getValue();
        assertEquals("reserva-imagens", request.bucket());
        assertEquals("image/png", request.contentType());
        assertTrue(request.key().startsWith("reserva-plus/espacos/"));
        assertTrue(request.key().endsWith(".png"));
        assertEquals("conteudo", readBody(requestBody));

        assertEquals(request.key(), storedObject.getObjectKey());
        assertEquals("quadra-principal.png", storedObject.getOriginalFilename());
        assertEquals("image/png", storedObject.getContentType());
        assertEquals(file.getSize(), storedObject.getSize());
        assertEquals("https://cdn.exemplo.com/" + request.key(), storedObject.getPublicUrl());
    }

    @Test
    void propertiesShouldNormalizeBucketEndpointAndPublicBaseUrl() {
        R2StorageProperties properties = new R2StorageProperties();
        properties.setBucket("  reserva-imagens  ");
        properties.setEndpoint(" https://abc.r2.cloudflarestorage.com ");
        properties.setPublicBaseUrl(" https://cdn.exemplo.com/ ");

        assertEquals("reserva-imagens", properties.resolveBucket());
        assertEquals("https://abc.r2.cloudflarestorage.com", properties.resolveEndpoint());
        assertEquals("https://cdn.exemplo.com", properties.resolvePublicBaseUrl());
    }

    @Test
    void uploadShouldRejectUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "arquivo",
                "planta.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(BadRequestException.class, () -> storageService.upload("espacos", file));

        assertEquals("Formato de imagem nao suportado. Use JPG, PNG ou WEBP.", exception.getMessage());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void deleteShouldRejectKeysOutsideConfiguredPrefix() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> storageService.delete("outro-prefixo/arquivo.png"));

        assertEquals("A chave informada nao pertence ao prefixo configurado para o sistema.", exception.getMessage());
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void resolvePublicUrlShouldReturnNullWhenNoBaseUrlExists() {
        R2StorageProperties properties = new R2StorageProperties();
        properties.setBucket("reserva-imagens");
        properties.setObjectKeyPrefix("reserva-plus");
        properties.setMaxFileSize(DataSize.ofMegabytes(5));

        CloudflareR2StorageService serviceWithoutPublicUrl = new CloudflareR2StorageService(s3Client, properties);

        assertNull(serviceWithoutPublicUrl.resolvePublicUrl("reserva-plus/espacos/arquivo.png"));
        assertNotNull(storageService.resolvePublicUrl("reserva-plus/espacos/arquivo.png"));
    }

    private String readBody(RequestBody requestBody) {
        try {
            return new String(requestBody.contentStreamProvider().newStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
