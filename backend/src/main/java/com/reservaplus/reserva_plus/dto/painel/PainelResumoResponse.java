package com.reservaplus.reserva_plus.dto.painel;

import java.util.List;

public record PainelResumoResponse(
        PainelEspacoMetricasResponse espacos,
        PainelReservaMetricasResponse reservas,
        PainelUsuarioMetricasResponse usuarios,
        PainelAgendaPageResponse agenda,
        List<PainelTipoResumoResponse> tiposResumo,
        List<PainelRankingEspacoResponse> topEspacos,
        List<PainelRankingEspacoResponse> espacosConcorridos
) {
}
