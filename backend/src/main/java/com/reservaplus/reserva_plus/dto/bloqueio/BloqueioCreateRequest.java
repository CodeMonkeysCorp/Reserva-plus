package com.reservaplus.reserva_plus.dto.bloqueio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class BloqueioCreateRequest {

    @NotNull(message = "Selecione o espaço do bloqueio.")
    private Long espacoId;

    @NotNull(message = "Informe a data do bloqueio.")
    private LocalDate data;

    @NotNull(message = "Informe o horário de início.")
    private LocalTime horarioInicio;

    @NotNull(message = "Informe o horário de fim.")
    private LocalTime horarioFim;

    @Size(max = 300, message = "O motivo deve ter no máximo 300 caracteres.")
    private String motivo;

    private Boolean recorrenteSemanal;

    private LocalDate dataFimRecorrencia;

    public Long getEspacoId() {
        return espacoId;
    }

    public void setEspacoId(Long espacoId) {
        this.espacoId = espacoId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public LocalTime getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(LocalTime horarioFim) {
        this.horarioFim = horarioFim;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Boolean getRecorrenteSemanal() {
        return recorrenteSemanal;
    }

    public void setRecorrenteSemanal(Boolean recorrenteSemanal) {
        this.recorrenteSemanal = recorrenteSemanal;
    }

    public LocalDate getDataFimRecorrencia() {
        return dataFimRecorrencia;
    }

    public void setDataFimRecorrencia(LocalDate dataFimRecorrencia) {
        this.dataFimRecorrencia = dataFimRecorrencia;
    }
}
