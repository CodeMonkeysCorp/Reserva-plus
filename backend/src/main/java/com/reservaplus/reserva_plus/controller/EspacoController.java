package com.reservaplus.reserva_plus.controller;

import com.reservaplus.reserva_plus.dto.espaco.EspacoRequest;
import com.reservaplus.reserva_plus.dto.espaco.EspacoResponse;
import com.reservaplus.reserva_plus.service.EspacoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/espacos")
public class EspacoController {

    private final EspacoService espacoService;

    public EspacoController(EspacoService espacoService) {
        this.espacoService = espacoService;
    }

    @GetMapping
    public ResponseEntity<List<EspacoResponse>> listAll() {
        return ResponseEntity.ok(espacoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspacoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(espacoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EspacoResponse> create(@Valid @RequestBody EspacoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacoService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EspacoResponse> update(@PathVariable Long id, @Valid @RequestBody EspacoRequest request) {
        return ResponseEntity.ok(espacoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        espacoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
