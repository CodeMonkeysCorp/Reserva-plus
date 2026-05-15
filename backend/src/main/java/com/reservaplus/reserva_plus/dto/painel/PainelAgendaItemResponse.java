package com.reservaplus.reserva_plus.dto.painel;

import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;

public record PainelAgendaItemResponse(
        ReservaResponse reserva,
        boolean emAndamento
) {
}
