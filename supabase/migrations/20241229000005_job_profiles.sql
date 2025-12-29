-- Migration: Create job_profiles table with status enum
-- Description: User application profiles with versioned resume/cover letter data
-- Dependencies: 20241229000002_profiles.sql

-- ============================================================================
-- ENUM: job_profile_status
-- ============================================================================
-- Status for job profiles: draft (work in progress) or live (available for use)

CREATE TYPE job_profile_status AS ENUM ('draft', 'live');

-- ============================================================================
-- TABLE: job_profiles
-- ============================================================================
-- Stores user application profiles (Frontend Dev, Backend Dev, etc.)
-- Uses versioned JSONB envelopes for resume and cover letter data.

CREATE TABLE job_profiles (
  job_profile_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  resume_data JSONB,
  cover_letter_data JSONB,
  status job_profile_status NOT NULL DEFAULT 'draft',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on job_profiles
-- ============================================================================

CREATE TRIGGER job_profiles_update_updated_at
  BEFORE UPDATE ON job_profiles
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE job_profiles ENABLE ROW LEVEL SECURITY;

-- Policy: Users can SELECT their own job profiles
CREATE POLICY job_profiles_select_own
  ON job_profiles
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can INSERT their own job profiles
CREATE POLICY job_profiles_insert_own
  ON job_profiles
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can UPDATE their own job profiles
CREATE POLICY job_profiles_update_own
  ON job_profiles
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can DELETE their own job profiles
CREATE POLICY job_profiles_delete_own
  ON job_profiles
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX job_profiles_profile_id_idx ON job_profiles(profile_id);
CREATE INDEX job_profiles_status_idx ON job_profiles(status);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE job_profiles IS 'User application profiles with versioned resume and cover letter data';
COMMENT ON COLUMN job_profiles.job_profile_id IS 'Primary key';
COMMENT ON COLUMN job_profiles.profile_id IS 'Foreign key to profiles.id';
COMMENT ON COLUMN job_profiles.name IS 'Profile name (e.g., "Frontend Developer", "Backend Engineer")';
COMMENT ON COLUMN job_profiles.resume_data IS 'Versioned JSONB envelope: {"version": 1, "data": {...}}';
COMMENT ON COLUMN job_profiles.cover_letter_data IS 'Versioned JSONB envelope: {"version": 1, "data": {...}}';
COMMENT ON COLUMN job_profiles.status IS 'Profile status: draft (work in progress) or live (available for use in applications)';
