-- Migration: Create companies table
-- Description: Company information with enrichment fields for job listings
-- Dependencies: 20241229000001_functions.sql

-- ============================================================================
-- TABLE: companies
-- ============================================================================
-- Stores company information from various sources with AI-enriched metadata.

CREATE TABLE companies (
  company_id SERIAL PRIMARY KEY,
  source_company_id BIGINT,  -- Original ID from source data imports
  cc_id TEXT,
  company_name TEXT,
  company_domain TEXT,
  company_linkedin_url TEXT,
  company_jobs_url TEXT,
  company_phone TEXT,
  company_address TEXT,
  company_address_2 TEXT,
  company_city TEXT,
  company_state TEXT,
  company_zip TEXT,
  employee_count_min INTEGER,
  employee_count_max INTEGER,
  revenue_min BIGINT,
  revenue_max BIGINT,
  company_naics TEXT,
  company_sic TEXT,
  primary_industry TEXT,
  job_board_status TEXT,
  -- AI-enriched fields
  industry VARCHAR,
  company_size VARCHAR,
  description TEXT,
  headquarters VARCHAR,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on companies
-- ============================================================================

CREATE TRIGGER companies_update_updated_at
  BEFORE UPDATE ON companies
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE companies ENABLE ROW LEVEL SECURITY;

-- Policy: Authenticated users can SELECT companies (read-only public data)
CREATE POLICY companies_select_authenticated ON companies
  FOR SELECT
  TO authenticated
  USING (true);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX companies_domain_idx ON companies(company_domain);
CREATE INDEX companies_source_id_idx ON companies(source_company_id);
CREATE INDEX idx_companies_industry ON companies(industry);
CREATE INDEX idx_companies_size ON companies(company_size);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE companies IS 'Company information with AI-enriched metadata';
COMMENT ON COLUMN companies.source_company_id IS 'Original ID from source data imports';
COMMENT ON COLUMN companies.industry IS 'AI-enriched industry classification';
COMMENT ON COLUMN companies.company_size IS 'AI-enriched size category (1-50, 51-200, etc.)';
COMMENT ON COLUMN companies.description IS 'AI-enriched company description';
COMMENT ON COLUMN companies.headquarters IS 'AI-enriched headquarters location';
