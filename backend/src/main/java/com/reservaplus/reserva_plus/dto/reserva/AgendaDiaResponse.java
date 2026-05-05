package com.reservaplus.reserva_plus.dto.reserva;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;

import java.util.List;

public class AgendaDiaResponse {

    private List<ReservaResponse> reservasAtivas;
    private List<BloqueioResponse> bloqueios;

    public List<ReservaResponse> getReservasAtivas() {
        return reservasAtivas;
    }

    public void setReservasAtivas(List<ReservaResponse> reservasAtivas) {
        this.reservasAtivas = reservasAtivas;
    }

    public List<BloqueioResponse> getBloqueios() {
        return bloqueios;
    }

    public void setBloqueios(List<BloqueioResponse> bloqueios) {
        this.bloqueios = bloqueios;
    }
}
