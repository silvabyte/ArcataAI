-- Migration: Support orphaned jobs
-- Description: Jobs can now exist without a company when AI cannot extract company website
-- This enables "soft failure" in the job ingestion pipeline - jobs are still ingested
-- and trackable even when company resolution fails.

-- ============================================================================
-- STEP 1: Make company_id nullable
-- ============================================================================

ALTER TABLE jobs 
  ALTER COLUMN company_id DROP NOT NULL;

-- ============================================================================
-- STEP 2: Update foreign key to SET NULL on delete
-- ============================================================================
-- Previously CASCADE - deleting a company would delete all its jobs
-- Now SET NULL - jobs are preserved as orphaned if company is deleted

ALTER TABLE jobs 
  DROP CONSTRAINT jobs_company_id_fkey,
  ADD CONSTRAINT jobs_company_id_fkey 
    FOREIGN KEY (company_id) 
    REFERENCES companies(company_id) 
    ON DELETE SET NULL;

-- ============================================================================
-- STEP 3: Update column comment
-- ============================================================================

COMMENT ON COLUMN jobs.company_id IS 'Foreign key to companies table. NULL when company could not be resolved from job posting (orphaned job).';
