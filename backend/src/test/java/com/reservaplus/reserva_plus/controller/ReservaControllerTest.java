package com.reservaplus.reserva_plus.controller;

import com.reservaplus.reserva_plus.dto.reserva.ReservaResponse;
import com.reservaplus.reserva_plus.exception.GlobalExceptionHandler;
import com.reservaplus.reserva_plus.model.ReservaStatus;
import com.reservaplus.reserva_plus.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private ReservaService reservaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaController(reservaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createShouldAcceptTodayReservationEvenWhenJvmTimezoneIsUtc() throws Exception {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        try {
            LocalDate hojeSaoPaulo = LocalDate.now(APP_ZONE);
            ReservaResponse response = buildResponse(hojeSaoPaulo);

            given(reservaService.create(any(), eq("morador@teste.com"))).willReturn(response);

            MockHttpServletResponse httpResponse = mockMvc.perform(
                    post("/api/reservas")
                            .principal(new UsernamePasswordAuthenticationToken("morador@teste.com", "senha"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "espacoId": 1,
                                      "data": "%s",
                                      "horarioInicio": "22:00",
                                      "horarioFim": "23:00"
                                    }
                                    """.formatted(hojeSaoPaulo))
            ).andReturn().getResponse();

            assertEquals(201, httpResponse.getStatus());
            verify(reservaService).create(any(), eq("morador@teste.com"));
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    private ReservaResponse buildResponse(LocalDate data) {
        ReservaResponse response = new ReservaResponse();
        response.setId(10L);
        response.setUsuarioId(1L);
        response.setUsuarioNome("Morador");
        response.setEspacoId(1L);
        response.setEspacoNome("Quadra 1");
        response.setData(data);
        response.setHorarioInicio(LocalTime.of(22, 0));
        response.setHorarioFim(LocalTime.of(23, 0));
        response.setStatus(ReservaStatus.ATIVA);
        response.setCriadoEm(LocalDateTime.of(2026, 5, 14, 21, 37));
        return response;
    }
}
