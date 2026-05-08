package com.reservaplus.reserva_plus.dto.espaco;

import java.time.LocalTime;

public class EspacoResponse {

    private Long id;
    private String nome;
    private String tipo;
    private String descricao;
    private boolean ativo;
    private LocalTime horarioFuncionamentoInicio;
    private LocalTime horarioFuncionamentoFim;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalTime getHorarioFuncionamentoInicio() {
        return horarioFuncionamentoInicio;
    }

    public void setHorarioFuncionamentoInicio(LocalTime horarioFuncionamentoInicio) {
        this.horarioFuncionamentoInicio = horarioFuncionamentoInicio;
    }

    public LocalTime getHorarioFuncionamentoFim() {
        return horarioFuncionamentoFim;
    }

    public void setHorarioFuncionamentoFim(LocalTime horarioFuncionamentoFim) {
        this.horarioFuncionamentoFim = horarioFuncionamentoFim;
    }
}
