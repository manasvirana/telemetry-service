package com.lane.telemetry.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IngestRateLimitFilter implements WebFilter {

    private static final int LIMIT_PER_SECOND = 50;
    private final AtomicInteger count = new AtomicInteger();
    private long windowStart = System.currentTimeMillis();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!"/api/v1/telemetry/ingest".equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        synchronized (this) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 1000) {
                windowStart = now;
                count.set(0);
            }
            if (count.incrementAndGet() > LIMIT_PER_SECOND) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }
}