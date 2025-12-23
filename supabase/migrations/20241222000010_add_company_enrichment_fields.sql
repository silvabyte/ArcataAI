-- Migration 10: Add company enrichment fields
-- Created: 2024-12-22
-- Description: Adds AI-enriched fields for company data (industry, size, description, headquarters)

-- ============================================================================
-- COMPANY ENRICHMENT FIELDS
-- ============================================================================

-- Add industry classification (e.g., "Technology", "Healthcare", "Finance")
ALTER TABLE companies ADD COLUMN IF NOT EXISTS industry VARCHAR;

-- Add company size category (e.g., "1-50", "51-200", "201-500", "501-1000", "1001+")
ALTER TABLE companies ADD COLUMN IF NOT EXISTS company_size VARCHAR;

-- Add company description from AI enrichment
ALTER TABLE companies ADD COLUMN IF NOT EXISTS description TEXT;

-- Add headquarters location (e.g., "San Francisco, CA", "New York, NY")
ALTER TABLE companies ADD COLUMN IF NOT EXISTS headquarters VARCHAR;

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Index for filtering by industry
CREATE INDEX IF NOT EXISTS idx_companies_industry ON companies(industry);

-- Index for filtering by company size
CREATE INDEX IF NOT EXISTS idx_companies_size ON companies(company_size);
