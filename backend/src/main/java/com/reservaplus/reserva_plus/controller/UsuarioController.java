package com.reservaplus.reserva_plus.controller;

import com.reservaplus.reserva_plus.dto.usuario.UsuarioAdminResponse;
import com.reservaplus.reserva_plus.dto.usuario.UsuarioAdminUpdateRequest;
import com.reservaplus.reserva_plus.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioAdminResponse>> listAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAdminUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(usuarioService.updateAdminData(id, request, authentication.getName()));
    }
}
