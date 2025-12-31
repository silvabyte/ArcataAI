# Arcata API

Scala 3 backend for job ingestion and ETL processing.

## Prerequisites

- Java 11+
- [Mill](https://mill-build.org) (build tool, included via `./mill` wrapper)

## Setup

```bash
cd apps/api
cp .env.example .env
```

Update `.env` with keys from `supabase start`:

```
SUPABASE_URL=http://localhost:54321
SUPABASE_ANON_KEY=<anon key>
SUPABASE_SERVICE_ROLE_KEY=<service role key>
SUPABASE_JWT_SECRET=<jwt secret>
```

## Development

```bash
make build    # Compile
make run      # Start server (localhost:4203)
make test     # Run tests
make help     # All commands
```

## ETL Architecture

### Pipelines vs Workflows

| Concept | Execution | Use Case |
|---------|-----------|----------|
| **Pipeline** | Synchronous | API requests expecting immediate response |
| **Workflow** | Async (actor) | Cron jobs, long-running tasks, fire-and-forget |

Workflows extend `BaseWorkflow` which combines:
- `SimpleActor` from Castor for async message processing
- `BasePipeline` for step orchestration

### Triggering Workflows

```
POST /api/v1/cron/<workflow-name>
→ 202 Accepted { "runId": "..." }
```

Workflows process in background. Results logged, not returned to caller.

### Job Ingestion Pipeline

The job ingestion pipeline processes URLs into structured job data using a
**config-driven extraction system** that learns over time:

```
┌─────────────────────────────────────────────────────────────────┐
│                    JobIngestionPipeline                         │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  HtmlFetcher  │────▶│ JobExtractor  │────▶│  HtmlCleaner  │
│  Fetch HTML   │     │ Config-driven │     │  (for company │
│  Store in S3  │     │ extraction    │     │  enrichment)  │
└───────────────┘     └───────┬───────┘     └───────┬───────┘
                              │                     │
                              ▼                     ▼
                      ┌───────────────┐     ┌───────────────┐
                      │CompanyResolver│◄────│   markdown    │
                      │ Match/create  │     │   content     │
                      └───────┬───────┘     └───────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│   JobLoader   │     │ StreamLoader  │     │ApplicationLoader│
│  Insert job   │     │ Add to stream │     │  Create app   │
│  + comp.state │     │               │     │  (optional)   │
└───────────────┘     └───────────────┘     └───────────────┘
```

### Pipeline Steps

| Step | Responsibility |
|------|----------------|
| `HtmlFetcher` | Fetches job URL, stores raw HTML in ObjectStorage |
| `JobExtractor` | Config-driven extraction with AI fallback (see below) |
| `HtmlCleaner` | Converts HTML to markdown for company enrichment |
| `CompanyResolver` | Matches or creates company record in Supabase |
| `JobLoader` | Inserts job with completion_state |
| `StreamLoader` | Adds job to user's stream |
| `ApplicationLoader` | Creates job application record (optional) |

### Config-Driven Extraction

The `JobExtractor` step uses a self-improving extraction system:

```
HTML + URL
    │
    ▼
┌──────────────────┐
│ ConfigMatcher    │ ◄─── extraction_configs table
└────────┬─────────┘
         │
   ┌─────┴─────┐
   │           │
   ▼           ▼
Config      No Config
Found       Found
   │           │
   ▼           ▼
┌────────────┐  ┌────────────────────┐
│Determini-  │  │ AI Config          │
│stic        │  │ Generator          │
│Extractor   │  │ (3 retry attempts) │
└─────┬──────┘  └─────────┬──────────┘
      │                   │
      │            ┌──────┴──────┐
      │            │ Save Config │
      │            │ to DB       │
      │            └──────┬──────┘
      │                   │
      └────────┬──────────┘
               ▼
       ExtractedJobData
       + CompletionState
```

**Key benefits:**
- Deterministic extraction for known sites (no AI needed)
- AI generates reusable configs for new sites
- Configs saved to database for future use
- Tracks extraction quality (Complete, Sufficient, Partial, etc.)

See [Config-Driven Extraction Architecture](../../docs/plans/2024-12-24-config-driven-extraction.md)
for full details.

### Job Status Workflow

Async workflow that checks if tracked jobs are still active:

```
POST /api/v1/cron/job-status-check
Content-Type: application/json
X-Cron-Secret: <optional secret>

{ "batchSize": 100, "olderThanDays": 7 }

→ 202 Accepted
```

Pipeline steps:
1. `JobsToCheckFetcher` - Query open jobs not checked recently
2. `JobStatusChecker` - Re-fetch job URLs, detect if closed
3. `JobStatusUpdater` - Update job status in database

Closed jobs are filtered from user job streams.

## External Dependencies

- [ObjectStorage](https://github.com/silvabyte/ObjectStorage) - Raw HTML storage
- [BoogieLoops](https://github.com/silvabyte/BoogieLoops) - AI job extraction

## Documentation

- [Mill Build Tool](https://mill-build.org/docs)
- [Cask Web Framework](https://com-lihaoyi.github.io/cask/)
- [System Architecture](../../docs/architecture.md)
