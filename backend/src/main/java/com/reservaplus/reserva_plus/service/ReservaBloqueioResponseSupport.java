package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;
import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Reserva;

final class ReservaBloqueioResponseSupport {

    private ReservaBloqueioResponseSupport() {
    }

    static ReservaResponse toReservaResponse(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setId(reserva.getId());
        response.setUsuarioId(reserva.getUsuario().getId());
        response.setUsuarioNome(reserva.getUsuario().getNome());
        response.setEspacoId(reserva.getEspaco().getId());
        response.setEspacoNome(reserva.getEspaco().getNome());
        response.setData(reserva.getData());
        response.setHorarioInicio(reserva.getHorarioInicio());
        response.setHorarioFim(reserva.getHorarioFim());
        response.setStatus(reserva.getStatus());
        response.setCriadoEm(reserva.getCriadoEm());
        return response;
    }

    static ReservaResponse toAgendaReservaResponse(Reserva reserva) {
        ReservaResponse response = toReservaResponse(reserva);
        response.setUsuarioId(null);
        response.setUsuarioNome(null);
        return response;
    }

    static BloqueioResponse toBloqueioResponse(BloqueioHorario bloqueio) {
        BloqueioResponse response = new BloqueioResponse();
        response.setId(bloqueio.getId());
        response.setEspacoId(bloqueio.getEspaco().getId());
        response.setEspacoNome(bloqueio.getEspaco().getNome());
        response.setData(bloqueio.getData());
        response.setHorarioInicio(bloqueio.getHorarioInicio());
        response.setHorarioFim(bloqueio.getHorarioFim());
        response.setMotivo(bloqueio.getMotivo());
        response.setSerieRecorrenciaId(bloqueio.getSerieRecorrenciaId());
        return response;
    }
}
