-- Migration: Shared trigger functions
-- Description: Creates reusable trigger functions used across multiple tables
-- Dependencies: None (first migration)

-- ============================================================================
-- FUNCTION: update_updated_at_column
-- ============================================================================
-- Reusable trigger function to automatically update the updated_at timestamp
-- whenever a row is modified. Used by all tables with updated_at columns.

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

COMMENT ON FUNCTION update_updated_at_column() IS 'Reusable trigger function to update updated_at timestamp on row modification';

-- ============================================================================
-- FUNCTION: handle_new_user
-- ============================================================================
-- Automatically creates a profile when a new user signs up via Supabase Auth.
-- Runs with SECURITY DEFINER to bypass RLS policies during profile creation.

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

COMMENT ON FUNCTION handle_new_user() IS 'Auto-creates profile for new users on signup (SECURITY DEFINER)';

-- ============================================================================
-- FUNCTION: seed_default_statuses
-- ============================================================================
-- Automatically creates default kanban status columns when a new profile is created.
-- Runs with SECURITY DEFINER to bypass RLS policies during seeding.

CREATE OR REPLACE FUNCTION seed_default_statuses()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.application_statuses (profile_id, name, color, column_order, is_default)
  VALUES
    (NEW.id, 'Saved', '#6B7280', 0, TRUE),
    (NEW.id, 'Applied', '#3B82F6', 1, FALSE),
    (NEW.id, 'Phone Screen', '#8B5CF6', 2, FALSE),
    (NEW.id, 'Interview', '#F59E0B', 3, FALSE),
    (NEW.id, 'Offer', '#10B981', 4, FALSE),
    (NEW.id, 'Rejected', '#EF4444', 5, FALSE),
    (NEW.id, 'Withdrawn', '#9CA3AF', 6, FALSE);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION seed_default_statuses() IS 'Auto-seeds default kanban status columns for new profiles (SECURITY DEFINER)';
