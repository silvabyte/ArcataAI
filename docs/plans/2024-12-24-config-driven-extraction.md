# Config-Driven Job Extraction System

**Created**: 2024-12-24  
**Epic**: ArcataAI-xzo  
**Status**: Planning

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
| `html_decode` | Decode HTML entities (`&amp;` → `&`) |
| `inner_html` | Get innerHTML of element |
| `inner_text` | Get text content, strip tags |
| `regex:<pattern>` | Apply regex, return first capture group |
| `parse_number` | Remove non-numeric chars, parse as number |
| `split:<delim>:<idx>` | Split string, return nth element |
| `json_parse` | Parse as JSON (for embedded JSON strings) |

## Database Schema

### extraction_configs table

```sql
CREATE TABLE extraction_configs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  
  -- Matching
  match_patterns JSONB NOT NULL,
  match_hash TEXT NOT NULL,       -- Deterministic hash for lookup
  
  -- Extraction rules
  extract_rules JSONB NOT NULL,
  
  -- Metadata
  completion_state TEXT NOT NULL DEFAULT 'unknown',
  times_used INTEGER DEFAULT 0,
  times_succeeded INTEGER DEFAULT 0,
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,                -- 'ai' or 'manual'
  
  -- Debugging
  sample_url TEXT,
  sample_extraction JSONB,
  
  UNIQUE(match_hash, version)
);

CREATE INDEX idx_extraction_configs_match_hash ON extraction_configs(match_hash);
CREATE INDEX idx_extraction_configs_completion ON extraction_configs(completion_state);
```

### jobs table additions

```sql
ALTER TABLE jobs
ADD COLUMN completion_state TEXT DEFAULT 'unknown';

ALTER TABLE jobs  
ADD COLUMN extraction_config_id UUID REFERENCES extraction_configs(id);

CREATE INDEX idx_jobs_completion_state ON jobs(completion_state);
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

## Implementation Tasks

See bd issues under epic `ArcataAI-xzo`:

1. **ArcataAI-xzo.1**: Define CompletionState enum and scoring system
2. **ArcataAI-xzo.2**: Create extraction_configs database table
3. **ArcataAI-xzo.3**: Define ExtractionConfig domain models
4. **ArcataAI-xzo.4**: Implement ConfigMatcher
5. **ArcataAI-xzo.5**: Implement DeterministicExtractor
6. **ArcataAI-xzo.6**: Implement AI ConfigGenerator with retry loop
7. **ArcataAI-xzo.7**: Create ExtractionConfigRepository
8. **ArcataAI-xzo.8**: Integrate into JobIngestionPipeline
9. **ArcataAI-xzo.9**: Research upickle vs json-path
10. **ArcataAI-xzo.10**: Create seed config for Schema.org JSON-LD
11. **ArcataAI-xzo.11**: Add completion_state to jobs table
12. **ArcataAI-xzo.12**: Write architecture documentation (this doc)

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
