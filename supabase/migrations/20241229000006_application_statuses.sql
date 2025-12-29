-- Migration: Create application_statuses table
-- Description: Customizable kanban columns for job application tracking
-- Dependencies: 20241229000002_profiles.sql, 20241229000001_functions.sql (seed_default_statuses)

-- ============================================================================
-- TABLE: application_statuses
-- ============================================================================
-- Customizable status columns for the kanban board.
-- Each user gets their own set of statuses with default values seeded on signup.

CREATE TABLE application_statuses (
  status_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  color TEXT,
  column_order INTEGER NOT NULL,
  is_default BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(profile_id, name)
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on application_statuses
-- ============================================================================

CREATE TRIGGER update_application_statuses_updated_at
  BEFORE UPDATE ON application_statuses
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TRIGGER: Seed statuses on profile creation
-- ============================================================================
-- Uses seed_default_statuses() function defined in 20241229000001_functions.sql

CREATE TRIGGER on_profile_created_seed_statuses
  AFTER INSERT ON profiles
  FOR EACH ROW
  EXECUTE FUNCTION seed_default_statuses();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE application_statuses ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own statuses
CREATE POLICY application_statuses_select_own
  ON application_statuses
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can insert their own statuses
CREATE POLICY application_statuses_insert_own
  ON application_statuses
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can update their own statuses
CREATE POLICY application_statuses_update_own
  ON application_statuses
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can delete their own statuses
CREATE POLICY application_statuses_delete_own
  ON application_statuses
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX application_statuses_profile_id_idx ON application_statuses(profile_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE application_statuses IS 'Customizable kanban column statuses for job applications';
COMMENT ON COLUMN application_statuses.status_id IS 'Primary key, auto-incremented';
COMMENT ON COLUMN application_statuses.profile_id IS 'Foreign key to profiles.id';
COMMENT ON COLUMN application_statuses.name IS 'Display name of the status (e.g., "Applied", "Interview")';
COMMENT ON COLUMN application_statuses.color IS 'Hex color code for UI display';
COMMENT ON COLUMN application_statuses.column_order IS 'Order of columns in the kanban board (0-based)';
COMMENT ON COLUMN application_statuses.is_default IS 'Whether this is the default status for new applications';
