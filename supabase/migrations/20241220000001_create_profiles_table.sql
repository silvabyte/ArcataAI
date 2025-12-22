-- Migration: Create profiles table with auth trigger
-- Description: Sets up user profiles with automatic creation on signup
-- Dependencies: None (first migration)

-- ============================================================================
-- REUSABLE TRIGGER FUNCTION: update_updated_at_column
-- ============================================================================
-- This function will be used by this and subsequent migrations to automatically
-- update the updated_at timestamp whenever a row is modified.

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
SECURITY INVOKER
SET search_path = ''
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TABLE: profiles
-- ============================================================================
-- User profile information tied to auth.users via foreign key
-- Each authenticated user gets a profile auto-created via trigger

CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE NOT NULL,
  full_name TEXT,
  avatar_url TEXT,
  website TEXT,
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
-- AUTH TRIGGER FUNCTION: handle_new_user
-- ============================================================================
-- Automatically creates a profile when a new user signs up
-- Runs with SECURITY DEFINER to bypass RLS policies

CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, email)
  VALUES (NEW.id, NEW.email);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- AUTH TRIGGER: Create profile on user signup
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

-- Email lookup index (already unique, but explicit index for performance)
CREATE INDEX idx_profiles_email ON profiles(email);

COMMENT ON TABLE profiles IS 'User profile information linked to auth.users';
COMMENT ON COLUMN profiles.id IS 'Foreign key to auth.users.id';
COMMENT ON COLUMN profiles.email IS 'User email, auto-populated from auth.users';
COMMENT ON FUNCTION handle_new_user() IS 'Auto-creates profile for new users (SECURITY DEFINER)';
COMMENT ON FUNCTION update_updated_at_column() IS 'Reusable trigger function to update updated_at timestamp';
