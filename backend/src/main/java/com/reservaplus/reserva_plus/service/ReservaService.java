package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.reserva.AgendaDiaResponse;
import com.reservaplus.reserva_plus.dto.reserva.ReservaCreateRequest;
import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReservaService {

    ReservaResponse create(ReservaCreateRequest request, String userEmail);

    ReservaResponse cancel(Long reservaId, String userEmail, boolean isAdmin);

    List<ReservaResponse> historico(String userEmail, boolean isAdmin, LocalDate data);

    AgendaDiaResponse agendaDoDia(Long espacoId, LocalDate data);

    int concluirReservasPendentes();
}
