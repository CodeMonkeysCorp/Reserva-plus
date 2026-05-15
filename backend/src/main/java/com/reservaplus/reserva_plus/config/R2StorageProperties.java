package com.reservaplus.reserva_plus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.storage.r2")
public class R2StorageProperties {

    private boolean enabled;
    private String accountId;
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String secretAccessKey;
    private String publicBaseUrl;
    private String objectKeyPrefix = "reserva-plus";
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getObjectKeyPrefix() {
        return objectKeyPrefix;
    }

    public void setObjectKeyPrefix(String objectKeyPrefix) {
        this.objectKeyPrefix = objectKeyPrefix;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String resolveEndpoint() {
        String normalizedEndpoint = normalize(endpoint);
        if (normalizedEndpoint != null) {
            return normalizedEndpoint;
        }
        return "https://" + normalize(accountId) + ".r2.cloudflarestorage.com";
    }

    public String resolveBucket() {
        return normalize(bucket);
    }

    public String resolvePublicBaseUrl() {
        String normalized = normalize(publicBaseUrl);
        if (normalized == null) {
            return null;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
