package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.config.R2StorageProperties;
import com.reservaplus.reserva_plus.dto.espaco.EspacoRequest;
import com.reservaplus.reserva_plus.dto.espaco.EspacoResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class EspacoServiceImpl implements EspacoService {

    private static final int TIPO_MAX_LENGTH = 60;

    private final EspacoRepository espacoRepository;
    private final StorageService storageService;
    private final R2StorageProperties storageProperties;

    public EspacoServiceImpl(
            EspacoRepository espacoRepository,
            StorageService storageService,
            R2StorageProperties storageProperties
    ) {
        this.espacoRepository = espacoRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
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
        String previousImageObjectKey = espaco.getImagemObjectKey();
        applyRequest(espaco, request);
        Espaco savedEspaco = espacoRepository.save(espaco);
        deletePreviousImageIfReplaced(previousImageObjectKey, savedEspaco.getImagemObjectKey());
        return toResponse(savedEspaco);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Espaco espaco = espacoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));
        espacoRepository.delete(espaco);
        deleteManagedImageIfPresent(espaco.getImagemObjectKey());
    }

    private void applyRequest(Espaco espaco, EspacoRequest request) {
        LocalTime horarioInicio = HorarioFuncionamentoSupport.resolveOrDefault(
                request.getHorarioFuncionamentoInicio(),
                HorarioFuncionamentoSupport.HORARIO_PADRAO_INICIO
        );
        LocalTime horarioFim = HorarioFuncionamentoSupport.resolveOrDefault(
                request.getHorarioFuncionamentoFim(),
                HorarioFuncionamentoSupport.HORARIO_PADRAO_FIM
        );
        boolean destaque = request.getDestaque() != null ? request.getDestaque() : espaco.isDestaque();

        HorarioFuncionamentoSupport.validateHoraCheia(
                horarioInicio,
                horarioFim,
                "Os horarios de funcionamento devem ser configurados em hora cheia.",
                "O horario final de funcionamento deve ser maior que o inicial."
        );

        espaco.setNome(request.getNome().trim());
        espaco.setTipo(normalizeTipo(request.getTipo()));
        espaco.setDescricao(normalizeDescricao(request.getDescricao()));
        espaco.setImagemObjectKey(normalizeImagemObjectKey(request.getImagemObjectKey()));
        espaco.setAtivo(request.getAtivo() == null || request.getAtivo());
        espaco.setDestaque(destaque);
        espaco.setHorarioFuncionamentoInicio(horarioInicio);
        espaco.setHorarioFuncionamentoFim(horarioFim);
    }

    private EspacoResponse toResponse(Espaco espaco) {
        EspacoResponse response = new EspacoResponse();
        response.setId(espaco.getId());
        response.setNome(espaco.getNome());
        response.setTipo(espaco.getTipo());
        response.setDescricao(espaco.getDescricao());
        response.setImagemObjectKey(espaco.getImagemObjectKey());
        response.setImagemUrl(storageService.resolvePublicUrl(espaco.getImagemObjectKey()));
        response.setAtivo(espaco.isAtivo());
        response.setDestaque(espaco.isDestaque());
        response.setHorarioFuncionamentoInicio(HorarioFuncionamentoSupport.resolveHorarioInicio(espaco));
        response.setHorarioFuncionamentoFim(HorarioFuncionamentoSupport.resolveHorarioFim(espaco));
        return response;
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

    private String normalizeImagemObjectKey(String imagemObjectKey) {
        if (imagemObjectKey == null) {
            return null;
        }

        String normalized = imagemObjectKey.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        String expectedPrefix = buildExpectedImagePrefix();
        if (!normalized.startsWith(expectedPrefix)) {
            throw new BadRequestException("A imagem informada nao pertence ao armazenamento de espacos.");
        }

        return normalized;
    }

    private String buildExpectedImagePrefix() {
        String configuredPrefix = sanitizePathSegment(storageProperties.getObjectKeyPrefix());
        if (!StringUtils.hasText(configuredPrefix)) {
            return "espacos/";
        }
        return configuredPrefix + "/espacos/";
    }

    private String sanitizePathSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9/_-]+", "-");
        normalized = normalized.replaceAll("/{2,}", "/");
        normalized = normalized.replaceAll("-{2,}", "-");
        return normalized.replaceAll("^/+|/+$", "");
    }

    private void deletePreviousImageIfReplaced(String previousImageObjectKey, String currentImageObjectKey) {
        if (previousImageObjectKey == null || previousImageObjectKey.equals(currentImageObjectKey)) {
            return;
        }

        deleteManagedImageIfPresent(previousImageObjectKey);
    }

    private void deleteManagedImageIfPresent(String imageObjectKey) {
        if (!StringUtils.hasText(imageObjectKey)) {
            return;
        }

        storageService.delete(imageObjectKey);
    }
}
