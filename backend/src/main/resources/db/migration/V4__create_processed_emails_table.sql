-- Flyway Migration V4: Create processed_emails table for idempotent email processing

CREATE TABLE IF NOT EXISTS processed_emails (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    sender VARCHAR(255),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_processed_emails_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_processed_emails_user_message UNIQUE (user_id, message_id)
);

CREATE INDEX IF NOT EXISTS idx_processed_emails_user_message ON processed_emails(user_id, message_id);
