package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.espaco.EspacoRequest;
import com.reservaplus.reserva_plus.dto.espaco.EspacoResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Locale;
import java.util.List;

@Service
public class EspacoServiceImpl implements EspacoService {

    private static final LocalTime HORARIO_PADRAO_INICIO = LocalTime.of(6, 0);
    private static final LocalTime HORARIO_PADRAO_FIM = LocalTime.of(23, 0);
    private static final int TIPO_MAX_LENGTH = 60;

    private final EspacoRepository espacoRepository;

    public EspacoServiceImpl(EspacoRepository espacoRepository) {
        this.espacoRepository = espacoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspacoResponse> findAll() {
        return espacoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EspacoResponse findById(Long id) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));
        return toResponse(espaco);
    }

    @Override
    @Transactional
    public EspacoResponse create(EspacoRequest request) {
        Espaco espaco = new Espaco();
        applyRequest(espaco, request);
        return toResponse(espacoRepository.save(espaco));
    }

    @Override
    @Transactional
    public EspacoResponse update(Long id, EspacoRequest request) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));
        applyRequest(espaco, request);
        return toResponse(espacoRepository.save(espaco));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));
        espacoRepository.delete(espaco);
    }

    private void applyRequest(Espaco espaco, EspacoRequest request) {
        LocalTime horarioInicio = request.getHorarioFuncionamentoInicio() != null
                ? request.getHorarioFuncionamentoInicio()
                : HORARIO_PADRAO_INICIO;
        LocalTime horarioFim = request.getHorarioFuncionamentoFim() != null
                ? request.getHorarioFuncionamentoFim()
                : HORARIO_PADRAO_FIM;

        validateHorarioFuncionamento(horarioInicio, horarioFim);

        espaco.setNome(request.getNome().trim());
        espaco.setTipo(normalizeTipo(request.getTipo()));
        espaco.setDescricao(normalizeDescricao(request.getDescricao()));
        espaco.setAtivo(request.getAtivo() == null || request.getAtivo());
        espaco.setHorarioFuncionamentoInicio(horarioInicio);
        espaco.setHorarioFuncionamentoFim(horarioFim);
    }

    private EspacoResponse toResponse(Espaco espaco) {
        EspacoResponse response = new EspacoResponse();
        response.setId(espaco.getId());
        response.setNome(espaco.getNome());
        response.setTipo(espaco.getTipo());
        response.setDescricao(espaco.getDescricao());
        response.setAtivo(espaco.isAtivo());
        response.setHorarioFuncionamentoInicio(resolveHorarioInicio(espaco));
        response.setHorarioFuncionamentoFim(resolveHorarioFim(espaco));
        return response;
    }

    private void validateHorarioFuncionamento(LocalTime inicio, LocalTime fim) {
        if (inicio.getMinute() != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0
                || fim.getMinute() != 0 || fim.getSecond() != 0 || fim.getNano() != 0) {
            throw new BadRequestException("Os horarios de funcionamento devem ser configurados em hora cheia.");
        }

        if (!fim.isAfter(inicio)) {
            throw new BadRequestException("O horario final de funcionamento deve ser maior que o inicial.");
        }
    }

    private String normalizeTipo(String tipo) {
        if (tipo == null) {
            throw new BadRequestException("Selecione o tipo do espaco.");
        }

        String normalized = tipo.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new BadRequestException("Selecione o tipo do espaco.");
        }

        if (normalized.length() > TIPO_MAX_LENGTH) {
            throw new BadRequestException("O tipo do espaco deve ter no maximo 60 caracteres.");
        }

        return normalized;
    }

    private String normalizeDescricao(String descricao) {
        if (descricao == null) {
            return null;
        }

        String normalized = descricao.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LocalTime resolveHorarioInicio(Espaco espaco) {
        return espaco.getHorarioFuncionamentoInicio() != null ? espaco.getHorarioFuncionamentoInicio() : HORARIO_PADRAO_INICIO;
    }

    private LocalTime resolveHorarioFim(Espaco espaco) {
        return espaco.getHorarioFuncionamentoFim() != null ? espaco.getHorarioFuncionamentoFim() : HORARIO_PADRAO_FIM;
    }
}
