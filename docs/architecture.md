# Architecture

Arcata AI is a monorepo with React frontends, a Scala backend, and Supabase
for auth and data persistence.

## System Overview

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│     Auth     │────▶│      HQ      │────▶│     API      │
│    :4200     │     │    :4201     │     │    :4203     │
│  React/Vite  │     │  React/Vite  │     │  Scala/Cask  │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                 │
                     ┌───────────────────────────┼───────────────────────────┐
                     │                           │                           │
              ┌──────▼──────┐            ┌───────▼───────┐           ┌───────▼───────┐
              │  Supabase   │            │ ObjectStorage │           │  BoogieLoops  │
              │   :54321    │            │   (S3-like)   │           │   (AI/LLM)    │
              │  Auth + DB  │            │  Raw HTML     │           │  Job Parsing  │
              └─────────────┘            └───────────────┘           └───────────────┘
```

## Components

### Auth (`apps/auth/`)

Authentication flows: login, signup, email verification. Communicates with
Supabase Auth. Redirects to HQ on successful auth.

### HQ (`apps/hq/`)

Main application. Job stream viewing, application tracking, kanban board.
The primary interface users interact with.

### API (`apps/api/`)

Scala 3 backend handling job ingestion and discovery. 

**Job Ingestion Pipeline:**
- Fetches job postings and stores raw HTML in ObjectStorage
- Uses config-driven extraction with AI fallback
- Tracks extraction quality with CompletionState
- Persists job data to Supabase

**Cron Workflows (async via Castor actors):**
- `JobStatusWorkflow` - Checks if tracked jobs are still active
- `JobDiscoveryWorkflow` - Discovers new jobs from ATS platforms (Greenhouse, etc.)

Workflows are triggered via HTTP (`POST /api/v1/cron/<workflow>`) and return 202 Accepted immediately. Processing happens in background.

See [API Architecture](../apps/api/README.md) for ETL pipeline details.  
See [Job Sources README](../apps/api/src/main/scala/arcata/api/etl/sources/README.md) for adding new job sources.

## Shared Libraries (`libs/`)

| Library | Purpose |
|---------|---------|
| `components` | Shared React UI components |
| `db` | Supabase client, types, and resource APIs |
| `envs` | Environment variable handling |
| `translate` | i18n utilities |

## External Services

| Service | Purpose |
|---------|---------|
| [Supabase](https://supabase.com) | Auth + PostgreSQL database |
| [ObjectStorage](https://github.com/silvabyte/ObjectStorage) | S3-compatible storage for raw job HTML |
| [BoogieLoops](https://github.com/silvabyte/BoogieLoops) | AI service for job posting extraction |
