-- Migration: Create profiles table
-- Description: User profiles with automatic creation on signup via auth trigger
-- Dependencies: 20241229000001_functions.sql

-- ============================================================================
-- TABLE: profiles
-- ============================================================================
-- User profile information tied to auth.users via foreign key.
-- Each authenticated user gets a profile auto-created via trigger.

CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE NOT NULL,
  full_name TEXT,
  first_name TEXT,
  last_name TEXT,
  username TEXT UNIQUE,
  avatar_url TEXT,
  website TEXT,
  timezone TEXT DEFAULT 'UTC',
  created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on profiles
-- ============================================================================

CREATE TRIGGER update_profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TRIGGER: Create profile on user signup
-- ============================================================================

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION handle_new_user();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own profile
CREATE POLICY profiles_select_own
  ON profiles
  FOR SELECT
  TO authenticated
  USING (auth.uid() = id);

-- Policy: Users can update their own profile
CREATE POLICY profiles_update_own
  ON profiles
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

-- Note: No INSERT policy needed - profiles are created via trigger
-- Note: No DELETE policy needed - cascade from auth.users handles deletion

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_profiles_email ON profiles(email);
CREATE INDEX idx_profiles_username ON profiles(username);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE profiles IS 'User profile information linked to auth.users';
COMMENT ON COLUMN profiles.id IS 'Foreign key to auth.users.id';
COMMENT ON COLUMN profiles.email IS 'User email, auto-populated from auth.users';
COMMENT ON COLUMN profiles.username IS 'Unique username for public profile URL (arcata.ai/username)';
COMMENT ON COLUMN profiles.timezone IS 'User timezone preference (IANA timezone identifier)';
COMMENT ON COLUMN profiles.first_name IS 'User first name';
COMMENT ON COLUMN profiles.last_name IS 'User last name';
