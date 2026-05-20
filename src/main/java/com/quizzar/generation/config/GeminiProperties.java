package com.quizzar.generation.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "quizzar.gemini")
@Slf4j
public class GeminiProperties {
    private String apiKey;
    private String model = "";
    private String baseUrl = "";
    
    private Generation generation = new Generation();
    private Grading grading = new Grading();
    private Timeout timeout = new Timeout();

    @Data
    public static class Generation {
        private double temperature = 0.3;
        private int maxOutputTokens = 20000;
    }

    @Data
    public static class Grading {
        private double temperature = 0.1;
        private int maxOutputTokens = 1000;
    }

    @Data
    public static class Timeout {
        private int connectSeconds = 10;
        private int readSeconds = 30;
    }

    public double getGenerationTemperature() { return generation.getTemperature(); }
    public int getMaxOutputTokens() { return generation.getMaxOutputTokens(); }
    public double getGradingTemperature() { return grading.getTemperature(); }
    public int getGradingMaxOutputTokens() { return grading.getMaxOutputTokens(); }
}
