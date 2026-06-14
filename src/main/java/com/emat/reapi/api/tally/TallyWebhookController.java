package com.emat.reapi.api.tally;

import com.emat.reapi.clienttalytest.tally.TallyService;
import com.emat.reapi.clienttalytest.tally.TallySignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/tally")
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class TallyWebhookController {
    private final TallySignatureVerifier verifier;
    private final ObjectMapper objectMapper;
    private final TallyService tallyService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, String>>> handleTallyHook(
            @RequestHeader(value = "Tally-Signature", required = false) String signature,
            @RequestBody String rawBody
    ) {

        if (!verifier.verify(rawBody, signature)) {
            log.warn("Tally signature verification FAILED.");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        final TallyWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, TallyWebhookEvent.class);
        } catch (Exception e) {
            log.error("Parse error", e);
            return Mono.just(ResponseEntity.badRequest().body(Map.of("status", "bad_request")));
        }

        return tallyService.processTallyEvent(event)
                .doOnSubscribe(s -> log.info("Processing Tally event..."))
                .thenReturn(ResponseEntity.ok().body(Map.of("status", "ok")))
                .doOnSuccess(s-> log.info("Tally event processed successfully, submissionID: {}", event.getData().getSubmissionId()))
                .onErrorResume(e -> {
                    log.error("Error while processing Tally event", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error")));
                });
    }
}

