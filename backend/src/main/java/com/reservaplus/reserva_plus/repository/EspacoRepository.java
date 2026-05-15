package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.Espaco;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EspacoRepository extends JpaRepository<Espaco, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espaco e where e.id = :id")
    Optional<Espaco> findByIdForUpdate(@Param("id") Long id);

    long countByAtivoTrue();

    long countByDestaqueTrue();

    @Query("""
            select count(e)
            from Espaco e
            where e.imagemObjectKey is null
               or trim(e.imagemObjectKey) = ''
            """)
    long countSemImagem();

    @Query("""
            select count(e)
            from Espaco e
            where e.descricao is null
               or trim(e.descricao) = ''
            """)
    long countSemDescricao();

    @Query("""
            select e.tipo as tipo,
                   count(e) as total,
                   sum(case when e.ativo = true then 1 else 0 end) as ativos
            from Espaco e
            group by e.tipo
            order by count(e) desc, e.tipo asc
            """)
    List<PainelTipoResumoProjection> summarizeByTipo();
}
