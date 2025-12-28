-- Migration: Add status enum to job_profiles
-- Created: 2024-12-27
-- Description: Replaces is_active boolean with status enum (draft/live) for job profiles
-- Dependencies: Migration 3 (job_profiles table)

-- ============================================================================
-- CREATE ENUM TYPE
-- ============================================================================

CREATE TYPE job_profile_status AS ENUM ('draft', 'live');

-- ============================================================================
-- ALTER JOB_PROFILES TABLE
-- ============================================================================

-- Add status column with default 'draft'
ALTER TABLE job_profiles 
  ADD COLUMN status job_profile_status NOT NULL DEFAULT 'draft';

-- Drop the is_active column (no data to migrate)
ALTER TABLE job_profiles 
  DROP COLUMN is_active;

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Index on status for filtering by draft/live profiles
CREATE INDEX job_profiles_status_idx ON job_profiles(status);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON COLUMN job_profiles.status IS 'Profile status: draft (work in progress) or live (available for use in applications)';
