package com.reservaplus.reserva_plus.repository;

import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    List<Usuario> findAllByOrderByNomeAscEmailAsc();
}
