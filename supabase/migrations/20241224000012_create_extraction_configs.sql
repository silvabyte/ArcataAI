-- Migration 12: Create extraction_configs table
-- Created: 2024-12-24
-- Description: Stores extraction configurations for deterministic job data extraction from HTML
-- Epic: ArcataAI-xzo (Config-Driven Job Extraction)

-- ============================================================================
-- EXTRACTION_CONFIGS TABLE
-- ============================================================================
-- Stores configurations that define how to extract job data from HTML pages.
-- Configs are matched against pages using pattern matching, then applied
-- deterministically. AI generates new configs when no match exists.

CREATE TABLE extraction_configs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  
  -- Matching: Patterns to identify which pages this config applies to
  -- JSON array of match patterns, each with type, selector/pattern, etc.
  -- Example: [{"type": "css_exists", "selector": "script[type='application/ld+json']", "content_contains": "JobPosting"}]
  match_patterns JSONB NOT NULL,
  
  -- Deterministic hash of match_patterns for fast lookup
  -- Computed from sorted, canonical JSON representation
  match_hash TEXT NOT NULL,
  
  -- Extraction rules: How to extract each field
  -- JSON object mapping field names to extraction rules
  -- Example: {"title": [{"source": "jsonld", "path": "$.title"}], "description": [...]}
  extract_rules JSONB NOT NULL,
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  
  -- Ensure unique configs per match pattern + version
  UNIQUE(match_hash, version)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Primary lookup: find config by match hash
CREATE INDEX idx_extraction_configs_match_hash ON extraction_configs(match_hash);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Auto-update updated_at timestamp
CREATE TRIGGER extraction_configs_update_updated_at
  BEFORE UPDATE ON extraction_configs
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- RLS POLICIES
-- ============================================================================

-- Enable RLS
ALTER TABLE extraction_configs ENABLE ROW LEVEL SECURITY;

-- Authenticated users can read configs
CREATE POLICY extraction_configs_select_authenticated ON extraction_configs
  FOR SELECT
  TO authenticated
  USING (true);

-- Service role can do everything (for backend API)
CREATE POLICY extraction_configs_all_service ON extraction_configs
  FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE extraction_configs IS 'Stores extraction configurations for deterministic job data extraction from HTML pages';
COMMENT ON COLUMN extraction_configs.match_patterns IS 'JSON array of patterns to match pages. Types: css_exists, url_pattern, content_contains';
COMMENT ON COLUMN extraction_configs.match_hash IS 'SHA256 hash of canonical match_patterns for fast lookup';
COMMENT ON COLUMN extraction_configs.extract_rules IS 'JSON object mapping field names to extraction rules. Sources: jsonld, css, meta, regex';
