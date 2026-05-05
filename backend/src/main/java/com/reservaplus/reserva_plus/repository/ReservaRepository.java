package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<Reserva> findByUsuarioIdAndData(Long usuarioId, LocalDate data);

    List<Reserva> findByData(LocalDate data);

    List<Reserva> findAllByOrderByDataDescHorarioInicioDesc();

    List<Reserva> findByEspacoIdAndDataAndStatusOrderByHorarioInicio(Long espacoId, LocalDate data, ReservaStatus status);

    @Query("""
            select r
            from Reserva r
            where r.status = :status
              and (
                r.data < :hoje
                or (r.data = :hoje and r.horarioFim <= :agora)
              )
            """)
    List<Reserva> findPendentesDeConclusao(
            @Param("status") ReservaStatus status,
            @Param("hoje") LocalDate hoje,
            @Param("agora") LocalTime agora
    );
}
