import { supabaseClient } from "@arcata/db";
import { useEffect, useState } from "react";
import type { JobDetail } from "./types";

type UseJobDetailResult = {
  data: JobDetail | null;
  isLoading: boolean;
  error: Error | null;
};

/**
 * Maps database snake_case row to camelCase JobDetail type.
 */
function mapRowToJobDetail(row: Record<string, unknown>): JobDetail {
  const company = row.companies as Record<string, unknown> | null;

  return {
    jobId: row.job_id as number,
    companyId: row.company_id as number,
    title: row.title as string,
    description: row.description as string | null,
    location: row.location as string | null,
    jobType: row.job_type as string | null,
    category: row.category as string | null,
    experienceLevel: row.experience_level as string | null,
    educationLevel: row.education_level as string | null,
    salaryMin: row.salary_min as number | null,
    salaryMax: row.salary_max as number | null,
    salaryCurrency: row.salary_currency as string | null,
    qualifications: row.qualifications as string[] | null,
    preferredQualifications: row.preferred_qualifications as string[] | null,
    responsibilities: row.responsibilities as string[] | null,
    benefits: row.benefits as string[] | null,
    sourceUrl: row.source_url as string | null,
    applicationUrl: row.application_url as string | null,
    isRemote: row.is_remote as boolean | null,
    postedDate: row.posted_date as string | null,
    closingDate: row.closing_date as string | null,
    companyName: company?.company_name as string | null,
    companyDomain: company?.company_domain as string | null,
  };
}

/**
 * Hook to fetch job details by ID from Supabase.
 *
 * @param jobId - The job ID to fetch, or null to skip fetching
 * @returns Object with data, isLoading, and error states
 *
 * @example
 * const { data: job, isLoading, error } = useJobDetail(jobId);
 */
export function useJobDetail(jobId: number | null): UseJobDetailResult {
  const [data, setData] = useState<JobDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (jobId === null) {
      setData(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    let cancelled = false;

    async function fetchJob() {
      setIsLoading(true);
      setError(null);

      const { data: row, error: fetchError } = await supabaseClient
        .from("jobs")
        .select(
          `
          job_id,
          company_id,
          title,
          description,
          location,
          job_type,
          category,
          experience_level,
          education_level,
          salary_min,
          salary_max,
          salary_currency,
          qualifications,
          preferred_qualifications,
          responsibilities,
          benefits,
          source_url,
          application_url,
          is_remote,
          posted_date,
          closing_date,
          companies (
            company_name,
            company_domain
          )
        `
        )
        .eq("job_id", jobId)
        .single();

      if (cancelled) {
        return;
      }

      if (fetchError) {
        setError(new Error(fetchError.message));
        setData(null);
      } else if (row) {
        setData(mapRowToJobDetail(row));
      } else {
        setData(null);
      }

      setIsLoading(false);
    }

    fetchJob();

    return () => {
      cancelled = true;
    };
  }, [jobId]);

  return { data, isLoading, error };
}
