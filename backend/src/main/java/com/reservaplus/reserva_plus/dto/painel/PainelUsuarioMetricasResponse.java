package com.reservaplus.reserva_plus.dto.painel;

public record PainelUsuarioMetricasResponse(
        long totalUsuarios,
        long totalAdmins,
        long usuariosComReserva,
        long usuariosSemReserva
) {
}
