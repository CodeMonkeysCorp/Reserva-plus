package com.reservaplus.reserva_plus.controller;

import com.reservaplus.reserva_plus.dto.painel.PainelResumoResponse;
import com.reservaplus.reserva_plus.service.PainelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painelService;

    public PainelController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping("/resumo")
    public ResponseEntity<PainelResumoResponse> resumo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(painelService.resumo(page, size));
    }
}
