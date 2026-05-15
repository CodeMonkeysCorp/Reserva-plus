package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.Reserva;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
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

    long countByStatus(ReservaStatus status);

    long countByStatusAndData(ReservaStatus status, LocalDate data);

    List<Reserva> findByUsuarioIdAndDataBetweenOrderByDataDescHorarioInicioDesc(
            Long usuarioId,
            LocalDate dataInicial,
            LocalDate dataFinal
    );

    List<Reserva> findByData(LocalDate data);

    List<Reserva> findByDataBetweenOrderByDataDescHorarioInicioDesc(LocalDate dataInicial, LocalDate dataFinal);

    List<Reserva> findAllByOrderByDataDescHorarioInicioDesc();

    List<Reserva> findByEspacoIdAndDataAndStatusOrderByHorarioInicio(Long espacoId, LocalDate data, ReservaStatus status);

    @Query("""
            select count(distinct r.usuario.id)
            from Reserva r
            """)
    long countDistinctUsuariosComReserva();

    @Query("""
            select count(r)
            from Reserva r
            where r.status = :status
              and r.data = :hoje
              and r.horarioInicio <= :agora
              and r.horarioFim > :agora
            """)
    long countReservasEmAndamento(
            @Param("status") ReservaStatus status,
            @Param("hoje") LocalDate hoje,
            @Param("agora") LocalTime agora
    );

    @Query("""
            select count(r)
            from Reserva r
            where r.status = :status
              and (
                r.data > :hoje
                or (r.data = :hoje and r.horarioInicio > :agora)
              )
            """)
    long countReservasFuturas(
            @Param("status") ReservaStatus status,
            @Param("hoje") LocalDate hoje,
            @Param("agora") LocalTime agora
    );

    @EntityGraph(attributePaths = {"usuario", "espaco"})
    @Query("""
            select r
            from Reserva r
            where r.status = :status
              and (
                r.data > :hoje
                or (r.data = :hoje and r.horarioFim > :agora)
              )
            order by
              case
                when r.data = :hoje and r.horarioInicio <= :agora and r.horarioFim > :agora then 0
                else 1
              end,
              r.data asc,
              r.horarioInicio asc
            """)
    Page<Reserva> findAgendaGeralPaginada(
            @Param("status") ReservaStatus status,
            @Param("hoje") LocalDate hoje,
            @Param("agora") LocalTime agora,
            Pageable pageable
    );

    @Query("""
            select r.espaco.id as espacoId,
                   r.espaco.nome as nome,
                   r.espaco.tipo as tipo,
                   r.espaco.destaque as destaque,
                   count(r) as totalReservas,
                   sum(
                     case
                       when r.status = :statusAtiva
                        and (
                          r.data > :hoje
                          or (r.data = :hoje and r.horarioInicio > :agora)
                        )
                       then 1
                       else 0
                     end
                   ) as futuras,
                   sum(
                     case
                       when r.status = :statusAtiva
                        and (
                          r.data > :hoje
                          or (r.data = :hoje and r.horarioFim > :agora)
                        )
                       then 1
                       else 0
                     end
                   ) as agendaAtiva
            from Reserva r
            where r.status <> :statusCancelada
            group by r.espaco.id, r.espaco.nome, r.espaco.tipo, r.espaco.destaque
            order by
              count(r) desc,
              sum(
                case
                  when r.status = :statusAtiva
                   and (
                     r.data > :hoje
                     or (r.data = :hoje and r.horarioFim > :agora)
                   )
                  then 1
                  else 0
                end
              ) desc,
              r.espaco.nome asc
            """)
    List<PainelEspacoRankingProjection> findTopEspacosPainel(
            @Param("statusAtiva") ReservaStatus statusAtiva,
            @Param("statusCancelada") ReservaStatus statusCancelada,
            @Param("hoje") LocalDate hoje,
            @Param("agora") LocalTime agora,
            Pageable pageable
    );

    @Query("""
            select r.espaco.id as espacoId,
                   r.espaco.nome as nome,
                   r.espaco.tipo as tipo,
                   r.espaco.destaque as destaque,
                   count(r) as totalReservas,
                   sum(
                     case
                       when r.status = :statusAtiva
                        and (
                          r.data > :hoje
                          or (r.data = :hoje and r.horarioInicio > :agora)
                        )
                       then 1
                       else 0
                     end
                   ) as futuras,
                   sum(
                     case
                       when r.status = :statusAtiva
                        and (
                          r.data > :hoje
                          or (r.data = :hoje and r.horarioFim > :agora)
                        )
                       then 1
                       else 0
                     end
                   ) as agendaAtiva
            from Reserva r
            where r.status <> :statusCancelada
            group by r.espaco.id, r.espaco.nome, r.espaco.tipo, r.espaco.destaque
            having sum(
              case
                when r.status = :statusAtiva
                 and (
                   r.data > :hoje
                   or (r.data = :hoje and r.horarioFim > :agora)
                 )
                then 1
                else 0
              end
            ) > 0
            order by
              sum(
                case
                  when r.status = :statusAtiva
                   and (
                     r.data > :hoje
                     or (r.data = :hoje and r.horarioFim > :agora)
                   )
                  then 1
                  else 0
                end
              ) desc,
              count(r) desc,
              r.espaco.nome asc
            """)
    List<PainelEspacoRankingProjection> findEspacosConcorridosPainel(
            @Param("statusAtiva") ReservaStatus statusAtiva,
            @Param("statusCancelada") ReservaStatus statusCancelada,
            @Param("hoje") LocalDate hoje,
            @Param("agora") LocalTime agora,
            Pageable pageable
    );

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
