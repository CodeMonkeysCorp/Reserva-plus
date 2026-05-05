package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.usuario.UsuarioAdminResponse;
import com.reservaplus.reserva_plus.dto.usuario.UsuarioAdminUpdateRequest;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.UserRole;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> findAll() {
        return usuarioRepository.findAllByOrderByNomeAscEmailAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioAdminResponse updateAdminData(Long usuarioId, UsuarioAdminUpdateRequest request, String actorEmail) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        validateRoleChange(usuario, request.getRole(), actorEmail);

        usuario.setRole(request.getRole());

        if (request.getSenha() != null && !request.getSenha().trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha().trim()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    private void validateRoleChange(Usuario usuario, UserRole newRole, String actorEmail) {
        if (usuario.getRole() == newRole) {
            return;
        }

        if (usuario.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new BadRequestException("Você não pode alterar o próprio perfil de acesso.");
        }

        if (usuario.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            long totalAdmins = usuarioRepository.countByRole(UserRole.ADMIN);
            if (totalAdmins <= 1) {
                throw new BadRequestException("O sistema precisa manter pelo menos um administrador.");
            }
        }
    }

    private UsuarioAdminResponse toResponse(Usuario usuario) {
        UsuarioAdminResponse response = new UsuarioAdminResponse();
        response.setId(usuario.getId());
        response.setNome(usuario.getNome());
        response.setEmail(usuario.getEmail());
        response.setRole(usuario.getRole());
        return response;
    }
}
