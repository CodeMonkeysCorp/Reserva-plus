package com.reservaplus.reserva_plus.config;

import com.reservaplus.reserva_plus.storage.CloudflareR2StorageService;
import com.reservaplus.reserva_plus.storage.DisabledStorageService;
import com.reservaplus.reserva_plus.storage.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
    public S3Client r2S3Client(R2StorageProperties properties) {
        validate(properties);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKeyId().trim(),
                properties.getSecretAccessKey().trim()
        );

        return S3Client.builder()
                .endpointOverride(URI.create(properties.resolveEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnBean(S3Client.class)
    public StorageService r2StorageService(S3Client s3Client, R2StorageProperties properties) {
        return new CloudflareR2StorageService(s3Client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    public StorageService disabledStorageService() {
        return new DisabledStorageService();
    }

    private void validate(R2StorageProperties properties) {
        if (!StringUtils.hasText(properties.getAccessKeyId())) {
            throw new IllegalStateException("Defina APP_STORAGE_R2_ACCESS_KEY_ID para habilitar o Cloudflare R2.");
        }
        if (!StringUtils.hasText(properties.getSecretAccessKey())) {
            throw new IllegalStateException("Defina APP_STORAGE_R2_SECRET_ACCESS_KEY para habilitar o Cloudflare R2.");
        }
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("Defina APP_STORAGE_R2_BUCKET para habilitar o Cloudflare R2.");
        }
        if (!StringUtils.hasText(properties.getEndpoint()) && !StringUtils.hasText(properties.getAccountId())) {
            throw new IllegalStateException("Defina APP_STORAGE_R2_ENDPOINT ou APP_STORAGE_R2_ACCOUNT_ID para habilitar o Cloudflare R2.");
        }
    }
}
