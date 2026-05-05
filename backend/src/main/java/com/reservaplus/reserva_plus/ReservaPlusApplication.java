package com.reservaplus.reserva_plus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservaPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservaPlusApplication.class, args);
    }
}
