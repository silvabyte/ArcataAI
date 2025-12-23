# Job URL Submission Flow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Connect the HQ frontend to the Scala API so users can submit job URLs, have them processed via ETL, and see results in the job stream/tracker.

**Architecture:** User submits URL via AddJobPopover -> Route action calls API client -> Scala API runs ETL pipeline -> Response triggers data revalidation -> JobDetailPanel opens with new job.

**Tech Stack:** React 18, React Router 6, HeadlessUI, Supabase Auth, TypeScript, Tailwind CSS

---

## Issue Tracker

All tasks are tracked in `bd`. Run `bd ready` to see unblocked work.

```
Epic: ArcataAI-0cn - Job URL Submission Flow

├── ArcataAI-0cn.1: Add VITE_API_URL environment variable [READY]
│   └── ArcataAI-0cn.2: Create API client for Scala backend
│       └── ArcataAI-0cn.5: Update AddJobByUrl action to call real API
│           └── ArcataAI-0cn.6: Update AddJobPopover UI for processing states
│
├── ArcataAI-0cn.3: Create Notification component [READY]
│   ├── ArcataAI-0cn.6: (blocked by this)
│   └── ArcataAI-0cn.7: Integrate NotificationProvider into HQ app
│
├── ArcataAI-0cn.4: Create JobDetailPanel side panel component [READY]
│   └── ArcataAI-0cn.6: (blocked by this)
│
└── ArcataAI-0cn.8: Add translations for job submission flow [READY]
```

---

## Task 1: Add VITE_API_URL Environment Variable (ArcataAI-0cn.1)

**Files:**
- Modify: `.env.example`

**Step 1: Update .env.example**

```bash
# Read current file
cat .env.example
```

Add the following line after `VITE_HQ_BASE_URL`:

```env
# API URL (Scala backend)
VITE_API_URL=http://localhost:4203
```

**Step 2: Commit**

```bash
git add .env.example
git commit -m "feat: add VITE_API_URL environment variable for Scala API"
bd close ArcataAI-0cn.1 --reason "Added VITE_API_URL to .env.example"
```

---

## Task 2: Create API Client for Scala Backend (ArcataAI-0cn.2)

**Files:**
- Create: `apps/hq/src/lib/api.ts`

**Step 1: Create the API client**

```typescript
// apps/hq/src/lib/api.ts
import { ui } from "@arcata/envs";

// Types based on OpenAPI spec from /openapi endpoint
export interface IngestJobRequest {
  url: string;
  source?: string;
  createApplication?: boolean;
  notes?: string;
}

export interface IngestJobResponse {
  success: boolean;
  jobId: number;
  streamId?: number;
  applicationId?: number;
  message: string;
}

export interface JobErrorResponse {
  success: boolean;
  error: string;
  details?: string;
}

export type IngestJobResult = 
  | { ok: true; data: IngestJobResponse }
  | { ok: false; error: JobErrorResponse };

/**
 * Get the API base URL from environment.
 * Defaults to localhost:4203 for development.
 */
function getApiUrl(): string {
  return ui.get("VITE_API_URL") || "http://localhost:4203";
}

/**
 * Ingest a job from a URL via the Scala ETL API.
 * 
 * @param request - The job ingestion request
 * @param token - Supabase JWT access token
 * @returns Result object with either success data or error
 */
export async function ingestJob(
  request: IngestJobRequest,
  token: string
): Promise<IngestJobResult> {
  const url = `${getApiUrl()}/api/v1/jobs/ingest`;

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
      body: JSON.stringify(request),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      return { ok: true, data: data as IngestJobResponse };
    }

    return { 
      ok: false, 
      error: data as JobErrorResponse 
    };
  } catch (error) {
    return {
      ok: false,
      error: {
        success: false,
        error: "Network error",
        details: error instanceof Error ? error.message : "Failed to connect to API",
      },
    };
  }
}

/**
 * Health check for the API.
 */
export async function pingApi(): Promise<boolean> {
  try {
    const response = await fetch(`${getApiUrl()}/api/v1/ping`);
    return response.ok;
  } catch {
    return false;
  }
}
```

**Step 2: Commit**

```bash
git add apps/hq/src/lib/api.ts
git commit -m "feat: add API client for Scala backend job ingestion"
bd close ArcataAI-0cn.2 --reason "Created typed API client with ingestJob function"
```

---

## Task 3: Create Notification Component (ArcataAI-0cn.3)

**Files:**
- Create: `libs/components/src/lib/notifications/Notification.tsx`
- Create: `libs/components/src/lib/notifications/NotificationProvider.tsx`
- Create: `libs/components/src/lib/notifications/index.ts`
- Modify: `libs/components/src/lib/index.ts`

**Step 1: Create Notification.tsx**

```typescript
// libs/components/src/lib/notifications/Notification.tsx
import { Transition } from "@headlessui/react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { Fragment } from "react";

export type NotificationVariant = "loading" | "success" | "error";

export interface NotificationProps {
  show: boolean;
  onClose: () => void;
  title: string;
  message?: string;
  variant: NotificationVariant;
}

function LoadingSpinner() {
  return (
    <svg
      aria-hidden="true"
      className="size-6 animate-spin text-gray-400"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        fill="currentColor"
      />
    </svg>
  );
}

function VariantIcon({ variant }: { variant: NotificationVariant }) {
  switch (variant) {
    case "loading":
      return <LoadingSpinner />;
    case "success":
      return (
        <CheckCircleIcon aria-hidden="true" className="size-6 text-green-400" />
      );
    case "error":
      return (
        <ExclamationCircleIcon
          aria-hidden="true"
          className="size-6 text-red-400"
        />
      );
  }
}

export function Notification({
  show,
  onClose,
  title,
  message,
  variant,
}: NotificationProps) {
  return (
    <div
      aria-live="assertive"
      className="pointer-events-none fixed inset-0 z-50 flex items-end px-4 py-6 sm:items-start sm:p-6"
    >
      <div className="flex w-full flex-col items-center space-y-4 sm:items-end">
        <Transition
          as={Fragment}
          enter="transform ease-out duration-300 transition"
          enterFrom="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
          enterTo="translate-y-0 opacity-100 sm:translate-x-0"
          leave="transition ease-in duration-100"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
          show={show}
        >
          <div className="pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg bg-white shadow-lg ring-1 ring-black/5">
            <div className="p-4">
              <div className="flex items-start">
                <div className="shrink-0">
                  <VariantIcon variant={variant} />
                </div>
                <div className="ml-3 w-0 flex-1 pt-0.5">
                  <p className="text-sm font-medium text-gray-900">{title}</p>
                  {message ? (
                    <p className="mt-1 text-sm text-gray-500">{message}</p>
                  ) : null}
                </div>
                {variant !== "loading" ? (
                  <div className="ml-4 flex shrink-0">
                    <button
                      className="inline-flex rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                      onClick={onClose}
                      type="button"
                    >
                      <span className="sr-only">Close</span>
                      <XMarkIcon aria-hidden="true" className="size-5" />
                    </button>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  );
}
```

**Step 2: Create NotificationProvider.tsx**

```typescript
// libs/components/src/lib/notifications/NotificationProvider.tsx
import {
  createContext,
  useCallback,
  useContext,
  useState,
  type ReactNode,
} from "react";
import { Notification, type NotificationVariant } from "./Notification";

interface NotificationState {
  show: boolean;
  title: string;
  message?: string;
  variant: NotificationVariant;
}

interface NotificationContextValue {
  notify: (
    title: string,
    variant: NotificationVariant,
    message?: string,
    autoDismiss?: number
  ) => void;
  dismiss: () => void;
}

const NotificationContext = createContext<NotificationContextValue | null>(
  null
);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<NotificationState>({
    show: false,
    title: "",
    variant: "success",
  });

  const dismiss = useCallback(() => {
    setState((prev) => ({ ...prev, show: false }));
  }, []);

  const notify = useCallback(
    (
      title: string,
      variant: NotificationVariant,
      message?: string,
      autoDismiss?: number
    ) => {
      setState({ show: true, title, message, variant });

      if (autoDismiss && variant !== "loading") {
        setTimeout(dismiss, autoDismiss);
      }
    },
    [dismiss]
  );

  return (
    <NotificationContext.Provider value={{ notify, dismiss }}>
      {children}
      <Notification
        message={state.message}
        onClose={dismiss}
        show={state.show}
        title={state.title}
        variant={state.variant}
      />
    </NotificationContext.Provider>
  );
}

export function useNotification(): NotificationContextValue {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error(
      "useNotification must be used within a NotificationProvider"
    );
  }
  return context;
}
```

**Step 3: Create index.ts barrel**

```typescript
// libs/components/src/lib/notifications/index.ts
export { Notification, type NotificationProps, type NotificationVariant } from "./Notification";
export { NotificationProvider, useNotification } from "./NotificationProvider";
```

**Step 4: Export from main index**

Add to `libs/components/src/lib/index.ts`:

```typescript
export * from "./notifications";
```

**Step 5: Commit**

```bash
git add libs/components/src/lib/notifications/
git add libs/components/src/lib/index.ts
git commit -m "feat: add Notification component with provider and hook"
bd close ArcataAI-0cn.3 --reason "Created Notification, NotificationProvider, useNotification hook"
```

---

## Task 4: Create JobDetailPanel Side Panel (ArcataAI-0cn.4)

**Files:**
- Create: `apps/hq/src/routes/jobs/JobDetailPanel.tsx`
- Modify: `apps/hq/src/routes/jobs/index.ts`

**Step 1: Create JobDetailPanel.tsx**

```typescript
// apps/hq/src/routes/jobs/JobDetailPanel.tsx
import { Dialog, DialogPanel, DialogTitle, Transition } from "@headlessui/react";
import { XMarkIcon, ArrowTopRightOnSquareIcon } from "@heroicons/react/24/outline";
import { Fragment } from "react";
import { t } from "@arcata/translate";

export interface JobDetailData {
  title: string;
  company: string;
  url: string;
}

export interface JobDetailPanelProps {
  isOpen: boolean;
  onClose: () => void;
  jobId: number | null;
  jobData?: JobDetailData;
}

export function JobDetailPanel({
  isOpen,
  onClose,
  jobId,
  jobData,
}: JobDetailPanelProps) {
  return (
    <Transition as={Fragment} show={isOpen}>
      <Dialog as="div" className="relative z-50" onClose={onClose}>
        <Transition.Child
          as={Fragment}
          enter="ease-in-out duration-500"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in-out duration-500"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-gray-500/75 transition-opacity" />
        </Transition.Child>

        <div className="fixed inset-0 overflow-hidden">
          <div className="absolute inset-0 overflow-hidden">
            <div className="pointer-events-none fixed inset-y-0 right-0 flex max-w-full pl-10 sm:pl-16">
              <Transition.Child
                as={Fragment}
                enter="transform transition ease-in-out duration-500 sm:duration-700"
                enterFrom="translate-x-full"
                enterTo="translate-x-0"
                leave="transform transition ease-in-out duration-500 sm:duration-700"
                leaveFrom="translate-x-0"
                leaveTo="translate-x-full"
              >
                <DialogPanel className="pointer-events-auto w-screen max-w-2xl">
                  <div className="flex h-full flex-col overflow-y-auto bg-white shadow-xl">
                    {/* Header */}
                    <div className="bg-gray-50 px-4 py-6 sm:px-6">
                      <div className="flex items-start justify-between space-x-3">
                        <div className="space-y-1">
                          <DialogTitle className="text-lg font-semibold text-gray-900">
                            {jobData?.title || t("pages.hq.jobDetail.title")}
                          </DialogTitle>
                          {jobData?.company ? (
                            <p className="text-sm text-gray-500">
                              {jobData.company}
                            </p>
                          ) : null}
                        </div>
                        <div className="flex h-7 items-center">
                          <button
                            className="relative rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                            onClick={onClose}
                            type="button"
                          >
                            <span className="sr-only">Close panel</span>
                            <XMarkIcon aria-hidden="true" className="size-6" />
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* Content */}
                    <div className="flex-1 px-4 py-6 sm:px-6">
                      {/* Original posting link */}
                      {jobData?.url ? (
                        <div className="mb-6">
                          <a
                            className="inline-flex items-center gap-2 text-sm font-medium text-indigo-600 hover:text-indigo-500"
                            href={jobData.url}
                            rel="noopener noreferrer"
                            target="_blank"
                          >
                            <ArrowTopRightOnSquareIcon className="size-4" />
                            {t("pages.hq.jobDetail.viewPosting")}
                          </a>
                        </div>
                      ) : null}

                      {/* Placeholder sections */}
                      <div className="space-y-6">
                        <section>
                          <h3 className="text-sm font-medium text-gray-900">
                            Job Description
                          </h3>
                          <p className="mt-2 text-sm text-gray-500">
                            {t("pages.hq.jobDetail.placeholder.description")}
                          </p>
                        </section>

                        <section>
                          <h3 className="text-sm font-medium text-gray-900">
                            Requirements
                          </h3>
                          <p className="mt-2 text-sm text-gray-500">
                            {t("pages.hq.jobDetail.placeholder.requirements")}
                          </p>
                        </section>

                        <section>
                          <h3 className="text-sm font-medium text-gray-900">
                            Match Score
                          </h3>
                          <p className="mt-2 text-sm text-gray-500">
                            {t("pages.hq.jobDetail.placeholder.matchScore")}
                          </p>
                        </section>

                        <section>
                          <h3 className="text-sm font-medium text-gray-900">
                            Notes
                          </h3>
                          <p className="mt-2 text-sm text-gray-500">
                            {t("pages.hq.jobDetail.placeholder.notes")}
                          </p>
                        </section>
                      </div>

                      {/* Debug info */}
                      {jobId ? (
                        <div className="mt-8 rounded-md bg-gray-100 p-4">
                          <p className="text-xs text-gray-500">
                            Job ID: {jobId}
                          </p>
                        </div>
                      ) : null}
                    </div>

                    {/* Footer */}
                    <div className="shrink-0 border-t border-gray-200 px-4 py-5 sm:px-6">
                      <div className="flex justify-end space-x-3">
                        <button
                          className="rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                          onClick={onClose}
                          type="button"
                        >
                          Close
                        </button>
                        <button
                          className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                          disabled
                          type="button"
                        >
                          Track Job
                        </button>
                      </div>
                    </div>
                  </div>
                </DialogPanel>
              </Transition.Child>
            </div>
          </div>
        </div>
      </Dialog>
    </Transition>
  );
}
```

**Step 2: Update jobs/index.ts export**

```typescript
// apps/hq/src/routes/jobs/index.ts
export { route as addJobByUrlRoute, AddJobPopover } from "./AddJobByUrl";
export { JobDetailPanel, type JobDetailData, type JobDetailPanelProps } from "./JobDetailPanel";
```

**Step 3: Commit**

```bash
git add apps/hq/src/routes/jobs/JobDetailPanel.tsx
git add apps/hq/src/routes/jobs/index.ts
git commit -m "feat: add JobDetailPanel slide-over component (stubbed)"
bd close ArcataAI-0cn.4 --reason "Created stubbed JobDetailPanel with placeholder content"
```

---

## Task 5: Update AddJobByUrl Action (ArcataAI-0cn.5)

**Files:**
- Modify: `apps/hq/src/routes/jobs/AddJobByUrl.tsx`

**Step 1: Update the action function**

Replace the existing action function:

```typescript
import { getSession } from "@arcata/db";
import { ingestJob, type IngestJobResponse } from "../../lib/api";

export interface AddJobActionData {
  success: boolean;
  error: string | null;
  jobId?: number;
  streamId?: number;
  message?: string;
}

export async function action({ request }: ActionFunctionArgs): Promise<AddJobActionData> {
  const formData = await request.formData();
  const updates = Object.fromEntries(formData);
  
  // Validate URL
  const { error: validationError } = schema.validate(updates);
  if (validationError?.details[0]?.message) {
    return { success: false, error: validationError.details[0].message };
  }

  // Get auth session
  const session = await getSession();
  if (!session?.access_token) {
    return { success: false, error: "Not authenticated. Please log in again." };
  }

  // Call the API
  const result = await ingestJob(
    { 
      url: updates.jobUrl as string,
      source: "manual",
    },
    session.access_token
  );

  if (result.ok) {
    return {
      success: true,
      error: null,
      jobId: result.data.jobId,
      streamId: result.data.streamId,
      message: result.data.message,
    };
  }

  return {
    success: false,
    error: result.error.error,
  };
}
```

**Step 2: Commit**

```bash
git add apps/hq/src/routes/jobs/AddJobByUrl.tsx
git commit -m "feat: connect AddJobByUrl action to Scala API"
bd close ArcataAI-0cn.5 --reason "Action now calls ingestJob API with auth token"
```

---

## Task 6: Update AddJobPopover UI (ArcataAI-0cn.6)

**Files:**
- Modify: `apps/hq/src/routes/jobs/AddJobByUrl.tsx`

**Step 1: Update imports and add state**

```typescript
import { useNotification } from "@arcata/components";
import { useEffect, useState } from "react";
import { useRevalidator } from "react-router-dom";
import { JobDetailPanel, type JobDetailData } from "./JobDetailPanel";
import type { AddJobActionData } from "./AddJobByUrl"; // from same file
```

**Step 2: Update PanelContent to show loading state**

```typescript
function PanelContent({
  close,
  success,
  error,
  nav,
  fetcher,
}: PanelContentProps) {
  const isSubmitting = fetcher.state === "submitting" || fetcher.state === "loading";

  useEffect(() => {
    if (success) {
      close();
    }
  }, [success, close]);

  return (
    <div className="w-screen max-w-sm flex-auto rounded-3xl bg-white p-4 text-sm leading-6 shadow-lg ring-1 ring-gray-900/5">
      <fetcher.Form
        action={path}
        className="space-y-6"
        id="add-job-url-submit"
        method="POST"
      >
        <div>
          <label
            className="block font-medium text-gray-900 text-sm leading-6"
            htmlFor="jobUrl"
          >
            {t("pages.hq.inputs.jobUrl.label")}
          </label>
          <div className="mt-2 h-14">
            <input
              className="block w-full rounded-md border-0 px-2 py-1.5 text-gray-900 shadow-sm ring-1 ring-gray-900 ring-inset placeholder:text-gray-400 focus:ring-2 focus:ring-gray-900 focus:ring-inset disabled:bg-gray-100 disabled:cursor-not-allowed sm:text-sm sm:leading-6"
              disabled={isSubmitting}
              id="job-url-input"
              name="jobUrl"
              placeholder="https://boards.greenhouse.io/company/jobs/12345"
              required
              type="text"
            />
            {error ? (
              <p className="mt-2 text-red-600 text-sm" id="job-url-error">
                {error}
              </p>
            ) : null}
          </div>
        </div>
        <div className="flex justify-end">
          <div>
            <AppButton
              className="py-1"
              disabled={isSubmitting}
              onClick={() => close()}
              theme="link"
              type="button"
            >
              {t("common.actions.cancel")}
            </AppButton>
          </div>
          <div>
            <AppButton
              buttonState={isSubmitting ? "loading" : nav.state}
              className="py-1"
              disabled={isSubmitting}
              theme="primary_outline"
              type="submit"
            >
              {isSubmitting ? t("pages.hq.notifications.processing") : t("common.actions.add")}
            </AppButton>
          </div>
        </div>
      </fetcher.Form>
    </div>
  );
}
```

**Step 3: Update AddJobPopover component**

```typescript
export function AddJobPopover({ actuator }: AddJobPopoverProps) {
  const nav = useNavigation();
  const fetcher = useFetcher<AddJobActionData>();
  const revalidator = useRevalidator();
  const { notify, dismiss } = useNotification();
  
  // Panel state
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelJobId, setPanelJobId] = useState<number | null>(null);
  const [panelJobData, setPanelJobData] = useState<JobDetailData | undefined>();

  const data = fetcher.data || { success: false, error: null };

  // Handle submission state changes
  useEffect(() => {
    if (fetcher.state === "submitting") {
      notify(
        t("pages.hq.notifications.processing"),
        "loading",
        "This may take a moment..."
      );
    }
  }, [fetcher.state, notify]);

  // Handle response
  useEffect(() => {
    if (fetcher.state === "idle" && fetcher.data) {
      dismiss();
      
      if (fetcher.data.success && fetcher.data.jobId) {
        notify(
          t("pages.hq.notifications.success"),
          "success",
          fetcher.data.message,
          5000
        );
        
        // Revalidate data
        revalidator.revalidate();
        
        // Open job detail panel
        setPanelJobId(fetcher.data.jobId);
        setPanelJobData({
          title: "New Job", // Will be fetched properly later
          company: "",
          url: "",
        });
        setPanelOpen(true);
      } else if (fetcher.data.error) {
        notify(
          t("pages.hq.notifications.error"),
          "error",
          fetcher.data.error,
          8000
        );
      }
    }
  }, [fetcher.state, fetcher.data, notify, dismiss, revalidator]);

  return (
    <>
      <Popover className="relative">
        <PopoverButton
          className="inline-flex items-center gap-x-1 font-semibold text-gray-900 text-sm leading-6"
          theme="brand"
        >
          {actuator}
        </PopoverButton>

        <Transition
          as={Fragment}
          enter="transition ease-out duration-200"
          enterFrom="opacity-0 translate-y-1"
          enterTo="opacity-100 translate-y-0"
          leave="transition ease-in duration-150"
          leaveFrom="opacity-100 translate-y-0"
          leaveTo="opacity-0 translate-y-1"
        >
          <Popover.Panel className="absolute left-3/4 z-10 mt-5 flex w-screen max-w-max -translate-x-full px-4">
            {({ close }) => (
              <PanelContent
                close={close}
                error={data.error}
                fetcher={fetcher}
                nav={nav}
                success={data.success}
              />
            )}
          </Popover.Panel>
        </Transition>
      </Popover>

      <JobDetailPanel
        isOpen={panelOpen}
        jobData={panelJobData}
        jobId={panelJobId}
        onClose={() => {
          setPanelOpen(false);
          setPanelJobId(null);
          setPanelJobData(undefined);
        }}
      />
    </>
  );
}
```

**Step 4: Commit**

```bash
git add apps/hq/src/routes/jobs/AddJobByUrl.tsx
git commit -m "feat: add processing states, notifications, and panel integration"
bd close ArcataAI-0cn.6 --reason "UI now shows loading, success/error notifications, opens panel"
```

---

## Task 7: Integrate NotificationProvider (ArcataAI-0cn.7)

**Files:**
- Modify: `apps/hq/src/main.tsx`

**Step 1: Wrap app with NotificationProvider**

```typescript
// apps/hq/src/main.tsx
import { NotificationProvider } from "@arcata/components";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { route as appRoute } from "./routes/App";
import "./index.css";

const router = createBrowserRouter([appRoute]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <NotificationProvider>
      <RouterProvider router={router} />
    </NotificationProvider>
  </StrictMode>
);
```

**Step 2: Commit**

```bash
git add apps/hq/src/main.tsx
git commit -m "feat: wrap HQ app with NotificationProvider"
bd close ArcataAI-0cn.7 --reason "NotificationProvider integrated at app root"
```

---

## Task 8: Add Translations (ArcataAI-0cn.8)

**Files:**
- Modify: `apps/hq/public/locales/en.json`

**Step 1: Add translation keys**

Add these keys to the `pages.hq` section:

```json
{
  "pages": {
    "hq": {
      "notifications": {
        "processing": "Processing job URL...",
        "success": "Job added successfully",
        "error": "Failed to add job"
      },
      "jobDetail": {
        "title": "Job Details",
        "company": "Company",
        "viewPosting": "View Original Posting",
        "placeholder": {
          "description": "Job description will appear here",
          "requirements": "Requirements will appear here",
          "matchScore": "Match score coming soon",
          "notes": "Add your notes here"
        }
      }
    }
  }
}
```

**Step 2: Commit**

```bash
git add apps/hq/public/locales/en.json
git commit -m "feat: add translations for job submission flow"
bd close ArcataAI-0cn.8 --reason "Added notification and job detail translations"
```

---

## Final: Close Epic

```bash
bd close ArcataAI-0cn --reason "Job URL submission flow complete - UI connected to Scala API"
```

---

## Testing the Flow

1. Start the Scala API: `cd apps/api && make run`
2. Start the HQ app: `cd apps/hq && bun dev`
3. Login to the app
4. Click "Add Job" button
5. Enter a job URL (e.g., from greenhouse, lever, etc.)
6. Observe:
   - Loading notification appears
   - Success notification appears on completion
   - Data revalidates (job stream updates)
   - Job detail panel opens

## Known Limitations (Future Work)

- JobDetailPanel shows placeholder content - full job data fetch TBD
- No retry mechanism for failed API calls
- No offline/network error recovery
- Panel doesn't show actual job data from API response yet
