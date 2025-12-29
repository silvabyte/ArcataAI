-- Migration: Create application_answers table
-- Description: Historical Q&A data from applications for auto-fill feature
-- Dependencies: 20241229000002_profiles.sql, 20241229000005_job_profiles.sql, 
--               20241229000007_job_applications.sql

-- ============================================================================
-- TABLE: application_answers
-- ============================================================================
-- Stores user's answers to application questions for reuse and auto-fill.
-- Tracks question similarity via hashing and usage statistics.

CREATE TABLE application_answers (
  answer_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  job_profile_id INTEGER REFERENCES job_profiles(job_profile_id) ON DELETE SET NULL,
  application_id INTEGER REFERENCES job_applications(application_id) ON DELETE SET NULL,
  question TEXT NOT NULL,
  question_hash TEXT,
  answer TEXT NOT NULL,
  source TEXT,
  times_used INTEGER DEFAULT 1,
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on application_answers
-- ============================================================================

CREATE TRIGGER update_application_answers_updated_at
  BEFORE UPDATE ON application_answers
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE application_answers ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own answers
CREATE POLICY application_answers_select_own
  ON application_answers
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can insert their own answers
CREATE POLICY application_answers_insert_own
  ON application_answers
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can update their own answers
CREATE POLICY application_answers_update_own
  ON application_answers
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can delete their own answers
CREATE POLICY application_answers_delete_own
  ON application_answers
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX application_answers_profile_id_idx ON application_answers(profile_id);
CREATE INDEX application_answers_question_hash_idx ON application_answers(question_hash);
CREATE INDEX application_answers_job_profile_id_idx ON application_answers(job_profile_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE application_answers IS 'Historical Q&A data from applications for auto-fill feature';
COMMENT ON COLUMN application_answers.answer_id IS 'Primary key, auto-incremented';
COMMENT ON COLUMN application_answers.profile_id IS 'Foreign key to profiles.id (CASCADE delete)';
COMMENT ON COLUMN application_answers.job_profile_id IS 'Foreign key to job_profiles.job_profile_id (SET NULL on delete)';
COMMENT ON COLUMN application_answers.application_id IS 'Foreign key to job_applications.application_id (SET NULL on delete)';
COMMENT ON COLUMN application_answers.question IS 'The original question text';
COMMENT ON COLUMN application_answers.question_hash IS 'SHA-256 hash of normalized question for similarity matching';
COMMENT ON COLUMN application_answers.answer IS 'User''s answer to the question';
COMMENT ON COLUMN application_answers.source IS 'Answer source: ''manual'' (user typed) or ''ai_generated'' (AI generated and approved)';
COMMENT ON COLUMN application_answers.times_used IS 'Counter incremented each time answer is reused';
COMMENT ON COLUMN application_answers.last_used_at IS 'Timestamp of last usage for tracking popular answers';
