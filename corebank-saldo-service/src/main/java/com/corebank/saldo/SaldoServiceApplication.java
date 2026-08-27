package com.corebank.saldo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SaldoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaldoServiceApplication.class, args);
    }
}
