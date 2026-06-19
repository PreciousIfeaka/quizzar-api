ALTER TABLE teachers DROP COLUMN keycloak_subject;
DROP INDEX IF EXISTS idx_teachers_keycloak_subject;

ALTER TABLE teachers ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE teachers ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE otp_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_verifications_email_code ON otp_verifications(email, otp_code);
