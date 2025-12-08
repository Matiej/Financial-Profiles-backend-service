package com.emat.reapi.ai.port;

import com.emat.reapi.ai.integration.OpenAiClientFactory;
import com.emat.reapi.ai.schema.SchemaRegistry;
import com.emat.reapi.ai.validator.SchemaJsonValidator;
import com.emat.reapi.profileanalysis.domain.InsightReportAiResponse;
import com.emat.reapi.profileanalysis.domain.InsightReportStructuredAiResponse;
import com.emat.reapi.profileanalysis.domain.MinimizedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class AiInsightAnalyzerServiceImpl implements AiInsightAnalyzerService {

    private final OpenAiClientFactory openAiClientFactory;
    private final SchemaRegistry schemaRegistry;
    private final SchemaJsonValidator validator;
    private final ObjectMapper objectMapper;

    private static final String SCHEMA_NAME = "InsightReport";
    private static final String SCHEMA_VERSION = "v1";
    private static final OpenAiApi.ChatModel MODEL = OpenAiApi.ChatModel.GPT_4_O_MINI;
    @Value("${prompt.analytics}")
    private String userPrompt;

    public AiInsightAnalyzerServiceImpl(OpenAiClientFactory openAiClientFactory, SchemaRegistry schemaRegistry, SchemaJsonValidator validator, ObjectMapper objectMapper) {
        this.openAiClientFactory = openAiClientFactory;
        this.schemaRegistry = schemaRegistry;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<InsightReportAiResponse> analyze(MinimizedPayload payload) {
        return Mono.fromSupplier(() -> doCall(payload, false))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(120)) //todo poprawic
                .doOnSuccess(suc -> log.info("successfully analyzed payload: {}", suc.getSubmissionId()))
                .onErrorResume(ex -> {
                    log.warn("AI call failed ({}). Retrying with stricter instruction…", ex.getMessage());
                    return Mono.fromSupplier(() -> doCall(payload, true))
                            .doOnSuccess(suc -> log.info("successfully analyzed payload after fall: {}", suc.getSubmissionId()));
                });
    }

    private InsightReportAiResponse doCall(MinimizedPayload payload, boolean fixPass) {
        var model = openAiClientFactory.createStructuredOutputChatModel(MODEL);
        var schemaJson = schemaRegistry.get(SCHEMA_NAME, SCHEMA_VERSION);
        var options = openAiClientFactory.structuredOutputOptions(MODEL, SCHEMA_NAME, schemaJson, true);

        //todo refactor prompts, prepare some yml with them or so (allow to add from with request
//        var system = new SystemMessage
//                (fixPass
//                        ? "Zwróć wyłącznie JSON zgodny z dostarczonym schematem. Bez komentarzy, tylko JSON."
//                        : "Jesteś analityczką wzorców finansowych. Zwróć wyłącznie JSON zgodny ze schematem."
//                );
        var system = new SystemMessage(userPrompt);
        var user = new UserMessage(write(payload));

        Prompt prompt = new Prompt(List.of(system, user), options);
        String raw = model.call(prompt).getResult().getOutput().getText();

        validator.validate(raw, schemaJson);
        InsightReportStructuredAiResponse insightReportStructuredAiResponse = read(raw, InsightReportStructuredAiResponse.class);
        return InsightReportAiResponse.builder()
                .clientName(payload.getClientName())
                .clientId(payload.getClientId())
                .submissionId(payload.getSubmissionId())
                .testName(payload.getTestName())
                .schemaName(SCHEMA_NAME)
                .model(MODEL.getName())
                .schemaVersion(SCHEMA_VERSION)
                .rawJson(raw)
                .insightReportStructuredAiResponse(insightReportStructuredAiResponse)
                .build();
    }

    private String write(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize payload", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize AI response", e);
        }
    }
}
