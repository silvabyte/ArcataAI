-- Migration: Add username and timezone fields to profiles
-- Description: Adds username (unique) and timezone columns for account settings

-- ============================================================================
-- ADD COLUMNS
-- ============================================================================

ALTER TABLE profiles
  ADD COLUMN IF NOT EXISTS username TEXT UNIQUE,
  ADD COLUMN IF NOT EXISTS timezone TEXT DEFAULT 'UTC',
  ADD COLUMN IF NOT EXISTS first_name TEXT,
  ADD COLUMN IF NOT EXISTS last_name TEXT;

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Username lookup index (already unique, but explicit index for performance)
CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON COLUMN profiles.username IS 'Unique username for public profile URL (arcata.ai/username)';
COMMENT ON COLUMN profiles.timezone IS 'User timezone preference (IANA timezone identifier)';
COMMENT ON COLUMN profiles.first_name IS 'User first name';
COMMENT ON COLUMN profiles.last_name IS 'User last name';
