package com.reservaplus.reserva_plus.dto.espaco;

import com.reservaplus.reserva_plus.storage.StoredObject;

public class EspacoImagemUploadResponse {

    private String chaveObjeto;
    private String urlPublica;
    private String nomeArquivoOriginal;
    private String contentType;
    private long tamanhoBytes;

    public static EspacoImagemUploadResponse from(StoredObject storedObject) {
        EspacoImagemUploadResponse response = new EspacoImagemUploadResponse();
        response.setChaveObjeto(storedObject.getObjectKey());
        response.setUrlPublica(storedObject.getPublicUrl());
        response.setNomeArquivoOriginal(storedObject.getOriginalFilename());
        response.setContentType(storedObject.getContentType());
        response.setTamanhoBytes(storedObject.getSize());
        return response;
    }

    public String getChaveObjeto() {
        return chaveObjeto;
    }

    public void setChaveObjeto(String chaveObjeto) {
        this.chaveObjeto = chaveObjeto;
    }

    public String getUrlPublica() {
        return urlPublica;
    }

    public void setUrlPublica(String urlPublica) {
        this.urlPublica = urlPublica;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }
}
