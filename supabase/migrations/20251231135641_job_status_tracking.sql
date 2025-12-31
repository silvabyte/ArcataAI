-- Migration: Add job status tracking columns
-- Description: Support for cron-based job status checking workflow
-- Dependencies: 20241229000004_jobs.sql

-- ============================================================================
-- ALTER TABLE: jobs - Add status tracking columns
-- ============================================================================
-- These columns enable the JobStatusWorkflow to track whether job postings
-- are still active, and when they were last checked.

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'open';
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS closed_reason TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS last_status_check TIMESTAMPTZ;

-- ============================================================================
-- CONSTRAINT: Valid job status values
-- ============================================================================

ALTER TABLE jobs ADD CONSTRAINT jobs_status_check CHECK (
  status IS NULL OR status IN ('open', 'closed')
);

-- ============================================================================
-- INDEX: Efficient querying for jobs to check
-- ============================================================================
-- Supports the JobsToCheckFetcher step which queries:
-- WHERE status = 'open' AND (last_status_check IS NULL OR last_status_check < threshold)

CREATE INDEX IF NOT EXISTS idx_jobs_status_check 
ON jobs (status, last_status_check) 
WHERE status = 'open';

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON COLUMN jobs.status IS 'Job posting status: open (active), closed (no longer accepting applications)';
COMMENT ON COLUMN jobs.closed_reason IS 'Why job was marked closed (e.g., HTTP 404, page contains closure signal)';
COMMENT ON COLUMN jobs.closed_at IS 'Timestamp when job was marked as closed';
COMMENT ON COLUMN jobs.last_status_check IS 'Timestamp when job status was last verified by the status checker workflow';
