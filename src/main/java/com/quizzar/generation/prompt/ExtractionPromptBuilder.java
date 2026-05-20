package com.quizzar.generation.prompt;

import org.springframework.stereotype.Component;

@Component
public class ExtractionPromptBuilder {
    public String build(String extractedText) {
        return String.format("""
            The following is text extracted from a document containing quiz questions and answers.
            Extract all questions and answers and format them according to the JSON schema.
            You can generate the answers if they are not available.
            Determine the most appropriate question type for each (MCQ, TRUE_FALSE, or SHORT_ANSWER).
            
            Document Text:
            %s
            """, extractedText);
    }
}
