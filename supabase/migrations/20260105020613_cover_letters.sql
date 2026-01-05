-- Migration: Create cover_letters table
-- Description: Cover letters associated with job profiles
-- Dependencies: 20241229000002_profiles.sql, 20241229000005_job_profiles.sql

-- ============================================================================
-- TABLE: cover_letters
-- ============================================================================
-- Stores cover letters for job profiles. Each cover letter belongs to a user
-- (via profile_id) and is optionally associated with a job profile.

CREATE TABLE cover_letters (
  cover_letter_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  job_profile_id INTEGER REFERENCES job_profiles(job_profile_id) ON DELETE SET NULL,
  name TEXT NOT NULL,
  content TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on cover_letters
-- ============================================================================

CREATE TRIGGER cover_letters_update_updated_at
  BEFORE UPDATE ON cover_letters
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE cover_letters ENABLE ROW LEVEL SECURITY;

-- Policy: Users can SELECT their own cover letters
CREATE POLICY cover_letters_select_own
  ON cover_letters
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can INSERT their own cover letters
CREATE POLICY cover_letters_insert_own
  ON cover_letters
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can UPDATE their own cover letters
CREATE POLICY cover_letters_update_own
  ON cover_letters
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can DELETE their own cover letters
CREATE POLICY cover_letters_delete_own
  ON cover_letters
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX cover_letters_profile_id_idx ON cover_letters(profile_id);
CREATE INDEX cover_letters_job_profile_id_idx ON cover_letters(job_profile_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE cover_letters IS 'Cover letters associated with job profiles';
COMMENT ON COLUMN cover_letters.cover_letter_id IS 'Primary key';
COMMENT ON COLUMN cover_letters.profile_id IS 'User ownership - foreign key to profiles.id (CASCADE delete)';
COMMENT ON COLUMN cover_letters.job_profile_id IS 'Associated job profile - foreign key to job_profiles.job_profile_id (SET NULL on delete, orphans the cover letter)';
COMMENT ON COLUMN cover_letters.name IS 'Cover letter name/title (e.g., "Software Engineer @ Google")';
COMMENT ON COLUMN cover_letters.content IS 'Cover letter content/body text';
