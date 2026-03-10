package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByEspacoIdAndDataAndStatusAndHorarioInicioLessThanAndHorarioFimGreaterThan(
            Long espacoId,
            LocalDate data,
            ReservaStatus status,
            LocalTime horarioFim,
            LocalTime horarioInicio
    );

    List<Reserva> findByUsuarioIdOrderByDataDescHorarioInicioDesc(Long usuarioId);

    List<Reserva> findAllByOrderByDataDescHorarioInicioDesc();
}
