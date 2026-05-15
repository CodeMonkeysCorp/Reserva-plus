package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioCreateRequest;
import com.reservaplus.reserva_plus.dto.bloqueio.BloqueioRecorrenteResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BloqueioServiceImpl implements BloqueioService {

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
        LocalDate hoje = nowDate();
        LocalTime agora = nowTime();

        HorarioFuncionamentoSupport.validateHoraCheia(
                request.getHorarioInicio(),
                request.getHorarioFim(),
                "Os bloqueios devem ser definidos em hora cheia.",
                "Horário final deve ser maior que o horário inicial."
        );

        List<LocalDate> datas = resolveDatasDaSerie(request);
        for (LocalDate data : datas) {
            validateDataBloqueio(data, request.getHorarioFim(), hoje, agora);
        }

        Espaco espaco = espacoRepository.findById(request.getEspacoId())
                .orElseThrow(() -> new NotFoundException("Espaço não encontrado."));

        HorarioFuncionamentoSupport.validateDentroDoHorarioFuncionamento(
                espaco,
                request.getHorarioInicio(),
                request.getHorarioFim(),
                "O bloqueio precisa estar dentro do horário de funcionamento do espaço: %s às %s."
        );

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

        return ReservaBloqueioResponseSupport.toBloqueioResponse(bloqueioHorarioRepository.saveAll(bloqueios).get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueioResponse> findByEspacoAndData(Long espacoId, LocalDate data) {
        return bloqueioHorarioRepository.findByEspacoIdAndDataOrderByHorarioInicio(espacoId, data)
                .stream()
                .map(ReservaBloqueioResponseSupport::toBloqueioResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueioRecorrenteResponse> findRecurringByEspaco(Long espacoId) {
        Map<String, List<BloqueioHorario>> series = new LinkedHashMap<>();

        for (BloqueioHorario bloqueio : bloqueioHorarioRepository.findByEspacoIdAndSerieRecorrenciaIdIsNotNullOrderByDataAscHorarioInicioAsc(espacoId)) {
            String serieRecorrenciaId = bloqueio.getSerieRecorrenciaId();
            if (serieRecorrenciaId == null || serieRecorrenciaId.isBlank()) {
                continue;
            }

            series.computeIfAbsent(serieRecorrenciaId, ignored -> new ArrayList<>()).add(bloqueio);
        }

        return series.values().stream()
                .map(this::toRecurringResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bloqueio não encontrado."));
        bloqueioHorarioRepository.delete(bloqueio);
    }

    @Override
    @Transactional
    public void deleteSerie(Long id) {
        BloqueioHorario bloqueio = bloqueioHorarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bloqueio não encontrado."));

        if (bloqueio.getSerieRecorrenciaId() == null || bloqueio.getSerieRecorrenciaId().isBlank()) {
            bloqueioHorarioRepository.delete(bloqueio);
            return;
        }

        List<BloqueioHorario> bloqueiosDaSerie = bloqueioHorarioRepository.findBySerieRecorrenciaId(bloqueio.getSerieRecorrenciaId());
        bloqueioHorarioRepository.deleteAll(bloqueiosDaSerie);
    }

    private BloqueioRecorrenteResponse toRecurringResponse(List<BloqueioHorario> bloqueiosDaSerie) {
        BloqueioHorario primeiroBloqueio = bloqueiosDaSerie.get(0);
        BloqueioHorario ultimoBloqueio = bloqueiosDaSerie.get(bloqueiosDaSerie.size() - 1);

        BloqueioRecorrenteResponse response = new BloqueioRecorrenteResponse();
        response.setId(primeiroBloqueio.getId());
        response.setEspacoId(primeiroBloqueio.getEspaco().getId());
        response.setEspacoNome(primeiroBloqueio.getEspaco().getNome());
        response.setSerieRecorrenciaId(primeiroBloqueio.getSerieRecorrenciaId());
        response.setDataInicio(primeiroBloqueio.getData());
        response.setDataFim(ultimoBloqueio.getData());
        response.setHorarioInicio(primeiroBloqueio.getHorarioInicio());
        response.setHorarioFim(primeiroBloqueio.getHorarioFim());
        response.setMotivo(primeiroBloqueio.getMotivo());
        response.setTotalOcorrencias(bloqueiosDaSerie.size());
        return response;
    }

    private List<LocalDate> resolveDatasDaSerie(BloqueioCreateRequest request) {
        if (!Boolean.TRUE.equals(request.getRecorrenteSemanal())) {
            return List.of(request.getData());
        }

        if (request.getDataFimRecorrencia() == null) {
            throw new BadRequestException("Informe a data final da recorrência semanal.");
        }

        if (request.getDataFimRecorrencia().isBefore(request.getData())) {
            throw new BadRequestException("A data final da recorrência deve ser igual ou posterior à data inicial.");
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
            throw new ConflictException(buildConflictMessage("Já existe bloqueio para este intervalo.", data, recorrencia));
        }

        boolean conflitoReserva = reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espacoId,
                data,
                ReservaStatus.ATIVA,
                horarioFim,
                horarioInicio
        );

        if (conflitoReserva) {
            throw new ConflictException(buildConflictMessage("Já existe reserva ativa para este intervalo.", data, recorrencia));
        }
    }

    private String buildConflictMessage(String baseMessage, LocalDate data, boolean recorrencia) {
        if (!recorrencia) {
            return baseMessage;
        }

        return String.format("%s Conflito na recorrência em %s.", baseMessage, data.format(DATE_FORMATTER));
    }

    private void validateDataBloqueio(LocalDate data, LocalTime fim, LocalDate hoje, LocalTime agora) {
        if (data.isBefore(hoje)) {
            throw new BadRequestException("A data do bloqueio deve ser hoje ou futura.");
        }

        if (data.isEqual(hoje) && !fim.isAfter(agora)) {
            throw new BadRequestException("Para bloqueios de hoje, selecione um horario que ainda nao tenha terminado.");
        }
    }

    LocalDate nowDate() {
        return AppClockSupport.nowDate();
    }

    LocalTime nowTime() {
        return AppClockSupport.nowTime();
    }
}
