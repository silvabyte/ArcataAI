# Arcata

Job application tracking that you can depend on for your entire career.

Most job platforms are either overly predatory or they blur the lines there,
or they rug pull you after you've invested a lot of time, effort and energy into the platform.

With Arcata, your data is yours. The code is open source under the O'Saasy license. If you're
ever unhappy with our service, export your data and self-host.

I personally am getting exhausted of having the products I love vaporize or squeeze me out for the sake of a bigger payout.

## Quick Start

### Prerequisites

- [Bun](https://bun.sh) 1.x
- [Supabase CLI](https://supabase.com/docs/guides/cli)
- [Mill](https://mill-build.org) (for API development)

### Setup

```bash
git clone https://github.com/silvabyte/ArcataAI.git
cd ArcataAI
bun install
cp .env.example .env
```

### Local Supabase

```bash
supabase start
```

Note the `anon key` and `service_role key` from the output. Update `.env`:

```
VITE_SUPABASE_URL=http://localhost:54321
VITE_SUPABASE_KEY=<anon key from supabase start>
```

See [Supabase CLI docs](https://supabase.com/docs/guides/cli) for database
migrations, seeding, and more.

### Run Development Servers

```bash
bun run dev                # All apps
bun run --filter hq dev    # HQ (main app) - localhost:4201
bun run --filter auth dev  # Auth - localhost:4200
```

For the Scala API, see [apps/api/README.md](apps/api/README.md).

### Lint & Format

```bash
bun run lint       # Check
bun run lint:fix   # Fix
```

## Project Structure

```
apps/
  api/         Scala backend - job ingestion ETL
  auth/        Authentication frontend (React/Vite)
  hq/          Main application (React/Vite)
libs/
  components/  Shared React components
  db/          Supabase client & types
  envs/        Environment variable handling
  translate/   i18n utilities
supabase/      Database migrations & config
```

## Documentation

- [Architecture Overview](docs/architecture.md)
- [API Service](apps/api/README.md)

## License

[O'Saasy License](LICENSE.md) - Open source with anti-compete SaaS clause.
Your data is yours. Self-host anytime.
