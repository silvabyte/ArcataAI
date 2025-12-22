-- Migration 9: Update jobs table schema for job stream filtering
-- Created: 2024-12-22
-- Description: Adds structured salary fields, is_remote flag, and raw_attributes JSONB.
--              Removes deprecated columns (salary_range, contact fields, raw_html_object_id, status).

-- ============================================================================
-- ADD NEW COLUMNS
-- ============================================================================

-- Remote work filter flag
ALTER TABLE jobs ADD COLUMN is_remote BOOLEAN DEFAULT false;
COMMENT ON COLUMN jobs.is_remote IS 'Whether job allows remote work, used for URL-based filtering';

-- Structured salary fields (replacing salary_range text)
ALTER TABLE jobs ADD COLUMN salary_min INTEGER;
COMMENT ON COLUMN jobs.salary_min IS 'Minimum salary amount in currency units';

ALTER TABLE jobs ADD COLUMN salary_max INTEGER;
COMMENT ON COLUMN jobs.salary_max IS 'Maximum salary amount in currency units';

ALTER TABLE jobs ADD COLUMN salary_currency TEXT DEFAULT 'USD';
COMMENT ON COLUMN jobs.salary_currency IS 'ISO 4217 currency code for salary (default USD)';

-- Raw attributes for preserving imported source data
ALTER TABLE jobs ADD COLUMN raw_attributes JSONB;
COMMENT ON COLUMN jobs.raw_attributes IS 'Flattened raw source data preserved for future normalization';

-- ============================================================================
-- REMOVE DEPRECATED COLUMNS
-- ============================================================================

-- salary_range replaced by structured salary_min/salary_max fields
ALTER TABLE jobs DROP COLUMN IF EXISTS salary_range;

-- Contact fields are redundant (application_url is sufficient)
ALTER TABLE jobs DROP COLUMN IF EXISTS application_email;
ALTER TABLE jobs DROP COLUMN IF EXISTS contact_email;
ALTER TABLE jobs DROP COLUMN IF EXISTS contact_phone;

-- raw_html_object_id not used
ALTER TABLE jobs DROP COLUMN IF EXISTS raw_html_object_id;

-- status not used at job level
ALTER TABLE jobs DROP COLUMN IF EXISTS status;

-- ============================================================================
-- ADD INDEXES
-- ============================================================================

-- Index for remote work filtering (commonly used in URL-based filters)
CREATE INDEX jobs_is_remote_idx ON jobs(is_remote);

-- Index for salary range queries
CREATE INDEX jobs_salary_range_idx ON jobs(salary_min, salary_max) WHERE salary_min IS NOT NULL OR salary_max IS NOT NULL;

-- GIN index for raw_attributes JSONB queries
CREATE INDEX jobs_raw_attributes_idx ON jobs USING GIN (raw_attributes) WHERE raw_attributes IS NOT NULL;
