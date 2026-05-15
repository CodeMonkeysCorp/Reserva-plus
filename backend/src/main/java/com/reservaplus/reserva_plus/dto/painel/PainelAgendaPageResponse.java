package com.reservaplus.reserva_plus.dto.painel;

import java.util.List;

public record PainelAgendaPageResponse(
        List<PainelAgendaItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
