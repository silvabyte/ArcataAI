# Greenhouse ETL Module

This module provides optimized job ingestion from Greenhouse ATS without AI extraction.

## Overview

Greenhouse provides a [public Job Board API](https://developers.greenhouse.io/job-board.html) that returns structured JSON data. Instead of fetching HTML and using AI to extract job details, we can parse the JSON directly. This is:

- **Faster**: No LLM API call latency
- **Cheaper**: No token costs
- **More reliable**: Structured data vs. AI interpretation

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      JobDiscoveryWorkflow                           │
│                                                                     │
│  ┌─────────────┐                                                    │
│  │DiscoverJobs │──────┐                                             │
│  └─────────────┘      │                                             │
│                       ▼                                             │
│              ┌────────────────┐                                     │
│              │  Route by      │                                     │
│              │  Source Type   │                                     │
│              └────────┬───────┘                                     │
│                       │                                             │
│         ┌─────────────┴─────────────┐                               │
│         │                           │                               │
│         ▼                           ▼                               │
│  ┌──────────────────┐    ┌─────────────────────────┐                │
│  │ Greenhouse +     │    │ Other sources or        │                │
│  │ apiUrl defined   │    │ no apiUrl               │                │
│  └────────┬─────────┘    └───────────┬─────────────┘                │
│           │                          │                              │
│           ▼                          ▼                              │
│  ┌──────────────────────┐  ┌─────────────────────────┐              │
│  │GreenhouseIngestion   │  │ JobIngestionPipeline    │              │
│  │Pipeline (NO AI)      │  │ (AI-powered)            │              │
│  └──────────────────────┘  └─────────────────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

## Components

### GreenhouseDiscovery

Discovers job URLs from the Greenhouse Job Board API.

**List API**: `GET boards-api.greenhouse.io/v1/boards/{board_token}/jobs`

Returns job IDs, titles, and URLs. For each job, we also construct the detail API URL for later use.

### GreenhouseJobFetcher

Fetches detailed job data from Greenhouse.

**Detail API**: `GET boards-api.greenhouse.io/v1/boards/{board_token}/jobs/{id}?pay_transparency=true`

The `pay_transparency=true` parameter includes salary range information when available.

### GreenhouseJobParser

Parses the JSON response into `ExtractedJobData`.

**Field Mapping**:

| Greenhouse Field | ExtractedJobData | Transformation |
|-----------------|------------------|----------------|
| `title` | `title` | Direct |
| `location.name` | `location` | Direct |
| `content` | `description` | Unescape HTML entities |
| `absolute_url` | `applicationUrl` | Direct |
| `pay_input_ranges[0].min_cents` | `salaryMin` | Divide by 100 (cents to dollars) |
| `pay_input_ranges[0].max_cents` | `salaryMax` | Divide by 100 |
| `pay_input_ranges[0].currency_type` | `salaryCurrency` | Direct |
| (inferred from location) | `isRemote` | Contains "remote" (case-insensitive) |

**Fields not available from Greenhouse API** (left as `None`):
- `companyName` - Already known from discovery phase
- `jobType`, `experienceLevel`, `educationLevel`
- `qualifications`, `preferredQualifications`, `responsibilities`, `benefits`
- `category`, `postedDate`, `closingDate`

### GreenhouseIngestionPipeline

Orchestrates the full ingestion flow:

1. `GreenhouseJobFetcher` - Fetch JSON from API
2. `GreenhouseJobParser` - Parse JSON to `ExtractedJobData`
3. `JobTransformer` - Sanitize/normalize (shared step)
4. `JobLoader` - Create job record (shared step)
5. `StreamLoader` - Add to job stream (shared step)

**Skipped steps** (compared to `JobIngestionPipeline`):
- `HtmlFetcher` - We fetch JSON, not HTML
- `HtmlCleaner` - No HTML to clean
- `JobExtractor` - No AI needed!
- `CompanyResolver` - Company already known from discovery

## Usage

The `JobDiscoveryWorkflow` automatically routes jobs based on source:

```scala
// Greenhouse jobs with API URLs use the optimized pipeline
(job.source, job.apiUrl) match {
  case (JobSource.Greenhouse, Some(apiUrl)) =>
    greenhouseIngestionPipeline.run(...)
  case _ =>
    aiIngestionPipeline.run(...)  // Fallback to AI
}
```

## Adding New ATS Sources

To add support for another ATS (e.g., Lever):

1. Create `etl/lever/` directory
2. Implement `LeverDiscovery` - discover job URLs
3. Implement `LeverJobFetcher` - fetch structured data
4. Implement `LeverJobParser` - parse to `ExtractedJobData`
5. Implement `LeverIngestionPipeline` - orchestrate steps
6. Add `JobSource.Lever` enum case
7. Update `JobDiscoveryWorkflow` routing

## API Reference

### Greenhouse Job Board API

- **Base URL**: `https://boards-api.greenhouse.io/v1`
- **Authentication**: None required (public API)
- **Rate Limits**: Be conservative (1 req/sec recommended)

**Endpoints used**:
- `GET /boards/{token}/jobs` - List all jobs
- `GET /boards/{token}/jobs/{id}?pay_transparency=true` - Job details with salary

**Documentation**: https://developers.greenhouse.io/job-board.html
