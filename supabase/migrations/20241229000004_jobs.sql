-- Migration: Create jobs table
-- Description: Job listings with structured salary fields and extraction metadata
-- Dependencies: 20241229000003_companies.sql

-- ============================================================================
-- TABLE: jobs
-- ============================================================================
-- Job listings with references to companies, structured salary data,
-- and metadata about extraction quality.

CREATE TABLE jobs (
  job_id SERIAL PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT,
  location TEXT,
  is_remote BOOLEAN DEFAULT false,
  job_type TEXT,
  category TEXT,
  experience_level TEXT,
  education_level TEXT,
  -- Structured salary fields
  salary_min INTEGER,
  salary_max INTEGER,
  salary_currency TEXT DEFAULT 'USD',
  -- Job details
  benefits TEXT[],
  qualifications TEXT[],
  preferred_qualifications TEXT[],
  responsibilities TEXT[],
  application_url TEXT,
  source_url TEXT,
  posted_date TIMESTAMPTZ,
  closing_date TIMESTAMPTZ,
  -- Extraction metadata
  completion_state TEXT,
  raw_attributes JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  -- Constraints
  CONSTRAINT jobs_completion_state_check CHECK (
    completion_state IS NULL OR completion_state IN (
      'Complete', 'Sufficient', 'Partial', 'Minimal', 'Failed', 'Unknown'
    )
  )
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on jobs
-- ============================================================================

CREATE TRIGGER jobs_update_updated_at
  BEFORE UPDATE ON jobs
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

-- Policy: Authenticated users can SELECT jobs (read-only public data)
CREATE POLICY jobs_select_authenticated ON jobs
  FOR SELECT
  TO authenticated
  USING (true);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX jobs_company_id_idx ON jobs(company_id);
CREATE INDEX jobs_is_remote_idx ON jobs(is_remote);
CREATE INDEX jobs_salary_range_idx ON jobs(salary_min, salary_max) WHERE salary_min IS NOT NULL OR salary_max IS NOT NULL;
CREATE INDEX jobs_completion_state_idx ON jobs(completion_state);
CREATE INDEX jobs_raw_attributes_idx ON jobs USING GIN (raw_attributes) WHERE raw_attributes IS NOT NULL;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE jobs IS 'Job listings with structured salary and extraction metadata';
COMMENT ON COLUMN jobs.is_remote IS 'Whether job allows remote work, used for URL-based filtering';
COMMENT ON COLUMN jobs.salary_min IS 'Minimum salary amount in currency units';
COMMENT ON COLUMN jobs.salary_max IS 'Maximum salary amount in currency units';
COMMENT ON COLUMN jobs.salary_currency IS 'ISO 4217 currency code for salary (default USD)';
COMMENT ON COLUMN jobs.completion_state IS 'Extraction quality: Complete (100%), Sufficient (75%+), Partial (50%+), Minimal (25%+), Failed (<25%), Unknown';
COMMENT ON COLUMN jobs.raw_attributes IS 'Flattened raw source data preserved for future normalization';
