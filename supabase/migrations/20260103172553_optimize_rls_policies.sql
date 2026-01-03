-- Migration: Optimize RLS policies with scalar subqueries
-- Description: Wrap auth.uid() calls in SELECT subqueries for better query performance
-- 
-- WHY THIS MATTERS:
-- Direct use of auth.uid() in RLS policy expressions is evaluated per-row during scans,
-- updates, and deletes. This causes poor performance at scale because PostgreSQL cannot
-- treat the value as a stable constant.
--
-- By wrapping auth.uid() in a scalar subquery like (SELECT auth.uid()), PostgreSQL
-- evaluates the function once per statement, enabling better query planning and
-- indexing effectiveness.
--
-- Reference: https://supabase.com/docs/guides/database/postgres/row-level-security#call-functions-with-select
--
-- SAFETY: This migration runs in a transaction to ensure atomic policy replacement.
-- There is no window where policies are missing - all changes are applied together.

BEGIN;

-- ============================================================================
-- TABLE: public.profiles
-- ============================================================================

DROP POLICY IF EXISTS profiles_select_own ON public.profiles;
CREATE POLICY profiles_select_own
  ON public.profiles
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = id);

DROP POLICY IF EXISTS profiles_update_own ON public.profiles;
CREATE POLICY profiles_update_own
  ON public.profiles
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = id)
  WITH CHECK ((SELECT auth.uid()) = id);

-- ============================================================================
-- TABLE: public.job_profiles
-- ============================================================================

DROP POLICY IF EXISTS job_profiles_select_own ON public.job_profiles;
CREATE POLICY job_profiles_select_own
  ON public.job_profiles
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_profiles_insert_own ON public.job_profiles;
CREATE POLICY job_profiles_insert_own
  ON public.job_profiles
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_profiles_update_own ON public.job_profiles;
CREATE POLICY job_profiles_update_own
  ON public.job_profiles
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id)
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_profiles_delete_own ON public.job_profiles;
CREATE POLICY job_profiles_delete_own
  ON public.job_profiles
  FOR DELETE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

-- ============================================================================
-- TABLE: public.application_statuses
-- ============================================================================

DROP POLICY IF EXISTS application_statuses_select_own ON public.application_statuses;
CREATE POLICY application_statuses_select_own
  ON public.application_statuses
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS application_statuses_insert_own ON public.application_statuses;
CREATE POLICY application_statuses_insert_own
  ON public.application_statuses
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS application_statuses_update_own ON public.application_statuses;
CREATE POLICY application_statuses_update_own
  ON public.application_statuses
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id)
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS application_statuses_delete_own ON public.application_statuses;
CREATE POLICY application_statuses_delete_own
  ON public.application_statuses
  FOR DELETE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

-- ============================================================================
-- TABLE: public.job_applications
-- ============================================================================

DROP POLICY IF EXISTS job_applications_select_own ON public.job_applications;
CREATE POLICY job_applications_select_own
  ON public.job_applications
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_applications_insert_own ON public.job_applications;
CREATE POLICY job_applications_insert_own
  ON public.job_applications
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_applications_update_own ON public.job_applications;
CREATE POLICY job_applications_update_own
  ON public.job_applications
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id)
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_applications_delete_own ON public.job_applications;
CREATE POLICY job_applications_delete_own
  ON public.job_applications
  FOR DELETE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

-- ============================================================================
-- TABLE: public.job_stream
-- ============================================================================

DROP POLICY IF EXISTS job_stream_select_own ON public.job_stream;
CREATE POLICY job_stream_select_own
  ON public.job_stream
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_stream_insert_own ON public.job_stream;
CREATE POLICY job_stream_insert_own
  ON public.job_stream
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_stream_update_own ON public.job_stream;
CREATE POLICY job_stream_update_own
  ON public.job_stream
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id)
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS job_stream_delete_own ON public.job_stream;
CREATE POLICY job_stream_delete_own
  ON public.job_stream
  FOR DELETE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

-- ============================================================================
-- TABLE: public.conversations
-- ============================================================================

DROP POLICY IF EXISTS conversations_select_own ON public.conversations;
CREATE POLICY conversations_select_own
  ON public.conversations
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS conversations_insert_own ON public.conversations;
CREATE POLICY conversations_insert_own
  ON public.conversations
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS conversations_update_own ON public.conversations;
CREATE POLICY conversations_update_own
  ON public.conversations
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id)
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS conversations_delete_own ON public.conversations;
CREATE POLICY conversations_delete_own
  ON public.conversations
  FOR DELETE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

-- ============================================================================
-- TABLE: public.application_answers
-- ============================================================================

DROP POLICY IF EXISTS application_answers_select_own ON public.application_answers;
CREATE POLICY application_answers_select_own
  ON public.application_answers
  FOR SELECT
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS application_answers_insert_own ON public.application_answers;
CREATE POLICY application_answers_insert_own
  ON public.application_answers
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS application_answers_update_own ON public.application_answers;
CREATE POLICY application_answers_update_own
  ON public.application_answers
  FOR UPDATE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id)
  WITH CHECK ((SELECT auth.uid()) = profile_id);

DROP POLICY IF EXISTS application_answers_delete_own ON public.application_answers;
CREATE POLICY application_answers_delete_own
  ON public.application_answers
  FOR DELETE
  TO authenticated
  USING ((SELECT auth.uid()) = profile_id);

COMMIT;
