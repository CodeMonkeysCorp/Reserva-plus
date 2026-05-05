package com.reservaplus.reserva_plus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservaStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaStatusScheduler.class);

    private final ReservaService reservaService;

    public ReservaStatusScheduler(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAoIniciar() {
        executarConclusaoAutomatica("inicializacao");
    }

    @Scheduled(
            cron = "${app.reservas.conclusao.cron:0 0 * * * *}",
            zone = "${app.reservas.conclusao.zone:America/Sao_Paulo}"
    )
    public void concluirReservasNoHorario() {
        executarConclusaoAutomatica("rotina-horaria");
    }

    private void executarConclusaoAutomatica(String origemExecucao) {
        int concluidas = reservaService.concluirReservasPendentes();
        if (concluidas > 0) {
            log.info("Conclusao automatica ({}) atualizou {} reserva(s).", origemExecucao, concluidas);
        }
    }
}
