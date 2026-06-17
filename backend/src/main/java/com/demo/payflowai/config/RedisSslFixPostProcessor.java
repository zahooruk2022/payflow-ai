package com.demo.payflowai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Compatibility shim for java-cfenv-boot 3.1.x + Spring Boot 3.3+.
 *
 * cfenv sets spring.data.redis.ssl=true (a plain String). Spring Boot 3.3
 * changed RedisProperties.ssl from a boolean to a nested Ssl record, so the
 * String-to-object coercion fails at startup with ConverterNotFoundException.
 *
 * This processor runs last (LOWEST_PRECEDENCE), detects the scalar property,
 * and inserts spring.data.redis.ssl.enabled=<value> at the highest priority so
 * Spring Boot's ValueObjectBinder uses the structured path instead.
 */
public class RedisSslFixPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String sslRaw = environment.getProperty("spring.data.redis.ssl");
        if ("true".equalsIgnoreCase(sslRaw) || "false".equalsIgnoreCase(sslRaw)) {
            environment.getPropertySources().addFirst(
                new MapPropertySource("redisSslFix", Map.of("spring.data.redis.ssl.enabled", sslRaw))
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
