package com.reservaplus.reserva_plus.config;

import com.reservaplus.reserva_plus.model.UserRole;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import com.reservaplus.reserva_plus.support.EmailAddressSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_ADMIN_NAME:Administrador}")
    private String adminName;

    @Value("${APP_ADMIN_EMAIL:admin@reserva.local}")
    private String adminEmail;

    @Value("${APP_ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    public AdminInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String normalizedAdminEmail = EmailAddressSupport.normalize(adminEmail);
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedAdminEmail)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome(adminName);
        admin.setEmail(normalizedAdminEmail);
        admin.setSenha(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        usuarioRepository.save(admin);
    }
}
