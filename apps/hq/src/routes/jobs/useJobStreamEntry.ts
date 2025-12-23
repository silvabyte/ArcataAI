import { supabaseClient } from "@arcata/db";
import { useCallback, useEffect, useState } from "react";
import type { JobStreamEntryWithTracking } from "./types";

type UseJobStreamEntryResult = {
  data: JobStreamEntryWithTracking | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
};

/**
 * Hook to fetch a job stream entry by stream ID.
 * Returns tracking state (application_id) along with job reference.
 *
 * @param streamId - The stream ID to fetch, or null to skip fetching
 * @returns Object with data, isLoading, error, and refetch function
 *
 * @example
 * const { data: entry, isLoading } = useJobStreamEntry(streamId);
 * const isTracked = entry?.applicationId != null;
 */
export function useJobStreamEntry(
  streamId: number | null
): UseJobStreamEntryResult {
  const [data, setData] = useState<JobStreamEntryWithTracking | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchEntry = useCallback(async () => {
    if (streamId === null) {
      setData(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    const { data: row, error: fetchError } = await supabaseClient
      .from("job_stream")
      .select("stream_id, job_id, application_id, best_match_score, status")
      .eq("stream_id", streamId)
      .single();

    if (fetchError) {
      setError(new Error(fetchError.message));
      setData(null);
    } else if (row) {
      setData({
        streamId: row.stream_id,
        jobId: row.job_id,
        applicationId: row.application_id,
        bestMatchScore: row.best_match_score,
        status: row.status,
      });
    } else {
      setData(null);
    }

    setIsLoading(false);
  }, [streamId]);

  useEffect(() => {
    fetchEntry();
  }, [fetchEntry]);

  return { data, isLoading, error, refetch: fetchEntry };
}
