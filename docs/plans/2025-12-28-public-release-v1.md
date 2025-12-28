# Public Release v1.0 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Prepare and execute the initial public release of ArcataAI, including a public roadmap and community integration.

**Architecture:**
- **Frontend (`apps/hq`):** Add a new "Roadmap" page to transparency communicate project status.
- **Community:** Integrate with "Campfire" (community app) by automating user invites upon registration.
- **Infrastructure:** Ensure all services are build-ready and deployed.

**Tech Stack:** React, React Router, Tailwind CSS, Supabase (Triggers/Functions).

### Task 1: Implement Roadmap Page

**Files:**
- Create: `apps/hq/src/routes/roadmap/RoadmapPage.tsx`
- Modify: `apps/hq/src/routes/App.tsx`
- Modify: `apps/hq/public/locales/en.json` (for translations if needed, or hardcode for now as per snippet)

**Step 1: Create Roadmap Page Component**
Create the component using the provided template.

**Step 2: Register Route**
Add `/roadmap` to `apps/hq/src/routes/App.tsx`.

**Step 3: Add Navigation Item**
Add "Roadmap" to the sidebar and mobile navigation in `App.tsx`.

**Step 4: Verify**
Run dev server and check navigation to `/roadmap`.

### Task 2: Community Invite Automation

**Goal:** Automatically invite new users to the Campfire community upon signup.
**Approach:** Use a Supabase Database Webhook or Trigger to call an edge function or API endpoint when a new user is created in `auth.users`.

**Files:**
- Create: `supabase/migrations/20241228000000_auto_invite_function.sql`

**Step 1: Create Database Function**
Write a Postgres function `handle_new_user()` that:
1.  Logs the new user creation.
2.  (Placeholder) Calls an external API or inserts into a `community_invites` table.
    *For V1, we will create a `community_invites` table to queue these invites if the API isn't ready, or log it.*
    *Actually, let's assume we just need the hook point.*

**Step 2: Create Trigger**
Create a trigger `on_auth_user_created` that runs `handle_new_user()` after insert on `auth.users`.

### Task 3: Release Pre-flight

**Files:**
- Check: `apps/hq/package.json`
- Check: `apps/api/build.sc`

**Step 1: Lint & Type Check**
Run `bun x ultracite check` in root.
Run `mill api.compile` in `apps/api`.

**Step 2: Build Verification**
Run `bun run build` in `apps/hq`.

**Step 3: Tag Release**
(Manual) Git tag the release.

