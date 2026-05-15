package com.reservaplus.reserva_plus.controller;

import com.reservaplus.reserva_plus.dto.espaco.EspacoImagemUploadResponse;
import com.reservaplus.reserva_plus.storage.StorageService;
import com.reservaplus.reserva_plus.storage.StoredObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/espacos/imagens")
public class EspacoImagemController {

    private final StorageService storageService;

    public EspacoImagemController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EspacoImagemUploadResponse> upload(@RequestParam("arquivo") MultipartFile arquivo) {
        StoredObject storedObject = storageService.upload("espacos", arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(EspacoImagemUploadResponse.from(storedObject));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("chaveObjeto") String chaveObjeto) {
        storageService.delete(chaveObjeto);
        return ResponseEntity.noContent().build();
    }
}
