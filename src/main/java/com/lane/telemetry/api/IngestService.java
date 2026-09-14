package com.lane.telemetry.api;

import com.lane.telemetry.db.TelemetryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngestService {

    private final TelemetryRepository repo;

    public IngestService(TelemetryRepository repo) {
        this.repo = repo;
    }

    public Mono<IngestResponse> ingest(IngestRequest request) {
        if (request == null || request.deviceId() == null || request.deviceId().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "device_id is required"));
        }
        if (request.events() == null || request.events().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "events must be a non-empty array"));
        }
        if (request.events().size() > 500) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "events batch cannot exceed 500"));
        }

        String deviceId = request.deviceId();
        List<TelemetryEvent> events = request.events();
        List<IngestError> errors = new ArrayList<>();
        List<TelemetryEvent> valid = new ArrayList<>();

        for (int i = 0; i < events.size(); i++) {
            String reason = EventValidator.validate(events.get(i));
            if (reason != null) {
                errors.add(new IngestError(i, reason));
            } else {
                valid.add(events.get(i));
            }
        }

        if (valid.isEmpty()) {
            return Mono.just(new IngestResponse(deviceId, 0, 0, errors.size(), errors));
        }

        return Flux.fromIterable(valid)
                .concatMap(event -> repo.insertIgnoreDuplicate(deviceId, event))
                .collectList()
                .map(rowCounts -> {
                    int accepted = 0;
                    int duplicates = 0;
                    for (Long rows : rowCounts) {
                        if (rows != null && rows > 0) {
                            accepted++;
                        } else {
                            duplicates++;
                        }
                    }
                    return new IngestResponse(deviceId, accepted, duplicates, errors.size(), errors);
                });
    }
}