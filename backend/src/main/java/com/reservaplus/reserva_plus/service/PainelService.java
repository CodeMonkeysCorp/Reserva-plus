package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.painel.PainelResumoResponse;

public interface PainelService {

    PainelResumoResponse resumo(int page, int size);
}
