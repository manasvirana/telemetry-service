package com.lane.telemetry.api;

import com.lane.telemetry.db.TelemetryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class QueryService {

    private final TelemetryRepository repo;

    public QueryService(TelemetryRepository repo) {
        this.repo = repo;
    }

    public Mono<SummaryResponse> summary(String deviceId, Long from, Long to) {
        if (deviceId == null || deviceId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "device_id is required"));
        }
        if (from == null || to == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "from and to are required"));
        }
        if (from > to) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "from must be less than or equal to to"));
        }
        return repo.summary(deviceId, from, to);
    }
}