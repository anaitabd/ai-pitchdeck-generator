-- =====================================================
-- AI PitchDeck Generator - Database Schema
-- Version: 1.0
-- Description: Initial database schema for AI PitchDeck Generator
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- Table: users
-- Description: Stores user authentication and profile information
-- =====================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    company VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN', 'PREMIUM'))
);

-- Index for faster email lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);

-- =====================================================
-- Table: projects
-- Description: Stores user projects/campaigns
-- =====================================================
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    industry VARCHAR(100),
    target_audience VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED', 'COMPLETED'))
);

-- Indexes for projects
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

-- =====================================================
-- Table: file_uploads
-- Description: Stores metadata for uploaded files
-- =====================================================
CREATE TABLE file_uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    upload_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    content_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_file_uploads_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_uploads_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_file_type CHECK (file_type IN ('PDF', 'DOC', 'DOCX', 'TXT', 'MD'))
);

-- Indexes for file_uploads
CREATE INDEX idx_file_uploads_project_id ON file_uploads(project_id);
CREATE INDEX idx_file_uploads_user_id ON file_uploads(user_id);
CREATE INDEX idx_file_uploads_status ON file_uploads(upload_status);
CREATE INDEX idx_file_uploads_s3_key ON file_uploads(s3_key);

-- =====================================================
-- Table: generation_jobs
-- Description: Tracks AI pitch deck generation jobs
-- =====================================================
CREATE TABLE generation_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    ai_model VARCHAR(100) NOT NULL,
    prompt_template VARCHAR(100),
    input_file_ids UUID[] NOT NULL,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_generation_jobs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_job_status CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Indexes for generation_jobs
CREATE INDEX idx_generation_jobs_project_id ON generation_jobs(project_id);
CREATE INDEX idx_generation_jobs_user_id ON generation_jobs(user_id);
CREATE INDEX idx_generation_jobs_status ON generation_jobs(status);
CREATE INDEX idx_generation_jobs_created_at ON generation_jobs(created_at DESC);

-- =====================================================
-- Table: pitch_decks
-- Description: Stores generated pitch deck content and metadata
-- =====================================================
CREATE TABLE pitch_decks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    generation_job_id UUID,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    content JSONB NOT NULL,
    slide_count INTEGER NOT NULL,
    template_used VARCHAR(100),
    is_current_version BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pitch_decks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_decks_generation_job FOREIGN KEY (generation_job_id) REFERENCES generation_jobs(id) ON DELETE SET NULL,
    CONSTRAINT fk_pitch_decks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for pitch_decks
CREATE INDEX idx_pitch_decks_project_id ON pitch_decks(project_id);
CREATE INDEX idx_pitch_decks_user_id ON pitch_decks(user_id);
CREATE INDEX idx_pitch_decks_generation_job_id ON pitch_decks(generation_job_id);
CREATE INDEX idx_pitch_decks_version ON pitch_decks(project_id, version DESC);
CREATE INDEX idx_pitch_decks_current_version ON pitch_decks(project_id, is_current_version) WHERE is_current_version = true;
CREATE INDEX idx_pitch_decks_content ON pitch_decks USING GIN (content);

-- =====================================================
-- Table: audit_logs (Optional but recommended)
-- Description: Tracks important user actions for compliance and debugging
-- =====================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_method VARCHAR(10),
    request_path VARCHAR(500),
    status_code INTEGER,
    changes JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for audit_logs
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =====================================================
-- Table: refresh_tokens
-- Description: Stores JWT refresh tokens for authentication
-- =====================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for refresh_tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- =====================================================
-- Functions and Triggers
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_generation_jobs_updated_at BEFORE UPDATE ON generation_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pitch_decks_updated_at BEFORE UPDATE ON pitch_decks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Initial Data (Optional)
-- =====================================================

-- Create a default admin user (password: Admin123!)
-- Note: In production, this should be changed immediately
INSERT INTO users (email, password_hash, first_name, last_name, role, email_verified)
VALUES ('admin@pitchdeck.com', '$2a$10$8XVS7qH4vZ7Q0h9Z9Z9Z9.K9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z', 'Admin', 'User', 'ADMIN', true);

-- =====================================================
-- Comments
-- =====================================================

COMMENT ON TABLE users IS 'Stores user authentication and profile information';
COMMENT ON TABLE projects IS 'Stores user projects and campaigns';
COMMENT ON TABLE file_uploads IS 'Stores metadata for files uploaded by users';
COMMENT ON TABLE generation_jobs IS 'Tracks AI pitch deck generation jobs';
COMMENT ON TABLE pitch_decks IS 'Stores generated pitch deck content with version history';
COMMENT ON TABLE audit_logs IS 'Audit trail for important user actions';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';
