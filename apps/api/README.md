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

The job ingestion pipeline processes URLs into structured job data:

```
┌─────────────────────────────────────────────────────────────────┐
│                    JobIngestionPipeline                         │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  HtmlFetcher  │────▶│   JobParser   │────▶│CompanyResolver│
│  Fetch HTML   │     │ Extract data  │     │ Match/create  │
│  Store in S3  │     │ via AI        │     │ company       │
└───────────────┘     └───────────────┘     └───────┬───────┘
                                                    │
        ┌─────────────────────┬─────────────────────┤
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│   JobLoader   │     │ StreamLoader  │     │ApplicationLoader│
│  Upsert job   │     │ Add to stream │     │  Create app   │
└───────────────┘     └───────────────┘     └───────────────┘
```

### Pipeline Steps

| Step | Responsibility |
|------|----------------|
| `HtmlFetcher` | Fetches job URL, stores raw HTML in ObjectStorage |
| `JobParser` | Sends HTML to BoogieLoops AI for structured extraction |
| `CompanyResolver` | Matches or creates company record in Supabase |
| `JobLoader` | Upserts job record |
| `StreamLoader` | Adds job to user's stream |
| `ApplicationLoader` | Creates job application record |

## External Dependencies

- [ObjectStorage](https://github.com/silvabyte/ObjectStorage) - Raw HTML storage
- [BoogieLoops](https://github.com/silvabyte/BoogieLoops) - AI job extraction

## Documentation

- [Mill Build Tool](https://mill-build.org/docs)
- [Cask Web Framework](https://com-lihaoyi.github.io/cask/)
- [System Architecture](../../docs/architecture.md)
