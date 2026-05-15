package com.reservaplus.reserva_plus.dto.painel;

public record PainelRankingEspacoResponse(
        long espacoId,
        String nome,
        String tipo,
        long totalReservas,
        long futuras,
        long agendaAtiva,
        boolean destaque
) {
}
