package com.reservaplus.reserva_plus.dto.espaco;

import com.reservaplus.reserva_plus.model.EspacoTipo;

public class EspacoResponse {

    private Long id;
    private String nome;
    private EspacoTipo tipo;
    private String descricao;
    private boolean ativo;

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

    public EspacoTipo getTipo() {
        return tipo;
    }

    public void setTipo(EspacoTipo tipo) {
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
}
