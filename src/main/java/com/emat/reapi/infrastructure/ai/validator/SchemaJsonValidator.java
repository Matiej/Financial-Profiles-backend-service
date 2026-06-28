package com.emat.reapi.ai.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SchemaJsonValidator {
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public void validate(String json, String schemaJson) {
        try {
            JsonNode data = om.readTree(json);
            JsonNode schemaNode = om.readTree(schemaJson);
            JsonSchema schema = factory.getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(data);
            if (!errors.isEmpty()) {
                String msg = errors.stream().map(ValidationMessage::getMessage).reduce((a,b)->a+"; "+b).orElse("Schema validation failed");
                throw new IllegalArgumentException("InsightReport payload invalid: " + msg);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("InsightReport payload invalid: " + e.getMessage(), e);
        }
    }
}
