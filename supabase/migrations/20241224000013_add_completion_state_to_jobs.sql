-- Add completion_state column to track extraction quality
-- Values match the CompletionState enum: Complete, Sufficient, Partial, Minimal, Failed, Unknown

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS completion_state TEXT;

-- Add check constraint for valid values
ALTER TABLE jobs ADD CONSTRAINT jobs_completion_state_check
  CHECK (completion_state IS NULL OR completion_state IN (
    'Complete', 'Sufficient', 'Partial', 'Minimal', 'Failed', 'Unknown'
  ));

-- Add index for filtering by completion state (useful for finding jobs needing re-extraction)
CREATE INDEX IF NOT EXISTS jobs_completion_state_idx ON jobs(completion_state);

-- Comment for documentation
COMMENT ON COLUMN jobs.completion_state IS 
  'Quality level of the extraction: Complete (100%), Sufficient (75%+), Partial (50%+), Minimal (25%+), Failed (<25%), Unknown';
