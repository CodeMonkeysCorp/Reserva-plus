package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.usuario.UsuarioAdminResponse;
import com.reservaplus.reserva_plus.dto.usuario.UsuarioAdminUpdateRequest;

import java.util.List;

public interface UsuarioService {

    List<UsuarioAdminResponse> findAll();

    UsuarioAdminResponse updateAdminData(Long usuarioId, UsuarioAdminUpdateRequest request, String actorEmail);
}
