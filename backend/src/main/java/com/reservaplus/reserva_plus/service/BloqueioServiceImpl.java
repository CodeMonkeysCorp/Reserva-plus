package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioCreateRequest;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.BloqueioHorario;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.repository.BloqueioHorarioRepository;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BloqueioServiceImpl implements BloqueioService {

    private static final LocalTime HORARIO_PADRAO_INICIO = LocalTime.of(6, 0);
    private static final LocalTime HORARIO_PADRAO_FIM = LocalTime.of(23, 0);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BloqueioHorarioRepository bloqueioHorarioRepository;
    private final EspacoRepository espacoRepository;
    private final ReservaRepository reservaRepository;

    public BloqueioServiceImpl(
            BloqueioHorarioRepository bloqueioHorarioRepository,
            EspacoRepository espacoRepository,
            ReservaRepository reservaRepository
    ) {
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
        this.espacoRepository = espacoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    @Transactional
    public BloqueioResponse create(BloqueioCreateRequest request) {
        validateHorario(request.getHorarioInicio(), request.getHorarioFim());

        Espaco espaco = espacoRepository.findById(request.getEspacoId())
                .orElseThrow(() -> new NotFoundException("Espaco nao encontrado."));

        validateHorarioFuncionamento(espaco, request.getHorarioInicio(), request.getHorarioFim());

        List<LocalDate> datas = resolveDatasDaSerie(request);
        boolean recorrencia = datas.size() > 1;
        String serieRecorrenciaId = recorrencia ? UUID.randomUUID().toString() : null;
        List<BloqueioHorario> bloqueios = new ArrayList<>();

        for (LocalDate data : datas) {
            validarConflitos(request.getEspacoId(), data, request.getHorarioInicio(), request.getHorarioFim(), recorrencia);

            BloqueioHorario bloqueio = new BloqueioHorario();
            bloqueio.setEspaco(espaco);
            bloqueio.setData(data);
            bloqueio.setHorarioInicio(request.getHorarioInicio());
            bloqueio.setHorarioFim(request.getHorarioFim());
            bloqueio.setMotivo(request.getMotivo());
            bloqueio.setSerieRecorrenciaId(serieRecorrenciaId);
            bloqueios.add(bloqueio);
        }

        return toResponse(bloqueioHorarioRepository.saveAll(bloqueios).get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueioResponse> findByEspacoAndData(Long espacoId, LocalDate data) {
        return bloqueioHorarioRepository.findByEspacoIdAndDataOrderByHorarioInicio(espacoId, data)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bloqueio nao encontrado."));
        bloqueioHorarioRepository.delete(bloqueio);
    }

    @Override
    @Transactional
    public void deleteSerie(Long id) {
        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bloqueio nao encontrado."));

        if (bloqueio.getSerieRecorrenciaId() == null || bloqueio.getSerieRecorrenciaId().isBlank()) {
            bloqueioHorarioRepository.delete(bloqueio);
            return;
        }

        List<BloqueioHorario> bloqueiosDaSerie = bloqueioHorarioRepository.findBySerieRecorrenciaId(bloqueio.getSerieRecorrenciaId());
        bloqueioHorarioRepository.deleteAll(bloqueiosDaSerie);
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
        response.setSerieRecorrenciaId(bloqueio.getSerieRecorrenciaId());
        return response;
    }

    private List<LocalDate> resolveDatasDaSerie(BloqueioCreateRequest request) {
        if (!Boolean.TRUE.equals(request.getRecorrenteSemanal())) {
            return List.of(request.getData());
        }

        if (request.getDataFimRecorrencia() == null) {
            throw new BadRequestException("Informe a data final da recorrencia semanal.");
        }

        if (request.getDataFimRecorrencia().isBefore(request.getData())) {
            throw new BadRequestException("A data final da recorrencia deve ser igual ou posterior a data inicial.");
        }

        List<LocalDate> datas = new ArrayList<>();
        LocalDate dataAtual = request.getData();
        while (!dataAtual.isAfter(request.getDataFimRecorrencia())) {
            datas.add(dataAtual);
            dataAtual = dataAtual.plusWeeks(1);
        }

        return datas;
    }

    private void validarConflitos(Long espacoId, LocalDate data, LocalTime horarioInicio, LocalTime horarioFim, boolean recorrencia) {
        boolean overlap = bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espacoId,
                data,
                horarioFim,
                horarioInicio
        );

        if (overlap) {
            throw new ConflictException(buildConflictMessage("Ja existe bloqueio para este intervalo.", data, recorrencia));
        }

        boolean conflitoReserva = reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espacoId,
                data,
                ReservaStatus.ATIVA,
                horarioFim,
                horarioInicio
        );

        if (conflitoReserva) {
            throw new ConflictException(buildConflictMessage("Ja existe reserva ativa para este intervalo.", data, recorrencia));
        }
    }

    private String buildConflictMessage(String baseMessage, LocalDate data, boolean recorrencia) {
        if (!recorrencia) {
            return baseMessage;
        }

        return String.format("%s Conflito na recorrencia em %s.", baseMessage, data.format(DATE_FORMATTER));
    }

    private void validateHorario(LocalTime inicio, LocalTime fim) {
        if (inicio.getMinute() != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0
                || fim.getMinute() != 0 || fim.getSecond() != 0 || fim.getNano() != 0) {
            throw new BadRequestException("Os bloqueios devem ser definidos em hora cheia.");
        }

        if (!fim.isAfter(inicio)) {
            throw new BadRequestException("Horario final deve ser maior que o horario inicial.");
        }
    }

    private void validateHorarioFuncionamento(Espaco espaco, LocalTime inicio, LocalTime fim) {
        LocalTime funcionamentoInicio = espaco.getHorarioFuncionamentoInicio() != null
                ? espaco.getHorarioFuncionamentoInicio()
                : HORARIO_PADRAO_INICIO;
        LocalTime funcionamentoFim = espaco.getHorarioFuncionamentoFim() != null
                ? espaco.getHorarioFuncionamentoFim()
                : HORARIO_PADRAO_FIM;

        if (inicio.isBefore(funcionamentoInicio) || fim.isAfter(funcionamentoFim)) {
            throw new BadRequestException(
                    String.format(
                            "O bloqueio precisa estar dentro do horario de funcionamento do espaco: %s as %s.",
                            funcionamentoInicio,
                            funcionamentoFim
                    )
            );
        }
    }
}
