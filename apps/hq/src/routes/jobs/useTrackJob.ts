import { getCurrentUser, supabaseClient } from "@arcata/db";
import { useState } from "react";

type UseTrackJobResult = {
  trackJob: (jobId: number, streamId: number) => Promise<void>;
  isTracking: boolean;
};

/**
 * Hook to track a job (add to applications board).
 *
 * @returns Object with trackJob function and isTracking state
 *
 * @example
 * const { trackJob, isTracking } = useTrackJob();
 * await trackJob(jobId, streamId);
 */
export function useTrackJob(): UseTrackJobResult {
  const [isTracking, setIsTracking] = useState(false);

  async function trackJob(jobId: number, streamId: number): Promise<void> {
    setIsTracking(true);

    try {
      // Get current user
      const user = await getCurrentUser();
      if (!user) {
        throw new Error("User not authenticated");
      }
      const profileId = user.id;

      // Get the first status (ordered by column_order)
      const { data: statuses, error: statusError } = await supabaseClient
        .from("application_statuses")
        .select("status_id")
        .eq("profile_id", profileId)
        .order("column_order", { ascending: true })
        .limit(1);

      if (statusError) {
        throw new Error(`Failed to fetch statuses: ${statusError.message}`);
      }

      if (!statuses || statuses.length === 0) {
        throw new Error(
          "No application statuses found. Please set up your board first."
        );
      }

      const statusId = statuses[0].status_id;

      // Create job application
      const { data: application, error: appError } = await supabaseClient
        .from("job_applications")
        .insert({
          job_id: jobId,
          profile_id: profileId,
          status_id: statusId,
          status_order: 0,
        })
        .select("application_id")
        .single();

      if (appError) {
        throw new Error(`Failed to create application: ${appError.message}`);
      }

      if (!application) {
        throw new Error("Failed to create application: no data returned");
      }

      // Update job_stream with the application_id
      const { error: streamError } = await supabaseClient
        .from("job_stream")
        .update({ application_id: application.application_id })
        .eq("stream_id", streamId);

      if (streamError) {
        throw new Error(`Failed to update stream: ${streamError.message}`);
      }
    } finally {
      setIsTracking(false);
    }
  }

  return { trackJob, isTracking };
}
