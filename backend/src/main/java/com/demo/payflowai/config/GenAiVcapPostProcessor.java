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
 * Maps the Tanzu Platform GenAI chat service binding from VCAP_SERVICES to Spring AI properties.
 *
 * The bound "ai-models" service instance exposes:
 *   credentials.endpoint.openai_api_base  →  spring.ai.openai.chat.base-url
 *   credentials.endpoint.api_key          →  spring.ai.openai.chat.api-key
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

            // Use the first ai-models binding for chat
            JsonNode endpoint = bindings.get(0).path("credentials").path("endpoint");
            String openaiBase = endpoint.path("openai_api_base").asText(null);
            String apiKey     = endpoint.path("api_key").asText(null);

            if (openaiBase == null || apiKey == null) return;

            Map<String, Object> props = new HashMap<>();
            props.put("spring.ai.openai.chat.base-url", openaiBase);
            props.put("spring.ai.openai.chat.api-key",  apiKey);

            environment.getPropertySources().addFirst(
                new MapPropertySource("genAiVcapBinding", props)
            );
        } catch (Exception ignored) {
            // Fail silently — missing or malformed VCAP_SERVICES falls back to application.yml
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
