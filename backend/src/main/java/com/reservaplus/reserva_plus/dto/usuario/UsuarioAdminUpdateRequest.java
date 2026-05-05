package com.reservaplus.reserva_plus.dto.usuario;

import com.reservaplus.reserva_plus.model.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioAdminUpdateRequest {

    @NotNull(message = "Informe o perfil do usuário.")
    private UserRole role;

    @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.")
    private String senha;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
