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

Authentication flows—login, signup, email verification. Communicates with
Supabase Auth. Redirects to HQ on successful auth.

### HQ (`apps/hq/`)

Main application. Job stream viewing, application tracking, kanban board.
The primary interface users interact with.

### API (`apps/api/`)

Scala 3 backend handling job ingestion. Fetches job postings, extracts
structured data via BoogieLoops AI, stores raw HTML in ObjectStorage,
and persists job data to Supabase.

See [API Architecture](../apps/api/README.md) for ETL pipeline details.

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
