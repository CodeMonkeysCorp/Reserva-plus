package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.reserva.ReservaCreateRequest;
import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.exception.ForbiddenException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.Espaco;
import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.BloqueioHorarioRepository;
import com.reservaplus.reserva_plus.repository.EspacoRepository;
import com.reservaplus.reserva_plus.repository.ReservaRepository;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacoRepository espacoRepository;
    private final BloqueioHorarioRepository bloqueioHorarioRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            UsuarioRepository usuarioRepository,
            EspacoRepository espacoRepository,
            BloqueioHorarioRepository bloqueioHorarioRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.espacoRepository = espacoRepository;
        this.bloqueioHorarioRepository = bloqueioHorarioRepository;
    }

    @Transactional
    public ReservaResponse create(ReservaCreateRequest request, String userEmail) {
        validateHorario(request.getHorarioInicio(), request.getHorarioFim());

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        Espaco espaco = espacoRepository.findByIdForUpdate(request.getEspacoId())
                .orElseThrow(() -> new NotFoundException("Espaço não encontrado."));

        if (!espaco.isAtivo()) {
            throw new ConflictException("Espaço inativo. Não é possível reservar.");
        }

        boolean blocked = bloqueioHorarioRepository.existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espaco.getId(),
                request.getData(),
                request.getHorarioFim(),
                request.getHorarioInicio()
        );
        if (blocked) {
            throw new ConflictException("Horário bloqueado pela administração.");
        }

        boolean conflict = reservaRepository.existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
                espaco.getId(),
                request.getData(),
                ReservaStatus.ATIVA,
                request.getHorarioFim(),
                request.getHorarioInicio()
        );
        if (conflict) {
            throw new ConflictException("Conflito de horário. Já existe reserva nesse intervalo.");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setEspaco(espaco);
        reserva.setData(request.getData());
        reserva.setHorarioInicio(request.getHorarioInicio());
        reserva.setHorarioFim(request.getHorarioFim());
        reserva.setStatus(ReservaStatus.ATIVA);

        return toResponse(reservaRepository.save(reserva));
    }

    @Transactional
    public ReservaResponse cancel(Long reservaId, String userEmail, boolean isAdmin) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));

        if (!isAdmin && !reserva.getUsuario().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenException("Você não tem permissão para cancelar esta reserva.");
        }

        if (reserva.getStatus() == ReservaStatus.CANCELADA) {
            throw new BadRequestException("Reserva já está cancelada.");
        }

        reserva.setStatus(ReservaStatus.CANCELADA);
        return toResponse(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> historico(String userEmail, boolean isAdmin) {
        if (isAdmin) {
            return reservaRepository.findAllByOrderByDataDescHorarioInicioDesc()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        return reservaRepository.findByUsuarioIdOrderByDataDescHorarioInicioDesc(usuario.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReservaResponse toResponse(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setId(reserva.getId());
        response.setUsuarioId(reserva.getUsuario().getId());
        response.setUsuarioNome(reserva.getUsuario().getNome());
        response.setEspacoId(reserva.getEspaco().getId());
        response.setEspacoNome(reserva.getEspaco().getNome());
        response.setData(reserva.getData());
        response.setHorarioInicio(reserva.getHorarioInicio());
        response.setHorarioFim(reserva.getHorarioFim());
        response.setStatus(reserva.getStatus());
        response.setCriadoEm(reserva.getCriadoEm());
        return response;
    }

    private void validateHorario(java.time.LocalTime inicio, java.time.LocalTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new BadRequestException("Horário final deve ser maior que o horário inicial.");
        }
    }
}
