package com.reservaplus.reserva_plus.dto.reserva;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservaCreateRequest {

    @NotNull(message = "Selecione o espaço da reserva.")
    private Long espacoId;

    @NotNull(message = "Informe a data da reserva.")
    @FutureOrPresent(message = "A data da reserva deve ser hoje ou futura.")
    private LocalDate data;

    @NotNull(message = "Informe o horário de início.")
    private LocalTime horarioInicio;

    @NotNull(message = "Informe o horário de fim.")
    private LocalTime horarioFim;

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
}
