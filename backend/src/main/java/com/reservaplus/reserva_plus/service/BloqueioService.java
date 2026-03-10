package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioCreateRequest;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.repository.BloqueioHorarioRepository;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BloqueioService {

    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final EspacoRepository espacoRepository;

    public BloqueioService(BloqueioHorarioRepository bloqueioHorarioRepository, EspacoRepository espacoRepository) {
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
        this.espacoRepository = espacoRepository;
    }

    @Transactional
    public BloqueioResponse create(BloqueioCreateRequest request) {
        validateHorario(request.getHorarioInicio(), request.getHorarioFim());
        Espaco espaco = espacoRepository.findById(request.getEspacoId())
                .orElseThrow(() -> new NotFoundException("Espaço não encontrado."));

        boolean overlap = bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                request.getEspacoId(),
                request.getData(),
                request.getHorarioFim(),
                request.getHorarioInicio()
        );

        if (overlap) {
            throw new ConflictException("Já existe bloqueio para este intervalo.");
        }

        BloqueioHorario bloqueio = new BloqueioHorario();
        bloqueio.setEspaco(espaco);
        bloqueio.setData(request.getData());
        bloqueio.setHorarioInicio(request.getHorarioInicio());
        bloqueio.setHorarioFim(request.getHorarioFim());
        bloqueio.setMotivo(request.getMotivo());
        return toResponse(bloqueioHorarioRepository.save(bloqueio));
    }

    @Transactional(readOnly = true)
    public List<BloqueioResponse> findByEspacoAndData(Long espacoId, LocalDate data) {
        return bloqueioHorarioRepository.findByEspacoIdAndDataOrderByHorarioInicio(espacoId, data)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bloqueio não encontrado."));
        bloqueioHorarioRepository.delete(bloqueio);
    }

    private BloqueioResponse toResponse(BloqueioHorario bloqueio) {
        BloqueioResponse response = new BloqueioResponse();
        response.setId(bloqueio.getId());
        response.setEspacoId(bloqueio.getEspaco().getId());
        response.setEspacoNome(bloqueio.getEspaco().getNome());
        response.setData(bloqueio.getData());
        response.setHorarioInicio(bloqueio.getHorarioInicio());
        response.setHorarioFim(bloqueio.getHorarioFim());
        response.setMotivo(bloqueio.getMotivo());
        return response;
    }

    private void validateHorario(java.time.LocalTime inicio, java.time.LocalTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new BadRequestException("Horário final deve ser maior que o horário inicial.");
        }
    }
}
