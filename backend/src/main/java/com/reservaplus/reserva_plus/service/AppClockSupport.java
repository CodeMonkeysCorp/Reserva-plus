package com.reservaplus.reserva_plus.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

final class AppClockSupport {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    private AppClockSupport() {
    }

    static LocalDate nowDate() {
        return LocalDate.now(APP_ZONE);
    }

    static LocalTime nowTime() {
        return LocalTime.now(APP_ZONE).withSecond(0).withNano(0);
    }
}
