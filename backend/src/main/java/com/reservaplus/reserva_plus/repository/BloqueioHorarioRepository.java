package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.BloqueioHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BloqueioHorarioRepository extends JpaRepository<BloqueioHorario, Long> {

    boolean existsByEspacoIdAndDataAndHorarioInicioLessThanAndHorarioFimGreaterThan(
            Long espacoId,
            LocalDate data,
            LocalTime horarioFim,
            LocalTime horarioInicio
    );

    List<BloqueioHorario> findByEspacoIdAndDataOrderByHorarioInicio(Long espacoId, LocalDate data);
}
