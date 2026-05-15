package com.reservaplus.reserva_plus.repository;

public interface PainelEspacoRankingProjection {

    Long getEspacoId();

    String getNome();

    String getTipo();

    Boolean getDestaque();

    Long getTotalReservas();

    Long getFuturas();

    Long getAgendaAtiva();
}
