-- Flyway Migration V2: Application Tracker Domain Schema

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    google_subject_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_google_subject_id UNIQUE (google_subject_id)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_subject_id ON users(google_subject_id);

-- 2. Applications table
-- Status enum values: APPLIED, ASSESSMENT, INTERVIEW, REJECTED, OFFER
CREATE TABLE IF NOT EXISTS applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    current_status VARCHAR(50) NOT NULL,
    source_email_id VARCHAR(255),
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_applications_user_source_email UNIQUE (user_id, source_email_id)
);

CREATE INDEX IF NOT EXISTS idx_applications_user_id ON applications(user_id);
CREATE INDEX IF NOT EXISTS idx_applications_current_status ON applications(current_status);
CREATE INDEX IF NOT EXISTS idx_applications_source_email_id ON applications(source_email_id);

-- 3. Status History table
CREATE TABLE IF NOT EXISTS status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_email_id VARCHAR(255),
    CONSTRAINT fk_status_history_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT uq_status_history_application_source_email UNIQUE (application_id, source_email_id)
);

CREATE INDEX IF NOT EXISTS idx_status_history_application_id ON status_history(application_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON status_history(changed_at);

-- 4. Sync State table
CREATE TABLE IF NOT EXISTS sync_states (
    user_id UUID PRIMARY KEY,
    last_history_id VARCHAR(255),
    last_synced_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_sync_states_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
