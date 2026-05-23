package com.quizzar.generation.client;

import com.quizzar.common.exception.AiGenerationException;
import com.quizzar.generation.config.GeminiProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GeminiClientTest {

    private MockWebServer mockWebServer;
    private GeminiClient geminiClient;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        GeminiProperties props = new GeminiProperties();
        props.setApiKey("test-key");
        props.setModel("gemini-1.5-pro");
        props.setBaseUrl(mockWebServer.url("/").toString());
        props.getTimeout().setConnectSeconds(5);
        props.getTimeout().setReadSeconds(10);

        WebClient webClient = WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .build();

        geminiClient = new GeminiClient(webClient, props);
    }

    @AfterEach
    void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void generate_successResponse_returnsText() throws InterruptedException {
        String mockBody = """
            {
              "candidates": [{
                "content": { "parts": [{"text": "{\\"questions\\":[]}"}], "role": "model" },
                "finishReason": "STOP",
                "index": 0
              }]
            }
            """;
        mockWebServer.enqueue(new MockResponse()
            .setBody(mockBody)
            .addHeader("Content-Type", "application/json"));

        String result = geminiClient.generate("system", "user", 0.3, 1000);
        assertThat(result).contains("questions");
    }

    @Test
    void generate_429Response_throwsAiGenerationException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));

        assertThatThrownBy(() -> geminiClient.generate("system", "user", 0.3, 1000))
            .isInstanceOf(AiGenerationException.class)
            .hasMessageContaining("rate limit");
    }

    @Test
    void generate_safetyBlock_throwsAiGenerationException() {
        String blockedBody = """
            {
              "promptFeedback": { "blockReason": "SAFETY" },
              "candidates": []
            }
            """;
        mockWebServer.enqueue(new MockResponse()
            .setBody(blockedBody)
            .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> geminiClient.generate("system", "user", 0.3, 1000))
            .isInstanceOf(AiGenerationException.class)
            .hasMessageContaining("safety");
    }
}
