-- Migration 11: Add application_id to job_stream table
-- Created: 2024-12-23
-- Description: Adds application_id FK to track which jobs from the stream have been added to applications
-- Dependencies: Migration 5 (job_applications), Migration 7 (job_stream)

-- ============================================================================
-- ADD APPLICATION_ID COLUMN
-- ============================================================================
-- Links job stream entries to their corresponding application when tracked
-- NULL means the job hasn't been tracked yet, non-NULL means it's being tracked

ALTER TABLE job_stream
ADD COLUMN application_id BIGINT REFERENCES job_applications(application_id) ON DELETE SET NULL;

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Index: application_id for fast lookups of tracked jobs
CREATE INDEX idx_job_stream_application_id ON job_stream(application_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON COLUMN job_stream.application_id IS 'Foreign key to job_applications.application_id - SET NULL on delete. NULL means not tracked, non-NULL means job is being tracked in applications';
