package com.reservaplus.reserva_plus.dto.painel;

public record PainelReservaMetricasResponse(
        long reservasAtivas,
        long reservasCanceladas,
        long reservasConcluidas,
        long reservasHoje,
        long reservasEmAndamento,
        long reservasFuturas
) {
}
