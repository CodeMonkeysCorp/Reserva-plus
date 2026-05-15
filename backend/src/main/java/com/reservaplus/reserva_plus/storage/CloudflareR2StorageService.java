package com.reservaplus.reserva_plus.storage;

import com.reservaplus.reserva_plus.config.R2StorageProperties;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.StorageUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CloudflareR2StorageService implements StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp"
    );

    private final S3Client s3Client;
    private final R2StorageProperties properties;

    public CloudflareR2StorageService(S3Client s3Client, R2StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public StoredObject upload(String directory, MultipartFile file) {
        validateDirectory(directory);
        String contentType = validateFile(file);

        String objectKey = buildObjectKey(directory, contentType);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.resolveBucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        try {
            byte[] fileContent = file.getBytes();
            s3Client.putObject(request, RequestBody.fromBytes(fileContent));
        } catch (IOException | SdkException ex) {
            throw new StorageUnavailableException("Nao foi possivel enviar a imagem para o Cloudflare R2.", ex);
        }

        return new StoredObject(
                objectKey,
                StringUtils.cleanPath(file.getOriginalFilename()),
                contentType,
                file.getSize(),
                resolvePublicUrl(objectKey)
        );
    }

    @Override
    public void delete(String objectKey) {
        String normalizedKey = normalizeManagedObjectKey(objectKey);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.resolveBucket())
                    .key(normalizedKey)
                    .build());
        } catch (SdkException ex) {
            throw new StorageUnavailableException("Nao foi possivel remover a imagem do Cloudflare R2.", ex);
        }
    }

    @Override
    public String resolvePublicUrl(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        if (normalizedKey == null) {
            return null;
        }

        String baseUrl = properties.resolvePublicBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }

        return baseUrl + "/" + normalizedKey;
    }

    private void validateDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            throw new IllegalArgumentException("O diretorio de armazenamento deve ser informado.");
        }
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Envie uma imagem valida.");
        }
        if (!StringUtils.hasText(file.getContentType())) {
            throw new BadRequestException("Nao foi possivel identificar o tipo da imagem enviada.");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Formato de imagem nao suportado. Use JPG, PNG ou WEBP.");
        }

        long maxFileSize = properties.getMaxFileSize().toBytes();
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("A imagem excede o tamanho maximo configurado para upload.");
        }

        return contentType;
    }

    private String buildObjectKey(String directory, String contentType) {
        String rootPrefix = sanitizePathSegment(properties.getObjectKeyPrefix());
        String normalizedDirectory = sanitizePathSegment(directory);
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String randomName = UUID.randomUUID() + extension;

        if (!StringUtils.hasText(rootPrefix)) {
            return normalizedDirectory + "/" + randomName;
        }
        return rootPrefix + "/" + normalizedDirectory + "/" + randomName;
    }

    private String normalizeManagedObjectKey(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        if (normalizedKey == null) {
            throw new BadRequestException("Informe a chave do objeto a ser removido.");
        }

        String rootPrefix = sanitizePathSegment(properties.getObjectKeyPrefix());
        if (StringUtils.hasText(rootPrefix) && !normalizedKey.startsWith(rootPrefix + "/")) {
            throw new BadRequestException("A chave informada nao pertence ao prefixo configurado para o sistema.");
        }

        return normalizedKey;
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }

        String normalizedKey = objectKey.trim();
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }
        return normalizedKey;
    }

    private String sanitizePathSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9/_-]+", "-");
        normalized = normalized.replaceAll("/{2,}", "/");
        normalized = normalized.replaceAll("-{2,}", "-");
        return normalized.replaceAll("^/+|/+$", "");
    }

    private String normalizeContentType(String contentType) {
        return contentType.trim().toLowerCase(Locale.ROOT);
    }
}
