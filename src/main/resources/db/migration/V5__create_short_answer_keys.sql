CREATE TABLE short_answer_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    accepted_answer TEXT NOT NULL,
    is_case_sensitive BOOLEAN NOT NULL DEFAULT FALSE
);
