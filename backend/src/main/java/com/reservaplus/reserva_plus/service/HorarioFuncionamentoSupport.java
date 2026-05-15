package com.reservaplus.reserva_plus.service;

import com.reservaplus.reserva_plus.exception.BadRequestException;
import com.reservaplus.reserva_plus.model.Espaco;

import java.time.LocalTime;

final class HorarioFuncionamentoSupport {

    static final LocalTime HORARIO_PADRAO_INICIO = LocalTime.of(6, 0);
    static final LocalTime HORARIO_PADRAO_FIM = LocalTime.of(23, 0);

    private HorarioFuncionamentoSupport() {
    }

    static LocalTime resolveOrDefault(LocalTime value, LocalTime fallback) {
        return value != null ? value : fallback;
    }

    static LocalTime resolveHorarioInicio(Espaco espaco) {
        return resolveOrDefault(espaco.getHorarioFuncionamentoInicio(), HORARIO_PADRAO_INICIO);
    }

    static LocalTime resolveHorarioFim(Espaco espaco) {
        return resolveOrDefault(espaco.getHorarioFuncionamentoFim(), HORARIO_PADRAO_FIM);
    }

    static void validateHoraCheia(
            LocalTime inicio,
            LocalTime fim,
            String horaCheiaMessage,
            String ordemMessage
    ) {
        if (!isHoraCheia(inicio) || !isHoraCheia(fim)) {
            throw new BadRequestException(horaCheiaMessage);
        }

        if (!fim.isAfter(inicio)) {
            throw new BadRequestException(ordemMessage);
        }
    }

    static void validateDentroDoHorarioFuncionamento(
            Espaco espaco,
            LocalTime inicio,
            LocalTime fim,
            String horarioForaDaJanelaMessage
    ) {
        LocalTime funcionamentoInicio = resolveHorarioInicio(espaco);
        LocalTime funcionamentoFim = resolveHorarioFim(espaco);

        if (inicio.isBefore(funcionamentoInicio) || fim.isAfter(funcionamentoFim)) {
            throw new BadRequestException(
                    String.format(horarioForaDaJanelaMessage, funcionamentoInicio, funcionamentoFim)
            );
        }
    }

    private static boolean isHoraCheia(LocalTime value) {
        return value.getMinute() == 0 && value.getSecond() == 0 && value.getNano() == 0;
    }
}
