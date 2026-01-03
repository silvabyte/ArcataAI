# Job Sources Framework

This directory contains the `JobSource` enum for discovering jobs from various ATS (Applicant Tracking System) platforms.

## Overview

The `JobDiscoveryWorkflow` iterates through all `JobSource` enum values and discovers new jobs to ingest. Each source delegates to a source-specific discovery module.

## Architecture

```
etl/
├── sources/
│   ├── JobSource.scala      # Enum registry + DiscoveredJob type
│   └── README.md            # This file
├── greenhouse/              # Greenhouse-specific implementation
│   ├── GreenhouseDiscovery.scala
│   ├── GreenhouseJobFetcher.scala
│   ├── GreenhouseJobParser.scala
│   ├── GreenhouseIngestionPipeline.scala
│   └── README.md
├── lever/                   # Future: Lever implementation
└── workflows/
    └── JobDiscoveryWorkflow.scala  # Orchestrates discovery + routing
```

## Adding a New Job Source

### 1. Create the Source Directory

```bash
mkdir -p apps/api/src/main/scala/arcata/api/etl/{source_name}
mkdir -p apps/api/src/test/scala/arcata/api/etl/{source_name}
```

### 2. Implement Discovery Module

Create `{Source}Discovery.scala`:

```scala
package arcata.api.etl.lever

object LeverDiscovery extends Logging {
  def discoverJobs(supabase: SupabaseClient, config: JobSourceConfig): Seq[DiscoveredJob] = {
    val companies = supabase.findCompaniesByJobSource("lever", config.companyBatchSize)
    
    companies.flatMap { company =>
      // Call Lever API to discover job URLs
      // Return DiscoveredJob with source, apiUrl, metadata
    }
  }
}
```

### 3. Add Enum Case

Edit `JobSource.scala`:

```scala
enum JobSource(...) {
  case Greenhouse extends JobSource(...)
  
  case Lever extends JobSource(
    sourceId = "lever",
    name = "Lever ATS",
    config = JobSourceConfig(
      companyBatchSize = 1,
      jobsPerCompany = 10,
      delayBetweenRequestsMs = 1000
    )
  )
  
  def discoverJobs(supabase: SupabaseClient): Seq[DiscoveredJob] = this match
    case Greenhouse => GreenhouseDiscovery.discoverJobs(supabase, config)
    case Lever      => LeverDiscovery.discoverJobs(supabase, config)
}
```

### 4. (Optional) Implement Optimized Ingestion Pipeline

If the ATS provides structured API data (like Greenhouse), create an optimized pipeline:

```scala
// etl/lever/LeverIngestionPipeline.scala
class LeverIngestionPipeline(supabaseClient: SupabaseClient)
    extends BasePipeline[LeverIngestionInput, LeverIngestionOutput] {
  // Fetch structured data from API
  // Parse directly to ExtractedJobData (no AI needed)
  // Load to database
}
```

Then update `JobDiscoveryWorkflow` to route:

```scala
(job.source, job.apiUrl) match {
  case (JobSource.Greenhouse, Some(apiUrl)) => greenhousePipeline.run(...)
  case (JobSource.Lever, Some(apiUrl))      => leverPipeline.run(...)
  case _                                     => aiPipeline.run(...)  // Fallback
}
```

### 5. Add Database Migration (if needed)

If your source uses a new value for `company_jobs_source`, update the CHECK constraint:

```sql
ALTER TABLE companies
DROP CONSTRAINT companies_jobs_source_check;

ALTER TABLE companies
ADD CONSTRAINT companies_jobs_source_check
CHECK (company_jobs_source IS NULL OR company_jobs_source IN (
  'greenhouse',
  'lever',      -- Add new source here
  'ashby',
  'workday',
  'icims',
  'workable',
  'custom'
));
```

## DiscoveredJob Type

Jobs returned from discovery include routing information:

```scala
case class DiscoveredJob(
    url: String,                      // Normalized public job URL
    source: JobSource,                // For routing to optimized pipeline
    companyId: Option[Long] = None,   // Known company ID (skip resolver)
    apiUrl: Option[String] = None,    // Direct API URL for structured data
    metadata: Map[String, String] = Map.empty
)
```

## Configuration

Each source defines its own `JobSourceConfig`:

| Field | Description | Default |
|-------|-------------|---------|
| `companyBatchSize` | Max companies to process per workflow run | 1 |
| `jobsPerCompany` | Max jobs to fetch per company | 1 |
| `delayBetweenRequestsMs` | Rate limiting delay between HTTP requests | 1000 |

**Important:** Start with conservative defaults (1 company, 1 job, 1s delay) to avoid hitting rate limits. Increase gradually after testing.

## Prerequisites

For a source to discover jobs, companies must have:

1. `company_jobs_source` set to the source's `sourceId` (e.g., "greenhouse")
2. A valid `company_jobs_url` pointing to their job board

Example:
```sql
UPDATE companies
SET company_jobs_source = 'greenhouse',
    company_jobs_url = 'https://boards.greenhouse.io/temporal'
WHERE company_domain = 'temporal.io';
```

## Utility Functions

### UrlNormalizer

Use `UrlNormalizer` for consistent URL handling:

```scala
// Normalize URLs for deduplication
val normalized = UrlNormalizer.normalize("https://example.com/job/123?utm_source=x")
// Result: "https://example.com/job/123"

// Extract Greenhouse company ID from URL
val companyId = UrlNormalizer.extractGreenhouseCompanyId(
  "https://boards.greenhouse.io/temporal"
)
// Result: Some("temporal")
```

## Testing

Run tests from the api directory:

```bash
cd apps/api
make test
```

Tests are in:
- `src/test/scala/arcata/api/etl/sources/JobSourceSuite.scala`
- `src/test/scala/arcata/api/etl/greenhouse/GreenhouseJobParserSuite.scala`
- `src/test/scala/arcata/api/util/UrlNormalizerSuite.scala`

## Current Sources

| Source | sourceId | Ingestion | API Documentation |
|--------|----------|-----------|-------------------|
| Greenhouse ATS | `greenhouse` | Optimized (no AI) | [Greenhouse Job Board API](https://developers.greenhouse.io/job-board.html) |

See [Greenhouse README](../greenhouse/README.md) for implementation details.
