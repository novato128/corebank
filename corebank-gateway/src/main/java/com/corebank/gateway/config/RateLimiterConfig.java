package com.corebank.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Cada conta tem seu próprio "balde" de tokens: um cliente sozinho
     * fazendo muita requisição não consome o limite de outro. Se o
     * cabeçalho X-Conta-Id não vier (ex: chamada anônima), cai para o IP.
     */
    @Bean
    public KeyResolver contaKeyResolver() {
        return exchange -> {
            String contaId = exchange.getRequest().getHeaders().getFirst("X-Conta-Id");
            if (contaId != null && !contaId.isBlank()) {
                return Mono.just(contaId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "desconhecido";
            return Mono.just(ip);
        };
    }
}
