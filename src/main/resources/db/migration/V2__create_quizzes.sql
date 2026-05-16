CREATE TABLE quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    quiz_code VARCHAR(20) NOT NULL UNIQUE,
    timing_mode VARCHAR(20) NOT NULL DEFAULT 'NONE',
    timer_value_seconds INTEGER,
    ai_suggested_time_seconds INTEGER,
    ai_suggested_timing_mode VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
