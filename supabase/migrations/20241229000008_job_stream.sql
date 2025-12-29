-- Migration: Create job_stream table
-- Description: Discovery feed for jobs that have surfaced but aren't being tracked yet
-- Dependencies: 20241229000002_profiles.sql, 20241229000004_jobs.sql, 
--               20241229000005_job_profiles.sql, 20241229000007_job_applications.sql

-- ============================================================================
-- TABLE: job_stream
-- ============================================================================
-- Stores jobs that have surfaced in the discovery feed but aren't being tracked yet.
-- Tracks how jobs entered the stream and which job profiles they match.

CREATE TABLE job_stream (
  stream_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  job_id INTEGER NOT NULL REFERENCES jobs(job_id) ON DELETE CASCADE,
  source TEXT NOT NULL,
  best_match_job_profile_id INTEGER REFERENCES job_profiles(job_profile_id) ON DELETE SET NULL,
  best_match_score FLOAT,
  profile_matches JSONB,
  status TEXT DEFAULT 'new',
  application_id BIGINT REFERENCES job_applications(application_id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(profile_id, job_id)
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on job_stream
-- ============================================================================

CREATE TRIGGER job_stream_update_updated_at
  BEFORE UPDATE ON job_stream
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE job_stream ENABLE ROW LEVEL SECURITY;

-- Policy: Users can SELECT their own job stream entries
CREATE POLICY job_stream_select_own
  ON job_stream
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can INSERT their own job stream entries
CREATE POLICY job_stream_insert_own
  ON job_stream
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can UPDATE their own job stream entries
CREATE POLICY job_stream_update_own
  ON job_stream
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can DELETE their own job stream entries
CREATE POLICY job_stream_delete_own
  ON job_stream
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX job_stream_profile_id_idx ON job_stream(profile_id);
CREATE INDEX job_stream_job_id_idx ON job_stream(job_id);
CREATE INDEX job_stream_status_idx ON job_stream(status);
CREATE INDEX idx_job_stream_application_id ON job_stream(application_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE job_stream IS 'Discovery feed - jobs that have surfaced but are not being tracked yet';
COMMENT ON COLUMN job_stream.stream_id IS 'Primary key';
COMMENT ON COLUMN job_stream.profile_id IS 'Foreign key to profiles.id - CASCADE delete';
COMMENT ON COLUMN job_stream.job_id IS 'Foreign key to jobs.job_id - CASCADE delete';
COMMENT ON COLUMN job_stream.source IS 'How the job entered the stream: extension, ai_discovery, import, manual';
COMMENT ON COLUMN job_stream.best_match_job_profile_id IS 'Foreign key to job_profiles.job_profile_id - SET NULL on delete';
COMMENT ON COLUMN job_stream.best_match_score IS 'Confidence score for the best matching job profile (0.0-1.0)';
COMMENT ON COLUMN job_stream.profile_matches IS 'Array of all matching profiles: [{"job_profile_id": 1, "score": 0.95, "name": "Frontend Developer"}, ...]';
COMMENT ON COLUMN job_stream.status IS 'User interaction state: new, viewed, dismissed';
COMMENT ON COLUMN job_stream.application_id IS 'Foreign key to job_applications.application_id - SET NULL on delete. NULL means not tracked, non-NULL means job is being tracked in applications';
