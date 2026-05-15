package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.auth.AuthResponse;
import com.reservaplus.reserva_plus.dto.auth.LoginRequest;
import com.reservaplus.reserva_plus.dto.auth.RegisterRequest;
import com.reservaplus.reserva_plus.exception.ConflictException;
import com.reservaplus.reserva_plus.model.UserRole;
import com.reservaplus.reserva_plus.model.Usuario;
import com.reservaplus.reserva_plus.repository.UsuarioRepository;
import com.reservaplus.reserva_plus.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void registerShouldNormalizeEmailBeforeCheckingDuplicatesAndSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("  Morador Teste  ");
        request.setEmail("  MORADOR@Teste.com  ");
        request.setSenha("senha123");

        given(usuarioRepository.existsByEmailIgnoreCase("morador@teste.com")).willReturn(false);
        given(passwordEncoder.encode("senha123")).willReturn("senha-hash");
        given(usuarioRepository.save(any(Usuario.class))).willAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(7L);
            return usuario;
        });
        given(jwtService.generateToken(7L, "morador@teste.com", UserRole.USER)).willReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());

        Usuario savedUsuario = usuarioCaptor.getValue();
        assertEquals("Morador Teste", savedUsuario.getNome());
        assertEquals("morador@teste.com", savedUsuario.getEmail());
        assertEquals("senha-hash", savedUsuario.getSenha());
        assertEquals(UserRole.USER, savedUsuario.getRole());
        assertEquals("morador@teste.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void registerShouldRejectDuplicateEmailIgnoringCase() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Morador");
        request.setEmail(" MORADOR@teste.com ");
        request.setSenha("senha123");

        given(usuarioRepository.existsByEmailIgnoreCase("morador@teste.com")).willReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> authService.register(request));

        assertEquals("E-mail ja cadastrado.", exception.getMessage());
        verifyNoInteractions(passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void loginShouldAuthenticateAndLoadUserWithNormalizedEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  MORADOR@Teste.com  ");
        request.setSenha("senha123");

        Usuario usuario = new Usuario();
        usuario.setId(11L);
        usuario.setNome("Morador");
        usuario.setEmail("morador@teste.com");
        usuario.setSenha("senha-hash");
        usuario.setRole(UserRole.USER);

        given(usuarioRepository.findByEmailIgnoreCase("morador@teste.com")).willReturn(Optional.of(usuario));
        given(jwtService.generateToken(11L, "morador@teste.com", UserRole.USER)).willReturn("jwt-login");

        AuthResponse response = authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());

        UsernamePasswordAuthenticationToken token = tokenCaptor.getValue();
        assertEquals("morador@teste.com", token.getPrincipal());
        assertEquals("senha123", token.getCredentials());
        assertEquals("jwt-login", response.getToken());
        assertEquals("morador@teste.com", response.getEmail());
    }

    @Test
    void meShouldReturnCurrentAuthenticatedSession() {
        Usuario usuario = new Usuario();
        usuario.setId(15L);
        usuario.setNome("Administrador");
        usuario.setEmail("admin@teste.com");
        usuario.setSenha("senha-hash");
        usuario.setRole(UserRole.ADMIN);

        given(usuarioRepository.findByEmailIgnoreCase("admin@teste.com")).willReturn(Optional.of(usuario));
        given(jwtService.generateToken(15L, "admin@teste.com", UserRole.ADMIN)).willReturn("jwt-admin");

        AuthResponse response = authService.me(" ADMIN@teste.com ");

        assertEquals("jwt-admin", response.getToken());
        assertEquals(15L, response.getId());
        assertEquals("Administrador", response.getNome());
        assertEquals("admin@teste.com", response.getEmail());
        assertEquals(UserRole.ADMIN, response.getRole());
    }
}
