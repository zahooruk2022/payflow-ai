package com.demo.payflowai.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Tanzu Platform GenAI service bindings from VCAP_SERVICES to Spring AI properties.
 *
 * Each bound "ai-models" service instance exposes:
 *   credentials.endpoint.openai_api_base  →  per-type base URL
 *   credentials.endpoint.api_key          →  per-type JWT token
 *
 * Binding whose CF instance name contains "embed" is treated as the embedding model;
 * all others are treated as the chat model.
 *
 * On CF:  properties are set from VCAP_SERVICES, overriding application.yml.
 * Locally: VCAP_SERVICES is absent, so application.yml defaults apply unchanged.
 */
public class GenAiVcapPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String vcap = environment.getProperty("VCAP_SERVICES");
        if (vcap == null || vcap.isBlank()) return;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(vcap);
            JsonNode bindings = root.path("ai-models");
            if (!bindings.isArray() || bindings.isEmpty()) return;

            Map<String, Object> props = new HashMap<>();

            for (JsonNode binding : bindings) {
                JsonNode endpoint = binding.path("credentials").path("endpoint");
                String instanceName = binding.path("name").asText("");
                String plan        = binding.path("plan").asText("");

                String openaiBase = endpoint.path("openai_api_base").asText(null);
                String apiKey     = endpoint.path("api_key").asText(null);

                if (openaiBase == null || apiKey == null) continue;

                boolean isEmbedding = instanceName.contains("embed") || plan.contains("embed");

                if (isEmbedding) {
                    props.put("spring.ai.openai.embedding.base-url", openaiBase);
                    props.put("spring.ai.openai.embedding.api-key",  apiKey);
                } else {
                    props.put("spring.ai.openai.chat.base-url", openaiBase);
                    props.put("spring.ai.openai.chat.api-key",  apiKey);
                }
            }

            if (!props.isEmpty()) {
                environment.getPropertySources().addFirst(
                    new MapPropertySource("genAiVcapBinding", props)
                );
            }
        } catch (Exception ignored) {
            // Fail silently — missing or malformed VCAP_SERVICES falls back to application.yml
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
