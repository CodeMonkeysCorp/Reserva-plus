package com.reservaplus.reserva_plus.dto.espaco;

import com.reservaplus.reserva_plus.model.EspacoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public class EspacoRequest {

    @NotBlank(message = "Informe o nome do espaço.")
    @Size(min = 2, max = 120, message = "O nome do espaço deve ter entre 2 e 120 caracteres.")
    private String nome;

    @NotNull(message = "Selecione o tipo do espaço.")
    private EspacoTipo tipo;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    private String descricao;

    @NotNull(message = "Informe o horário inicial de funcionamento.")
    private LocalTime horarioFuncionamentoInicio;

    @NotNull(message = "Informe o horário final de funcionamento.")
    private LocalTime horarioFuncionamentoFim;

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
}
