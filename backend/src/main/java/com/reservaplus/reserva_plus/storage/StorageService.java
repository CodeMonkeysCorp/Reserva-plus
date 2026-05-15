package com.reservaplus.reserva_plus.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredObject upload(String directory, MultipartFile file);

    void delete(String objectKey);

    String resolvePublicUrl(String objectKey);
}
