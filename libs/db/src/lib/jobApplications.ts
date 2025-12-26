import supabaseClient from "./client";
import type { JobApplicationWithJob } from "./resourceTypes";

/**
 * Fetch job applications with joined job and company data
 */
export async function listApplicationsWithJobs(
  profileId: string
): Promise<JobApplicationWithJob[]> {
  const { data, error } = await supabaseClient
    .from("job_applications")
    .select(`
      *,
      jobs (
        title,
        experience_level,
        job_type,
        location,
        companies (
          company_name
        )
      )
    `)
    .eq("profile_id", profileId)
    .order("status_order", { ascending: true });

  if (error) {
    throw error;
  }
  return data as JobApplicationWithJob[];
}
