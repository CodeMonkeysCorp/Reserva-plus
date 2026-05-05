package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.dto.auth.AuthResponse;
import com.reservaplus.reserva_plus.dto.auth.LoginRequest;
import com.reservaplus.reserva_plus.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
