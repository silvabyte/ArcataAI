# Config-Driven Job Extraction System

**Created**: 2024-12-24  
**Epic**: ArcataAI-xzo  
**Status**: Implemented

## Problem Statement

The current job extraction pipeline has a fundamental limitation: it uses a monolithic HTML cleaner that doesn't understand the semantics of what it's cleaning. This leads to:

1. **Lost structured data**: JSON-LD in `<head>` is removed entirely
2. **SPA challenges**: Many job sites (Netflix, Greenhouse) load data dynamically
3. **No learning**: Each extraction uses AI, even for sites we've seen before
4. **No visibility**: Can't tell if an extraction was complete or partial

### Example: Netflix Job URL

The Netflix careers page has rich Schema.org JSON-LD data in the `<head>`:
```json
{
  "@type": "JobPosting",
  "title": "Software Engineer (L5), Ads Media Planning",
  "description": "...[full HTML description]...",
  "baseSalary": { "value": { "minValue": 100000, "maxValue": 720000 } }
}
```

But our cleaner removes `<head>`, losing all this structured data. The result is a job record with only title and company, missing description, salary, qualifications, etc.

## Solution: Config-Driven Extraction

### Vision

Replace the monolithic cleaner with a **config-driven extraction system** that:

1. **Learns over time**: Saves working extraction configs to database
2. **Prioritizes deterministic extraction**: Use saved configs when available
3. **Falls back to AI generation**: Create new configs when no match exists
4. **Self-corrects**: AI refines configs in a retry loop until extraction is sufficient
5. **Tracks quality**: Every extraction has a CompletionState indicating data quality

### Key Properties

| Property | Description |
|----------|-------------|
| **Starts dumb, gets smarter** | Initially relies on AI, learns deterministic configs over time |
| **Config as knowledge** | Database of configs becomes institutional knowledge |
| **Efficiency compounds** | More jobs = more configs = less AI needed |
| **Human-auditable** | Configs are inspectable JSON, not black-box AI |

## Architecture

### Main Pipeline Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Job Ingestion Pipeline                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  URL + HTML                                                      │
│      │                                                           │
│      ▼                                                           │
│  ┌──────────────────┐                                           │
│  │ Config Matcher   │ ◄─── extraction_configs table             │
│  │ (pattern match)  │                                           │
│  └────────┬─────────┘                                           │
│           │                                                      │
│     ┌─────┴─────┐                                               │
│     │           │                                                │
│     ▼           ▼                                                │
│  Config      No Config                                          │
│  Found       Found                                               │
│     │           │                                                │
│     ▼           ▼                                                │
│  ┌────────────┐  ┌────────────────────┐                         │
│  │ Determini- │  │ AI Config          │                         │
│  │ stic       │  │ Generator          │                         │
│  │ Extractor  │  │ (see detail below) │                         │
│  └─────┬──────┘  └─────────┬──────────┘                         │
│        │                   │                                     │
│        │            ┌──────┴──────┐                             │
│        │            │ Save Config │                             │
│        │            │ to DB       │                             │
│        │            └──────┬──────┘                             │
│        │                   │                                     │
│        └────────┬──────────┘                                    │
│                 ▼                                                │
│         ExtractedJobData                                         │
│         + CompletionState                                        │
│                 │                                                │
│                 ▼                                                │
│         Continue Pipeline                                        │
│         (company resolver, job loader, etc.)                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### AI Config Generator (Detail)

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI Config Generator                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  HTML + URL                                                      │
│      │                                                           │
│      ▼                                                           │
│  ┌──────────────────┐                                           │
│  │ Attempt 1        │                                           │
│  │ Generate Config  │◄─── AI analyzes page structure            │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────┐                                           │
│  │ Apply Config     │◄─── Use DeterministicExtractor            │
│  │ (extract data)   │                                           │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────┐     ┌─────────────────────┐               │
│  │ Evaluate         │────►│ Complete/Sufficient │───► Return    │
│  │ Completeness     │     └─────────────────────┘    Config +   │
│  └────────┬─────────┘                                Data       │
│           │                                                      │
│           ▼ (Incomplete + attempts < 3)                         │
│  ┌──────────────────┐                                           │
│  │ Attempt N        │                                           │
│  │ Refine Config    │◄─── Context: "Missing: description,       │
│  │ with feedback    │      salary. Selector X returned empty"   │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│       (loop back to Apply Config)                                │
│                                                                  │
│  After 3 attempts:                                               │
│  ┌──────────────────┐                                           │
│  │ Return best      │                                           │
│  │ config + data    │                                           │
│  │ with Partial/    │                                           │
│  │ Minimal state    │                                           │
│  └──────────────────┘                                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Models

### CompletionState Enum

```scala
enum CompletionState:
  case Complete    // 90%+ score - all weighted fields extracted
  case Sufficient  // 70-90% score - required + enough optional
  case Partial     // 50-70% score - some fields, missing important ones
  case Minimal     // <50% but has required - title, company only
  case Failed      // Missing required fields
```

### Weighted Scoring

| Field | Weight | Required |
|-------|--------|----------|
| title | 20 | Yes |
| company_name | 15 | Yes |
| description | 25 | Yes |
| location | 10 | No |
| salary_min/max | 10 | No |
| qualifications | 5 | No |
| responsibilities | 5 | No |
| benefits | 5 | No |
| job_type | 3 | No |
| experience_level | 2 | No |

**Thresholds**:
- Complete: 90%+ score
- Sufficient: 70-90% score  
- Partial: 50-70% score
- Minimal: <50% but has required fields
- Failed: Missing required fields

### Extraction Config Schema

```yaml
id: uuid
name: string
version: integer

# Patterns to match (1 or more must match)
match:
  - type: css_exists | url_pattern | content_contains
    selector: string      # CSS selector (for css_exists)
    pattern: string       # Regex (for url_pattern, content_contains)
    content_contains: string  # Optional filter (for css_exists)

# Extraction rules (try in order until one succeeds)
extract:
  <field_name>:
    - source: jsonld | css | meta | regex
      path: string        # JSONPath (for jsonld)
      selector: string    # CSS selector (for css, regex)
      name: string        # Meta tag name (for meta)
      pattern: string     # Regex with capture group (for regex)
      transform: string   # Optional transformation
```

### Available Transforms

| Transform | Description |
|-----------|-------------|
| `HtmlDecode` | Decode HTML entities (`&amp;` → `&`) |
| `InnerText` | Get text content, strip HTML tags |
| `ParseNumber` | Remove non-numeric chars, parse as number |

*Note: Additional transforms (regex capture, split, json parse) can be added as needed.*

## Database Schema

### extraction_configs table

```sql
-- Simplified schema (YAGNI - removed unused tracking fields)
CREATE TABLE extraction_configs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  
  -- Matching
  match_patterns JSONB NOT NULL,  -- Array of MatchPattern objects
  match_hash TEXT NOT NULL,       -- SHA-256 hash for fast lookup
  
  -- Extraction rules
  extract_rules JSONB NOT NULL,   -- Map of field -> extraction rules
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  
  UNIQUE(match_hash, version)
);

CREATE INDEX idx_extraction_configs_match_hash ON extraction_configs(match_hash);
```

### jobs table additions

```sql
ALTER TABLE jobs ADD COLUMN completion_state TEXT;

-- Valid values: Complete, Sufficient, Partial, Minimal, Failed, Unknown
ALTER TABLE jobs ADD CONSTRAINT jobs_completion_state_check
  CHECK (completion_state IS NULL OR completion_state IN (
    'Complete', 'Sufficient', 'Partial', 'Minimal', 'Failed', 'Unknown'
  ));

CREATE INDEX jobs_completion_state_idx ON jobs(completion_state);
```

## Config Matching Strategy

### Specificity Rules

1. **Most specific wins**: Config with more matching patterns wins
2. **Tie-breaker**: Most recently updated config wins
3. **All patterns must match**: Each pattern in `match` array must match

### Match Pattern Types

| Type | Description | Example |
|------|-------------|---------|
| `css_exists` | Check if CSS selector matches elements | `script[type='application/ld+json']` |
| `url_pattern` | Regex match against URL | `.*\.jobs\.netflix\.net/careers/job/.*` |
| `content_contains` | Check if HTML contains string | `JobPosting` |

## Implementation

All tasks completed. See `apps/api/src/main/scala/arcata/api/extraction/` for implementation.

### File Structure

```
apps/api/src/main/scala/arcata/api/
├── domain/
│   └── ExtractionConfig.scala      # Config domain models (MatchPattern, ExtractionRule, Transform)
├── extraction/
│   ├── CompletionState.scala       # Enum: Complete, Sufficient, Partial, Minimal, Failed, Unknown
│   ├── CompletionScorer.scala      # Weighted scoring system
│   ├── ConfigGenerator.scala       # AI config generation with retry loop
│   ├── ConfigMatcher.scala         # Pattern matching against HTML/URL
│   ├── DeterministicExtractor.scala # Apply configs to extract data
│   └── JsonPathTraverser.scala     # Simple JSONPath for JSON-LD traversal
├── etl/
│   ├── JobIngestionPipeline.scala  # Main pipeline (updated)
│   └── steps/
│       └── JobExtractor.scala      # Orchestrates extraction system
└── clients/
    └── SupabaseClient.scala        # DB operations for extraction_configs

supabase/migrations/
├── 20241224000012_create_extraction_configs.sql  # Config table
└── 20241224000013_add_completion_state_to_jobs.sql  # Jobs completion tracking

supabase/seed.sql  # Includes Schema.org JobPosting seed config
```

### Completed Tasks

| Task | Description | Status |
|------|-------------|--------|
| xzo.1 | CompletionState enum and scoring | ✅ Done |
| xzo.2 | extraction_configs database table | ✅ Done |
| xzo.3 | ExtractionConfig domain models | ✅ Done |
| xzo.4 | ConfigMatcher | ✅ Done |
| xzo.5 | DeterministicExtractor | ✅ Done |
| xzo.6 | AI ConfigGenerator with retry loop | ✅ Done |
| xzo.7 | ExtractionConfigRepository (in SupabaseClient) | ✅ Done |
| xzo.8 | Pipeline integration | ✅ Done |
| xzo.9 | JsonPathTraverser (upickle-based) | ✅ Done |
| xzo.10 | Schema.org JSON-LD seed config | ✅ Done |
| xzo.11 | completion_state column on jobs | ✅ Done |
| xzo.12 | Architecture documentation | ✅ Done |

## Future Considerations

### Background Retry Job
- Cron job to find jobs with `completion_state = 'partial'`
- Re-run extraction with updated configs
- Useful as configs improve over time

### A/B Testing Configs
- Multiple versions of same config
- Track success rates
- Automatically promote better performers

### Manual Config Editor
- UI for creating/editing configs
- Useful for known problematic sites
- Could be admin-only feature

### Config Sharing
- Export/import configs between instances
- Community-contributed configs
- Marketplace of extraction configs

## Success Criteria

1. Netflix job URL extracts: title, description, salary, qualifications, location
2. Extraction config is saved to database
3. Future Netflix jobs use saved config (no AI call)
4. Jobs with partial extraction show appropriate CompletionState
5. Pipeline maintains current performance for complete extractions
