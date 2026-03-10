package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.Espaco;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EspacoRepository extends JpaRepository<Espaco, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espaco e where e.id = :id")
    Optional<Espaco> findByIdForUpdate(@Param("id") Long id);
}
