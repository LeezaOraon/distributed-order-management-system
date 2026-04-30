package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import org.springframework.context.annotation.Primary;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Rate limit key: one bucket per IP address.
     * Swap this for username-based limiting once auth is applied
     * by reading the X-Auth-User header instead.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(
                        exchange.getRequest().getRemoteAddress()
                ).getAddress().getHostAddress()
        );
    }

    /**
     * User-based key resolver — use this after JWT filter populates X-Auth-User.
     * To activate: change key-resolver in application.yml to "#{@userKeyResolver}"
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("X-Auth-User");
            return Mono.just(user != null ? user : "anonymous");
        };
    }
}