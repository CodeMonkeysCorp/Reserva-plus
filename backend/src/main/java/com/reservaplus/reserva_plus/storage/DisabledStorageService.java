package com.reservaplus.reserva_plus.storage;

import com.reservaplus.reserva_plus.exception.StorageUnavailableException;
import org.springframework.web.multipart.MultipartFile;

public class DisabledStorageService implements StorageService {

    private static final String MESSAGE = "O armazenamento de imagens nao esta habilitado no ambiente atual.";

    @Override
    public StoredObject upload(String directory, MultipartFile file) {
        throw new StorageUnavailableException(MESSAGE);
    }

    @Override
    public void delete(String objectKey) {
        throw new StorageUnavailableException(MESSAGE);
    }

    @Override
    public String resolvePublicUrl(String objectKey) {
        return null;
    }
}
