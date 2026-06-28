package com.emat.reapi.ai.integration;

import com.emat.reapi.ai.configuration.OpenAiParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClientFactory {
    private static final int MAX_TOKENS = 240;
    private static final double TEMPERATURE = 0.7;
    private static final int SO_MAX_TOKENS = 4111;   // raporty bywają dłuższe
    private static final double SO_TEMPERATURE = 0.2; // stabilniejsze JSON-y
    private static final Map<OpenAiApi.ChatModel, OpenAiChatModel> openAiChatModelMap =
            new EnumMap<>(OpenAiApi.ChatModel.class);
    private static final Map<OpenAiApi.ChatModel, OpenAiChatModel> structuredOutputModelMap =
            new EnumMap<>(OpenAiApi.ChatModel.class);
    private final OpenAiParams opeApiParams;
    private final WebClient.Builder openAiWebClientBuilder;
    private final RestClient.Builder restClientBuilder;
    private final RetryTemplate openAiChatRetryTemplate;

    // Modele, które sensownie wspierają JSON_SCHEMA/JSON mode
    private static final List<OpenAiApi.ChatModel> STRUCTURED_SUPPORTED = List.of(
            OpenAiApi.ChatModel.GPT_4_O,
            OpenAiApi.ChatModel.GPT_4_O_MINI,
            OpenAiApi.ChatModel.GPT_4_TURBO,
            OpenAiApi.ChatModel.GPT_4_1,
            OpenAiApi.ChatModel.GPT_4_1_MINI
    );

    public OpenAiClientFactory(
            OpenAiParams opeApiParams,
            @Qualifier("openAiWebClientBuilder") WebClient.Builder openAiWebClientBuilder,
            @Qualifier("openAiRestClientBuilder") RestClient.Builder restClientBuilder,
            RetryTemplate openAiChatRetryTemplate) {
        this.opeApiParams = opeApiParams;
        this.openAiWebClientBuilder = openAiWebClientBuilder;
        this.restClientBuilder = restClientBuilder;
        this.openAiChatRetryTemplate = openAiChatRetryTemplate;
        initializeOpenAiChatModels();
        initializeStructuredOutputModels();
    }

    private void initializeOpenAiChatModels() {
        for (OpenAiApi.ChatModel chatModelEnum : OpenAiApi.ChatModel.values()) {
            var options = OpenAiChatOptions.builder()
                    .model(chatModelEnum)
                    .maxTokens(MAX_TOKENS)
                    .temperature(TEMPERATURE)
                    .logprobs(true) // Enable log probabilities
                    .build();

            var openAiChatModel = OpenAiChatModel.builder()
                    .openAiApi(buildOpenAiApi())
                    .defaultOptions(options)
                    .retryTemplate(openAiChatRetryTemplate)
                    .build();
            openAiChatModelMap.put(chatModelEnum, openAiChatModel);
        }
    }

    private void initializeStructuredOutputModels() {
        for (OpenAiApi.ChatModel chatModelEnum : STRUCTURED_SUPPORTED) {
            var options = OpenAiChatOptions.builder()
                    .model(chatModelEnum)
                    .maxTokens(SO_MAX_TOKENS)     // ważne dla modeli „nie-reasoning”
                    .temperature(SO_TEMPERATURE)  // niższa temp. = stabilniejsze JSON-y
                    .build();

            var soModel = OpenAiChatModel.builder()
                    .openAiApi(buildOpenAiApi())
                    .defaultOptions(options)
                    .retryTemplate(openAiChatRetryTemplate)
                    .build();

            structuredOutputModelMap.put(chatModelEnum, soModel);
        }
    }

    public OpenAiChatModel createStructuredOutputChatModel(OpenAiApi.ChatModel chatModel) {
        return structuredOutputModelMap.get(chatModel);
    }

    public OpenAiChatModel createChatModel(OpenAiApi.ChatModel chatModel) {
        return openAiChatModelMap.get(chatModel);
    }

    public OpenAiChatOptions structuredOutputOptions(OpenAiApi.ChatModel chatModel,
                                                     String schemaName,
                                                     String jsonSchema,
                                                     boolean strict) {
        var rf = ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(ResponseFormat.JsonSchema.builder()
                        .name(schemaName)
                        .schema(jsonSchema)
                        .strict(strict)
                        .build())
                .build();

        return OpenAiChatOptions.builder()
                .model(chatModel)
                .maxTokens(SO_MAX_TOKENS)
                .temperature(SO_TEMPERATURE)
                .responseFormat(rf)
                .build();
    }

    public OpenAiAudioSpeechModel createAudioSpeechModel(
            OpenAiAudioApi.SpeechRequest.Voice voice,
            OpenAiAudioApi.SpeechRequest.AudioResponseFormat audioFormat,
            OpenAiAudioApi.TtsModel ttsModel
    ) {
        var options = OpenAiAudioSpeechOptions.builder()
                .voice(voice.value.toLowerCase())
                .speed(1.0f)
                .responseFormat(audioFormat)
                .model(ttsModel.value)
                .build();

        var audioApi = OpenAiAudioApi.builder()
                .baseUrl(opeApiParams.getBaseUrl())
                .apiKey(opeApiParams.getOpenApiKey())
                .webClientBuilder(openAiWebClientBuilder)
                .build();

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(1)
                .build();
        return new OpenAiAudioSpeechModel(audioApi, options, retryTemplate);
    }

    public OpenAiEmbeddingModel createEmbeddingModel() {
        var options = OpenAiEmbeddingOptions.builder()
                .model("text-embedding-3-small")
                .build();
        var meta = MetadataMode.ALL;

        return new OpenAiEmbeddingModel(buildOpenAiApi(), meta, options);
    }

    public OpenAiImageModel createImageModel() {
        var options = OpenAiImageOptions.builder()
                .model("dall-e-3")
                .quality("hd")
                .N(1)
                .build();
        var imageApi = OpenAiImageApi.builder()
                .apiKey(opeApiParams.getOpenApiKey())
                .baseUrl(opeApiParams.getBaseUrl())
                .restClientBuilder(restClientBuilder)
                .build();
        var retryTemplate = RetryTemplate.defaultInstance();
        return new OpenAiImageModel(imageApi, options, retryTemplate);
    }

    public OpenAiChatModel createChatModelWithFunctions(
            OpenAiApi.ChatModel chatModel,
            List<ToolCallback> toolCallbackList
    ) {

        var options = OpenAiChatOptions.builder()
                .model(chatModel)
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .toolCallbacks(toolCallbackList)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(buildOpenAiApi())
                .defaultOptions(options)
                .retryTemplate(openAiChatRetryTemplate)
                .build();
    }

    private OpenAiApi buildOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(opeApiParams.getBaseUrl())
                .apiKey(opeApiParams.getOpenApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }
}
