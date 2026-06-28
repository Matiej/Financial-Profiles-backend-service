package com.emat.reapi.ai.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OpenAiParams {
    private final String openApiKey;
    private final String baseUrl;
}
