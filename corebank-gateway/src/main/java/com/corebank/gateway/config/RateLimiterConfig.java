package com.corebank.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

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
