-- Migration: Add company_jobs_source column
-- Description: Identifies which ATS/job board platform a company uses
-- Dependencies: 20241229000003_companies.sql

-- ============================================================================
-- COLUMN: company_jobs_source
-- ============================================================================
-- Stores the ATS platform type for job discovery workflows.
-- Used by JobDiscoveryWorkflow to filter companies by source type.

ALTER TABLE companies
ADD COLUMN company_jobs_source TEXT;

-- ============================================================================
-- CONSTRAINT: Valid ATS source values
-- ============================================================================

ALTER TABLE companies
ADD CONSTRAINT companies_jobs_source_check
CHECK (company_jobs_source IS NULL OR company_jobs_source IN (
  'greenhouse',
  'lever',
  'ashby',
  'workday',
  'icims',
  'workable',
  'custom'
));

-- ============================================================================
-- INDEX: Efficient filtering by source
-- ============================================================================
-- Partial index only includes rows with non-null source for efficient queries

CREATE INDEX companies_jobs_source_idx ON companies(company_jobs_source)
WHERE company_jobs_source IS NOT NULL;

-- ============================================================================
-- COMMENT
-- ============================================================================

COMMENT ON COLUMN companies.company_jobs_source IS 
  'ATS platform type: greenhouse, lever, ashby, workday, icims, workable, custom. Used by job discovery workflows to filter companies.';
