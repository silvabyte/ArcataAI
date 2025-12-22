-- Migration 2: Create companies and jobs tables
-- Created: 2024-12-20
-- Description: Creates companies and jobs tables with RLS policies for authenticated read-only access

-- Ensure the update_updated_at_column trigger function exists (idempotent)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMPANIES TABLE
-- ============================================================================

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
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS on companies
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Authenticated users can SELECT companies
CREATE POLICY companies_select_authenticated ON companies
  FOR SELECT
  TO authenticated
  USING (true);

-- Trigger: Auto-update updated_at on companies
CREATE TRIGGER companies_update_updated_at
  BEFORE UPDATE ON companies
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- Index: company_domain for fast lookups
CREATE INDEX companies_domain_idx ON companies(company_domain);

-- Index: source_company_id for lookups by original ID
CREATE INDEX companies_source_id_idx ON companies(source_company_id);

-- ============================================================================
-- JOBS TABLE
-- ============================================================================

CREATE TABLE jobs (
  job_id SERIAL PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT,
  location TEXT,
  job_type TEXT,
  category TEXT,
  experience_level TEXT,
  education_level TEXT,
  salary_range TEXT,
  benefits TEXT[],
  qualifications TEXT[],
  preferred_qualifications TEXT[],
  responsibilities TEXT[],
  application_url TEXT,
  application_email TEXT,
  contact_email TEXT,
  contact_phone TEXT,
  source_url TEXT,
  raw_html_object_id UUID,
  posted_date TIMESTAMPTZ,
  closing_date TIMESTAMPTZ,
  status TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS on jobs
ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Authenticated users can SELECT jobs
CREATE POLICY jobs_select_authenticated ON jobs
  FOR SELECT
  TO authenticated
  USING (true);

-- Trigger: Auto-update updated_at on jobs
CREATE TRIGGER jobs_update_updated_at
  BEFORE UPDATE ON jobs
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- Index: company_id foreign key for fast joins
CREATE INDEX jobs_company_id_idx ON jobs(company_id);
