package com.reservaplus.reserva_plus.storage;

public class StoredObject {

    private final String objectKey;
    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final String publicUrl;

    public StoredObject(String objectKey, String originalFilename, String contentType, long size, String publicUrl) {
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.publicUrl = publicUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}
