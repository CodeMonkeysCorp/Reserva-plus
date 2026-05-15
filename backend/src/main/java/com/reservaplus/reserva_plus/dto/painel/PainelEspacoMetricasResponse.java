package com.reservaplus.reserva_plus.dto.painel;

public record PainelEspacoMetricasResponse(
        long totalEspacos,
        long espacosAtivos,
        long espacosInativos,
        long espacosDestacados,
        long espacosSemImagem,
        long espacosSemDescricao
) {
}
