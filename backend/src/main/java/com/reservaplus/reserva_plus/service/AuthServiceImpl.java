package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.auth.AuthResponse;
import com.reservaplus.reserva_plus.dto.auth.LoginRequest;
import com.reservaplus.reserva_plus.dto.auth.RegisterRequest;
import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.exception.NotFoundException;
import com.reservaplus.reserva_plus.model.UserRole;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import com.reservaplus.reserva_plus.security.JwtService;
import com.reservaplus.reserva_plus.support.EmailAddressSupport;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = EmailAddressSupport.normalize(request.getEmail());

        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("E-mail ja cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(normalizedEmail);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(UserRole.USER);
        usuario = usuarioRepository.save(usuario);

        return buildAuthResponse(usuario);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = EmailAddressSupport.normalize(request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getSenha())
            );
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Credenciais invalidas.");
        }

        Usuario usuario = findByEmailOrThrow(email);

        return buildAuthResponse(usuario);
    }

    @Override
    public AuthResponse me(String email) {
        Usuario usuario = findByEmailOrThrow(email);

        return buildAuthResponse(usuario);
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), usuario.getRole());
        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }

    private Usuario findByEmailOrThrow(String email) {
        return usuarioRepository.findByEmailIgnoreCase(EmailAddressSupport.normalize(email))
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
    }
}
