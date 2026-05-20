package com.quizzar.generation.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        private String finishReason;
        private Integer index;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptFeedback {
        private String blockReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;
    }

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) return null;
        GeminiResponse.Content content = candidates.get(0).getContent();
        if (content == null || content.getParts() == null || content.getParts().isEmpty()) return null;
        return content.getParts().get(0).getText();
    }

    public boolean isBlocked() {
        return promptFeedback != null && promptFeedback.getBlockReason() != null;
    }

    public boolean isIncomplete() {
        if (candidates == null || candidates.isEmpty()) return true;
        String reason = candidates.get(0).getFinishReason();
        return !"STOP".equalsIgnoreCase(reason);
    }
}
