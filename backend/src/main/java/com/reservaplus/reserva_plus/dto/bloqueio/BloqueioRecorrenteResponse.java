package com.reservaplus.reserva_plus.dto.bloqueio;

import java.time.LocalDate;
import java.time.LocalTime;

public class BloqueioRecorrenteResponse {

    private Long id;
    private Long espacoId;
    private String espacoNome;
    private String serieRecorrenciaId;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalTime horarioInicio;
    private LocalTime horarioFim;
    private String motivo;
    private Integer totalOcorrencias;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEspacoId() {
        return espacoId;
    }

    public void setEspacoId(Long espacoId) {
        this.espacoId = espacoId;
    }

    public String getEspacoNome() {
        return espacoNome;
    }

    public void setEspacoNome(String espacoNome) {
        this.espacoNome = espacoNome;
    }

    public String getSerieRecorrenciaId() {
        return serieRecorrenciaId;
    }

    public void setSerieRecorrenciaId(String serieRecorrenciaId) {
        this.serieRecorrenciaId = serieRecorrenciaId;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
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

    public Integer getTotalOcorrencias() {
        return totalOcorrencias;
    }

    public void setTotalOcorrencias(Integer totalOcorrencias) {
        this.totalOcorrencias = totalOcorrencias;
    }
}
