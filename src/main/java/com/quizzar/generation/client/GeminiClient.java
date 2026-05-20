package com.quizzar.generation.client;

import com.quizzar.common.exception.AiGenerationException;
import com.quizzar.generation.client.dto.GeminiResponse;
import com.quizzar.generation.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Qualifier("geminiWebClient")
    private final WebClient webClient;

    private final GeminiProperties geminiProperties;

    /**
     * Calls the Gemini generateContent endpoint.
     *
     * @param systemPrompt  The system-level instruction (role, output format rules)
     * @param userPrompt    The user-level content (extracted text, raw paste, specs)
     * @param temperature   Sampling temperature (lower = more deterministic)
     * @param maxTokens     Maximum output tokens
     * @return              Raw text from the first candidate's first part
     * @throws AiGenerationException on any API error, safety block, or incomplete response
     */
    public String generate(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        Map<String, Object> request = buildRequest(systemPrompt, userPrompt, temperature, maxTokens);
        String endpoint = buildEndpoint();

        log.info("Calling Gemini generateContent for model: {}", geminiProperties.getModel());

        try {
            GeminiResponse response = webClient.post()
                .uri(endpoint)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals, clientResponse ->
                    clientResponse.bodyToMono(String.class).map(body ->
                        new AiGenerationException("Gemini rate limit exceeded. Please try again shortly.")))
                .onStatus(HttpStatus.BAD_REQUEST::equals, clientResponse ->
                    clientResponse.bodyToMono(String.class).map(body ->
                        new AiGenerationException("Gemini rejected the request (400): " + sanitize(body))))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                    clientResponse.bodyToMono(String.class).map(body ->
                        new AiGenerationException("Gemini server error (" + clientResponse.statusCode().value() + "): " + sanitize(body))))
                .bodyToMono(GeminiResponse.class)
                .block(); // Blocking is intentional — our service layer is synchronous MVC

            return extractAndValidate(response);

        } catch (AiGenerationException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("Gemini HTTP error {}: {}", e.getStatusCode(), sanitize(e.getResponseBodyAsString()));
            throw new AiGenerationException("Gemini API error (" + e.getStatusCode() + "): " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini: {}", e.getMessage());
            throw new AiGenerationException("Unexpected error communicating with Gemini: " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequest(String systemPrompt, String userPrompt,
                                             double temperature, int maxTokens) {
        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxTokens,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String buildEndpoint() {
        return String.format("/v1beta/models/%s:generateContent?key=%s",
            geminiProperties.getModel(),
            geminiProperties.getApiKey());
    }

    private String extractAndValidate(GeminiResponse response) {
        if (response == null) {
            throw new AiGenerationException("Gemini returned an empty response.");
        }
        if (response.isBlocked()) {
            throw new AiGenerationException(
                "Gemini blocked the prompt for safety reasons. Revise the input and try again.");
        }
        if (response.isIncomplete()) {
            String reason = response.getCandidates() != null && !response.getCandidates().isEmpty()
                ? response.getCandidates().get(0).getFinishReason() : "UNKNOWN";
            log.warn("Gemini response incomplete. FinishReason: {}", reason);

            throw new AiGenerationException(
                "Gemini did not complete the response (reason: " + reason + "). " +
                "Try reducing the number of questions or simplifying the request.");
        }

        String text = response.extractText();
        if (text == null || text.isBlank()) {
            throw new AiGenerationException("Gemini returned a response with no text content.");
        }
        return text;
    }

    private String sanitize(String body) {
        if (body == null) return "null";
        return body.length() > 300 ? body.substring(0, 300) + "...[truncated]" : body;
    }
}
