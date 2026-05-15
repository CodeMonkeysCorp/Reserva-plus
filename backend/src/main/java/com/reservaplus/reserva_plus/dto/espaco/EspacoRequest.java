package com.reservaplus.reserva_plus.dto.espaco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public class EspacoRequest {

    @NotBlank(message = "Informe o nome do espaco.")
    @Size(min = 2, max = 120, message = "O nome do espaco deve ter entre 2 e 120 caracteres.")
    private String nome;

    @NotBlank(message = "Selecione o tipo do espaco.")
    @Size(max = 60, message = "O tipo do espaco deve ter no maximo 60 caracteres.")
    private String tipo;

    @Size(max = 500, message = "A descricao deve ter no maximo 500 caracteres.")
    private String descricao;

    @Size(max = 255, message = "A chave da imagem deve ter no maximo 255 caracteres.")
    private String imagemObjectKey;

    @NotNull(message = "Informe o horario inicial de funcionamento.")
    private LocalTime horarioFuncionamentoInicio;

    @NotNull(message = "Informe o horario final de funcionamento.")
    private LocalTime horarioFuncionamentoFim;

    private Boolean ativo = true;
    private Boolean destaque;

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

    public String getImagemObjectKey() {
        return imagemObjectKey;
    }

    public void setImagemObjectKey(String imagemObjectKey) {
        this.imagemObjectKey = imagemObjectKey;
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

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getDestaque() {
        return destaque;
    }

    public void setDestaque(Boolean destaque) {
        this.destaque = destaque;
    }
}
