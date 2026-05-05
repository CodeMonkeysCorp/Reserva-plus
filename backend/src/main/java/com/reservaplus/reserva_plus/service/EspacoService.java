package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.espaco.EspacoRequest;
import com.reservaplus.reserva_plus.dto.espaco.EspacoResponse;

import java.util.List;

public interface EspacoService {

    List<EspacoResponse> findAll();

    EspacoResponse findById(Long id);

    EspacoResponse create(EspacoRequest request);

    EspacoResponse update(Long id, EspacoRequest request);

    void delete(Long id);
}
