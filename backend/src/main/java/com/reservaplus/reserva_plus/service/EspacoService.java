package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.espaco.EspacoRequest;
import com.reservaplus.reserva_plus.dto.espaco.EspacoResponse;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspacoService {

    private final EspacoRepository espacoRepository;

    public EspacoService(EspacoRepository espacoRepository) {
        this.espacoRepository = espacoRepository;
    }

    @Transactional(readOnly = true)
    public List<EspacoResponse> findAll() {
        return espacoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EspacoResponse findById(Long id) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaço não encontrado."));
        return toResponse(espaco);
    }

    @Transactional
    public EspacoResponse create(EspacoRequest request) {
        Espaco espaco = new Espaco();
        applyRequest(espaco, request);
        return toResponse(espacoRepository.save(espaco));
    }

    @Transactional
    public EspacoResponse update(Long id, EspacoRequest request) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaço não encontrado."));
        applyRequest(espaco, request);
        return toResponse(espacoRepository.save(espaco));
    }

    @Transactional
    public void delete(Long id) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaço não encontrado."));
        espacoRepository.delete(espaco);
    }

    private void applyRequest(Espaco espaco, EspacoRequest request) {
        espaco.setNome(request.getNome().trim());
        espaco.setTipo(request.getTipo());
        espaco.setDescricao(request.getDescricao() != null ? request.getDescricao().trim() : null);
        espaco.setAtivo(request.getAtivo() == null || request.getAtivo());
    }

    public EspacoResponse toResponse(Espaco espaco) {
        EspacoResponse response = new EspacoResponse();
        response.setId(espaco.getId());
        response.setNome(espaco.getNome());
        response.setTipo(espaco.getTipo());
        response.setDescricao(espaco.getDescricao());
        response.setAtivo(espaco.isAtivo());
        return response;
    }
}
