package com.quizzar.generation.prompt;

import org.springframework.stereotype.Component;

@Component
public class FormattingPromptBuilder {
    public String build(String rawText) {
        return String.format("""
            The following is raw, unformatted quiz content pasted by a teacher.
            Parse and format all questions and answers into the standard JSON schema.
            You can generate the answers if they are not available.
            Infer question types from context (MCQ, TRUE_FALSE, SHORT_ANSWER).
            
            Raw Content:
            %s
            """, rawText);
    }
}
