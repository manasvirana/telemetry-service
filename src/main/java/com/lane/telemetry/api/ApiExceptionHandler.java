package com.lane.telemetry.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Map<String, String>>> handle(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "bad request";
        return Mono.just(ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", message)));
    }
}