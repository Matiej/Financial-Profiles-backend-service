package com.emat.reapi.ai.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SchemaRegistry {
    private final ResourceLoader resourceLoader;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> idByNameVersion = new ConcurrentHashMap<>();

    @PostConstruct
    void initDefault() {
        register("InsightReport", "v1", "classpath:ai/schemas/open_ai_json_schema_v1.json");
    }

    public void register(String name, String version, String location) {
        String key = key(name, version);
        try {
            Resource res = resourceLoader.getResource(location);
            String raw = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            mapper.readTree(raw);
            cache.put(key, raw);
            idByNameVersion.put(key, location);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load schema " + name + "@" + version + " from " + location, e);
        }
    }

    public String get(String name, String version) {
        String key = key(name, version);
        String schema = cache.get(key);
        if (schema == null) throw new IllegalArgumentException("Schema not found: " + key);
        return schema;
    }

    private static String key(String name, String version) {
        return name + ":" + version;
    }
}
