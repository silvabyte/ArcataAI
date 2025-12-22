-- Migration: Create job_applications table
-- Description: Core table for tracking job applications on the kanban board
-- Dependencies: Migrations 1-4 (profiles, companies_and_jobs, job_profiles, application_statuses)

-- ============================================================================
-- TABLE: job_applications
-- ============================================================================
-- The main table for tracking job applications through the kanban workflow
-- Links users, jobs, profiles, and status tracking together

CREATE TABLE job_applications (
  application_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  job_id INTEGER REFERENCES jobs(job_id) ON DELETE SET NULL,
  job_profile_id INTEGER REFERENCES job_profiles(job_profile_id) ON DELETE SET NULL,
  status_id INTEGER REFERENCES application_statuses(status_id) ON DELETE SET NULL,
  status_order INTEGER NOT NULL DEFAULT 0,
  application_date TIMESTAMPTZ,
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on job_applications
-- ============================================================================

CREATE TRIGGER update_job_applications_updated_at
  BEFORE UPDATE ON job_applications
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE job_applications ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own applications
CREATE POLICY job_applications_select_own
  ON job_applications
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can insert their own applications
CREATE POLICY job_applications_insert_own
  ON job_applications
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can update their own applications
CREATE POLICY job_applications_update_own
  ON job_applications
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can delete their own applications
CREATE POLICY job_applications_delete_own
  ON job_applications
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Profile lookup index for efficient filtering by user
CREATE INDEX job_applications_profile_id_idx ON job_applications(profile_id);

-- Status lookup index for kanban column queries
CREATE INDEX job_applications_status_id_idx ON job_applications(status_id);

-- Job lookup index for finding applications by job
CREATE INDEX job_applications_job_id_idx ON job_applications(job_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE job_applications IS 'Core table for tracking job applications through the kanban workflow';
COMMENT ON COLUMN job_applications.application_id IS 'Primary key, auto-incremented';
COMMENT ON COLUMN job_applications.profile_id IS 'Foreign key to profiles.id (CASCADE delete)';
COMMENT ON COLUMN job_applications.job_id IS 'Foreign key to jobs.job_id (SET NULL on delete, preserves application)';
COMMENT ON COLUMN job_applications.job_profile_id IS 'Foreign key to job_profiles.job_profile_id (SET NULL on delete)';
COMMENT ON COLUMN job_applications.status_id IS 'Foreign key to application_statuses.status_id (SET NULL on delete)';
COMMENT ON COLUMN job_applications.status_order IS 'Position within the kanban column (use gaps: 0, 1000, 2000 for easy reordering)';
COMMENT ON COLUMN job_applications.application_date IS 'Date when the application was submitted';
COMMENT ON COLUMN job_applications.notes IS 'User notes about the application';
