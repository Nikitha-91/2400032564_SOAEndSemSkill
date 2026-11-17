package com.example.driver.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Creates the RestTemplate the Driver Service uses to call the microservices.
 * Timeouts make sure a dead or slow service does not block the driver forever.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${driver.http.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${driver.http.read-timeout-ms}") long readTimeoutMs) {

        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
