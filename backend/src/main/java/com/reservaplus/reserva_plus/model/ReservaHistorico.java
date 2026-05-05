package com.reservaplus.reserva_plus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reserva_historicos",
        indexes = {
                @Index(name = "idx_reserva_historico_reserva_data", columnList = "reserva_id,alterado_em"),
                @Index(name = "idx_reserva_historico_origem", columnList = "origem")
        }
)
public class ReservaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private ReservaStatus statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private ReservaStatus statusNovo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReservaHistoricoOrigem origem;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;

    @PrePersist
    public void prePersist() {
        if (alteradoEm == null) {
            alteradoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public ReservaStatus getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(ReservaStatus statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public ReservaStatus getStatusNovo() {
        return statusNovo;
    }

    public void setStatusNovo(ReservaStatus statusNovo) {
        this.statusNovo = statusNovo;
    }

    public ReservaHistoricoOrigem getOrigem() {
        return origem;
    }

    public void setOrigem(ReservaHistoricoOrigem origem) {
        this.origem = origem;
    }

    public LocalDateTime getAlteradoEm() {
        return alteradoEm;
    }

    public void setAlteradoEm(LocalDateTime alteradoEm) {
        this.alteradoEm = alteradoEm;
    }
}
