package com.emat.reapi.ai.configuration;

import com.emat.reapi.ai.integration.OpenAiClientFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorNettyClientRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class OpenAIiConfiguration {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    public static final int CONNECT_TIMEOUT_MS = 30_000;
    public static final int READ_WRITE_TIMEOUT_S = 560;
    public static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(480);

    @Bean
    public OpenAiParams getOpeApiParams() {
        return new OpenAiParams(apiKey, baseUrl);
    }

    private static ExchangeFilterFunction logRequestAndResponse() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("Request: {} {}", request.method(), request.url());
            request.headers().forEach((name, values) -> values.forEach(value ->
                    log.info("Request Header: {}={}", name, value)));
            return Mono.just(request);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("Response status: {}", response.statusCode());
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .doOnNext(body -> log.info("Response body: {}", body))
                    .then(Mono.just(response));
        }));
    }

    @Bean
    public HttpClient openAiHttpClient() {
        return HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_WRITE_TIMEOUT_S))
                        .addHandlerLast(new WriteTimeoutHandler(READ_WRITE_TIMEOUT_S)))
                .wiretap("reactor.netty.http.client.HttpClient");
    }

    @Bean
    @Qualifier("openAiRestClientBuilder")
    public RestClient.Builder openAiRestClientBuilder(HttpClient openAiHttpClient) {
        var connector = new ReactorClientHttpConnector(openAiHttpClient);
        var requestFactory = new ReactorNettyClientRequestFactory(openAiHttpClient);
        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    @Bean
    @Qualifier("openAiWebClientBuilder")
    public WebClient.Builder openAiWebClientBuilder(HttpClient openAiHttpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(openAiHttpClient))
                // bigger http buffer in case of large json s
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10 MB
                        .build());
    }
    @Bean
    public SimpleVectorStore simpleVectorStore(OpenAiClientFactory factory) {
        OpenAiEmbeddingModel model = factory.createEmbeddingModel();
        return SimpleVectorStore.builder(model).build();
    }

    @Bean
    public RetryTemplate openAiChatRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(4)
                .exponentialBackoff(Duration.ofSeconds(1), 2.0,  // 1s, 2s, 4s, 8s
                        Duration.ofSeconds(20))
                .build();
    }
}
