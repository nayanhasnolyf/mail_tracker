-- Application Tracker initial migration schema
-- Tables for applications, email logs, and OAuth tokens will be added in upcoming features.

CREATE TABLE IF NOT EXISTS schema_initialization_check (
    id SERIAL PRIMARY KEY,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(255) DEFAULT 'Application Tracker database schema initialized'
);
