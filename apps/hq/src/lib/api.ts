export type IngestJobRequest = {
  url: string;
  source?: string;
  createApplication?: boolean;
  notes?: string;
};

export type IngestionStatus =
  | "checking"
  | "fetching"
  | "parsing"
  | "resolving"
  | "loading"
  | "streaming"
  | "tracking"
  | "complete"
  | "error";

export type ProgressUpdate = {
  step: number;
  totalSteps: number;
  status: IngestionStatus;
  message: string;
  jobId?: number;
  streamId?: number;
  applicationId?: number;
  error?: string;
};

export type IngestJobResponse = {
  success: boolean;
  jobId: number;
  streamId?: number;
  applicationId?: number;
  message: string;
};

export type JobErrorResponse = {
  success: boolean;
  error: string;
  details?: string;
};

export type IngestJobResult =
  | { ok: true; data: IngestJobResponse }
  | { ok: false; error: JobErrorResponse };

/**
 * Get the API base URL from environment.
 * Defaults to localhost:4203 for development.
 */
function getApiUrl(): string {
  return import.meta.env.VITE_API_URL || "http://localhost:4203";
}

/**
 * Ingest a job from a URL via the Scala ETL API.
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
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(request),
    });

    const data = await response.json();

    if (response.ok && data.success) {
      return { ok: true, data: data as IngestJobResponse };
    }

    return {
      ok: false,
      error: data as JobErrorResponse,
    };
  } catch (error) {
    return {
      ok: false,
      error: {
        success: false,
        error: "Network error",
        details:
          error instanceof Error ? error.message : "Failed to connect to API",
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

const STEP_MESSAGES: Record<IngestionStatus, string> = {
  checking: "Looking up job...",
  fetching: "Getting job page...",
  parsing: "Extracting job details...",
  resolving: "Finding company...",
  loading: "Creating job record...",
  streaming: "Adding to your feed...",
  tracking: "Creating application...",
  complete: "Job added!",
  error: "Failed to add job",
};

const STEP_ORDER: IngestionStatus[] = [
  "checking",
  "fetching",
  "parsing",
  "resolving",
  "loading",
  "streaming",
  "tracking",
];

/**
 * Ingest a job with simulated progress updates.
 *
 * Since the backend is REST, we simulate progress while waiting for the response.
 * The actual step timing on the backend is opaque to us.
 */
export async function ingestJobWithProgress(
  request: IngestJobRequest,
  token: string,
  onProgress: (update: ProgressUpdate) => void
): Promise<IngestJobResult> {
  const totalSteps = request.createApplication ? 7 : 6;
  let currentStep = 0;
  let completed = false;

  // Emit progress updates every 2 seconds while waiting
  const progressInterval = setInterval(() => {
    if (completed || currentStep >= totalSteps - 1) {
      return;
    }

    currentStep += 1;
    const status = STEP_ORDER[currentStep] || "loading";
    onProgress({
      step: currentStep,
      totalSteps,
      status,
      message: STEP_MESSAGES[status],
    });
  }, 2000);

  // Emit initial progress
  onProgress({
    step: 0,
    totalSteps,
    status: "checking",
    message: STEP_MESSAGES.checking,
  });

  try {
    const result = await ingestJob(request, token);
    completed = true;
    clearInterval(progressInterval);

    if (result.ok) {
      onProgress({
        step: totalSteps,
        totalSteps,
        status: "complete",
        message: STEP_MESSAGES.complete,
        jobId: result.data.jobId,
        streamId: result.data.streamId,
        applicationId: result.data.applicationId,
      });
    } else {
      onProgress({
        step: currentStep,
        totalSteps,
        status: "error",
        message: STEP_MESSAGES.error,
        error: result.error.error,
      });
    }

    return result;
  } catch (error) {
    completed = true;
    clearInterval(progressInterval);

    const errorResult: IngestJobResult = {
      ok: false,
      error: {
        success: false,
        error: "Network error",
        details: error instanceof Error ? error.message : "Unknown error",
      },
    };

    onProgress({
      step: currentStep,
      totalSteps,
      status: "error",
      message: STEP_MESSAGES.error,
      error: errorResult.error.error,
    });

    return errorResult;
  }
}
