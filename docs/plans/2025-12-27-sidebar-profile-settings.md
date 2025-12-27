# Sidebar Profile & Settings Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire up the sidebar profile action to display current user info and navigate to a new Settings page.

**Architecture:**
- Extend the existing `App` route in `apps/hq/src/routes/App.tsx` to include a `/settings` child route.
- Create a new `Settings` page component based on the provided template.
- Update the sidebar in `App` to render dynamic user data.

**Tech Stack:** React, React Router, HeadlessUI, Heroicons, Supabase Auth (for user data).

### Task 1: Create Settings Page Component

**Files:**
- Create: `apps/hq/src/routes/settings/Settings.tsx`
- Modify: None

**Step 1: Create the component file**

Create `apps/hq/src/routes/settings/Settings.tsx` with the provided template code.
Adapt imports if necessary (e.g., icons are already available).
Ensure it exports `Settings` as default or named export.

**Step 2: Fix Imports**

The example uses `@heroicons/react/24/outline` and `@heroicons/react/20/solid`.
Check if these are available in `package.json`. If not, use available icons from `@heroicons/react/24/outline` which is used in `App.tsx`.

**Step 3: Verification**
Run `bun run build` (or relevant build command) to ensure no TS errors.

### Task 2: Add Settings Route

**Files:**
- Modify: `apps/hq/src/routes/App.tsx`

**Step 1: Import Settings component**

```typescript
import Settings from "./settings/Settings";
```

**Step 2: Add child route**

In `route` object in `App.tsx`:

```typescript
children: [
  // ... existing children
  {
    path: "settings",
    element: <Settings />,
  },
]
```

**Step 3: Verification**
Start the app and navigate to `/settings` manually to verify it renders.

### Task 3: Update Sidebar Profile

**Files:**
- Modify: `apps/hq/src/routes/App.tsx`

**Step 1: Access User Data**

In `App` component:
```typescript
import { useLoaderData } from "react-router-dom";
import type { User } from "@supabase/supabase-js"; // or from @arcata/db if re-exported

// ... inside App component
const { user } = useLoaderData() as { user: User };
```

**Step 2: Helper for User Info**

Create helpers to extract name and avatar:
```typescript
const userName = user.user_metadata?.full_name || user.email || "User";
const userAvatar = user.user_metadata?.avatar_url;
```

**Step 3: Update Desktop Sidebar**

Replace the static button with:

```tsx
<a href="/settings" className="flex items-center gap-x-4 px-6 py-3 text-sm font-semibold leading-6 text-white hover:bg-gray-800">
  {userAvatar ? (
    <img className="h-8 w-8 rounded-full bg-gray-800" src={userAvatar} alt="" />
  ) : (
    <UserCircleIcon className="h-8 w-8 text-gray-400" aria-hidden="true" />
  )}
  <span className="sr-only">Your profile</span>
  <span aria-hidden="true">{userName}</span>
</a>
```

**Step 4: Update Mobile Sidebar**

Apply similar changes to the mobile sidebar section.

**Step 5: Verification**
Check if sidebar shows user info and clicking it goes to `/settings`.

