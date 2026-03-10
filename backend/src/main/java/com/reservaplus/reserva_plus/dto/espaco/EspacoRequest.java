package com.reservaplus.reserva_plus.dto.espaco;

import com.reservaplus.reserva_plus.model.EspacoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EspacoRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String nome;

    @NotNull
    private EspacoTipo tipo;

    @Size(max = 500)
    private String descricao;

    private Boolean ativo = true;

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

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
