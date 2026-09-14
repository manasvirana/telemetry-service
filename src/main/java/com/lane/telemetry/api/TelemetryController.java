package com.lane.telemetry.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final IngestService ingestService;
    private final QueryService queryService;

    public TelemetryController(IngestService ingestService, QueryService queryService) {
        this.ingestService = ingestService;
        this.queryService = queryService;
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.OK)
    public Mono<IngestResponse> ingest(@RequestBody IngestRequest request) {
        return ingestService.ingest(request);
    }

    @GetMapping("/summary")
    public Mono<SummaryResponse> summary(
            @RequestParam(required = false) String device_id,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {
        return queryService.summary(device_id, from, to);
    }
}