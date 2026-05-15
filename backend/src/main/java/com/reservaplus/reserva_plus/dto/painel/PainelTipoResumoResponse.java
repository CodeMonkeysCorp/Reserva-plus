package com.reservaplus.reserva_plus.dto.painel;

public record PainelTipoResumoResponse(
        String tipo,
        long total,
        long ativos,
        long percentual
) {
}
