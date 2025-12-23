export type IngestJobRequest = {
  url: string;
  source?: string;
  createApplication?: boolean;
  notes?: string;
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
