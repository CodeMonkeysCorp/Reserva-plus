package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioCreateRequest;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;

import java.time.LocalDate;
import java.util.List;

public interface BloqueioService {

    BloqueioResponse create(BloqueioCreateRequest request);

    List<BloqueioResponse> findByEspacoAndData(Long espacoId, LocalDate data);

    void delete(Long id);

    void deleteSerie(Long id);
}
