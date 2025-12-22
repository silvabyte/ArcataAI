import client from "./client";

/**
 * Filter parameters for querying the job stream
 */
export type JobStreamFilters = {
  search?: string; // ilike on jobs.title
  locations?: string[]; // in() on jobs.location
  remote?: boolean; // eq on jobs.is_remote
  experience?: string[]; // in() on jobs.experience_level
  education?: string[]; // in() on jobs.education_level
  jobTypes?: string[]; // in() on jobs.job_type
  organizationIds?: number[]; // in() on jobs.company_id
  skills?: string[]; // overlaps on jobs.qualifications array
  sort?: "relevance" | "date"; // order by best_match_score or jobs.posted_date
};

/**
 * Company data from nested select
 */
export type JobStreamCompany = {
  company_id: number;
  company_name: string | null;
  company_domain: string | null;
};

/**
 * Job data from nested select
 * Note: Types defined explicitly to support new schema columns before DB types are regenerated
 */
export type JobStreamJob = {
  job_id: number;
  title: string;
  description: string | null;
  location: string | null;
  is_remote: boolean;
  job_type: string | null;
  experience_level: string | null;
  education_level: string | null;
  salary_min: number | null;
  salary_max: number | null;
  salary_currency: string | null;
  posted_date: string | null;
  application_url: string | null;
  companies: JobStreamCompany | null;
};

/**
 * Job stream entry with nested job and company data
 */
export type JobStreamEntry = {
  stream_id: number;
  best_match_score: number | null;
  status: string | null;
  jobs: JobStreamJob | null;
};

/**
 * Result type for listJobStream
 */
export type JobStreamResult = {
  data: JobStreamEntry[] | null;
  error: Error | null;
};

/**
 * Result type for listAllJobs (fallback when no profiles configured)
 */
export type JobListResult = {
  data: JobStreamJob[] | null;
  error: Error | null;
};

/**
 * Query job_stream table with joins to jobs and companies.
 * Applies dynamic filters based on the provided filter parameters.
 *
 * @param filters - Optional filters to apply to the query
 * @returns Promise with job stream entries or error
 */
// biome-ignore lint/complexity/noExcessiveCognitiveComplexity: Linear filter application is readable despite metric
export async function listJobStream(
  filters: JobStreamFilters = {}
): Promise<JobStreamResult> {
  // Start building the query with nested select for joins
  let query = client.from("job_stream").select(`
    stream_id,
    best_match_score,
    status,
    jobs (
      job_id,
      title,
      description,
      location,
      is_remote,
      job_type,
      experience_level,
      education_level,
      salary_min,
      salary_max,
      salary_currency,
      posted_date,
      application_url,
      companies (
        company_id,
        company_name,
        company_domain
      )
    )
  `);

  // Apply search filter (ilike on jobs.title)
  if (filters.search) {
    query = query.ilike("jobs.title", `%${filters.search}%`);
  }

  // Apply location filter (in() on jobs.location)
  if (filters.locations && filters.locations.length > 0) {
    query = query.in("jobs.location", filters.locations);
  }

  // Apply remote filter (eq on jobs.is_remote)
  if (filters.remote === true) {
    query = query.eq("jobs.is_remote", true);
  }

  // Apply experience level filter (in() on jobs.experience_level)
  if (filters.experience && filters.experience.length > 0) {
    query = query.in("jobs.experience_level", filters.experience);
  }

  // Apply education level filter (in() on jobs.education_level)
  if (filters.education && filters.education.length > 0) {
    query = query.in("jobs.education_level", filters.education);
  }

  // Apply job type filter (in() on jobs.job_type)
  if (filters.jobTypes && filters.jobTypes.length > 0) {
    query = query.in("jobs.job_type", filters.jobTypes);
  }

  // Apply organization/company filter (in() on jobs.company_id)
  if (filters.organizationIds && filters.organizationIds.length > 0) {
    query = query.in("jobs.company_id", filters.organizationIds);
  }

  // Apply skills filter (overlaps on jobs.qualifications array)
  if (filters.skills && filters.skills.length > 0) {
    query = query.overlaps("jobs.qualifications", filters.skills);
  }

  // Apply sorting
  if (filters.sort === "date") {
    query = query.order("jobs(posted_date)", { ascending: false });
  } else {
    // Default to relevance (best_match_score)
    query = query.order("best_match_score", {
      ascending: false,
      nullsFirst: false,
    });
  }

  const { data, error } = await query;

  return {
    data: data as JobStreamEntry[] | null,
    error: error as Error | null,
  };
}

/**
 * Query jobs table directly (fallback when user has no profiles/job_stream entries).
 * Returns jobs in a format compatible with JobStreamEntry for easy UI consumption.
 *
 * @param filters - Optional filters to apply to the query
 * @returns Promise with jobs or error
 */
export async function listAllJobs(
  filters: JobStreamFilters = {}
): Promise<JobStreamResult> {
  // Query jobs directly with company join
  let query = client.from("jobs").select(`
    job_id,
    title,
    description,
    location,
    is_remote,
    job_type,
    experience_level,
    education_level,
    salary_min,
    salary_max,
    salary_currency,
    posted_date,
    application_url,
    companies (
      company_id,
      company_name,
      company_domain
    )
  `);

  // Apply search filter
  if (filters.search) {
    query = query.ilike("title", `%${filters.search}%`);
  }

  // Apply location filter
  if (filters.locations && filters.locations.length > 0) {
    query = query.in("location", filters.locations);
  }

  // Apply remote filter
  if (filters.remote === true) {
    query = query.eq("is_remote", true);
  }

  // Apply experience level filter
  if (filters.experience && filters.experience.length > 0) {
    query = query.in("experience_level", filters.experience);
  }

  // Apply education level filter
  if (filters.education && filters.education.length > 0) {
    query = query.in("education_level", filters.education);
  }

  // Apply job type filter
  if (filters.jobTypes && filters.jobTypes.length > 0) {
    query = query.in("job_type", filters.jobTypes);
  }

  // Apply organization/company filter
  if (filters.organizationIds && filters.organizationIds.length > 0) {
    query = query.in("company_id", filters.organizationIds);
  }

  // Apply skills filter (overlaps on qualifications array)
  if (filters.skills && filters.skills.length > 0) {
    query = query.overlaps("qualifications", filters.skills);
  }

  // Apply sorting (only date makes sense without match scores)
  query = query.order("posted_date", { ascending: false, nullsFirst: false });

  const { data, error } = await query;

  // Transform to JobStreamEntry format for UI compatibility
  const transformed: JobStreamEntry[] | null = data
    ? data.map((job) => ({
        stream_id: job.job_id, // Use job_id as pseudo stream_id
        best_match_score: null, // No score when querying all jobs
        status: "new" as const,
        jobs: {
          job_id: job.job_id,
          title: job.title,
          description: job.description,
          location: job.location,
          is_remote: job.is_remote,
          job_type: job.job_type,
          experience_level: job.experience_level,
          education_level: job.education_level,
          salary_min: job.salary_min,
          salary_max: job.salary_max,
          salary_currency: job.salary_currency,
          posted_date: job.posted_date,
          application_url: job.application_url,
          // Supabase returns single object for 1:1 FK, but typed as array; extract first
          companies: Array.isArray(job.companies)
            ? (job.companies[0] ?? null)
            : job.companies,
        },
      }))
    : null;

  return {
    data: transformed,
    error: error as Error | null,
  };
}

/**
 * Filter option for combobox/select components
 */
export type FilterOption = {
  id: string | number;
  label: string;
};

/**
 * All filter options needed for the JobStreamFilterPanel
 */
export type JobFilterOptions = {
  locations: FilterOption[];
  companies: FilterOption[];
  skills: FilterOption[];
};

/**
 * Fetch unique locations from jobs table for filter dropdown.
 * Returns distinct non-null locations sorted alphabetically.
 */
export async function getLocationOptions(): Promise<FilterOption[]> {
  const { data, error } = await client
    .from("jobs")
    .select("location")
    .not("location", "is", null)
    .order("location");

  if (error || !data) {
    return [];
  }

  // Deduplicate and format
  const uniqueLocations = [...new Set(data.map((row) => row.location))].filter(
    (loc): loc is string => loc !== null
  );

  return uniqueLocations.map((loc) => ({
    id: loc,
    label: loc,
  }));
}

/**
 * Fetch companies from companies table for filter dropdown.
 * Returns companies with jobs, sorted by name.
 */
export async function getCompanyOptions(): Promise<FilterOption[]> {
  // Get companies that have at least one job
  const { data, error } = await client
    .from("companies")
    .select("company_id, company_name")
    .not("company_name", "is", null)
    .order("company_name");

  if (error || !data) {
    return [];
  }

  return data
    .filter((company) => company.company_name !== null)
    .map((company) => ({
      id: company.company_id,
      label: company.company_name as string,
    }));
}

/**
 * Fetch unique skills from job qualifications for filter dropdown.
 * Extracts skills from the qualifications array field across all jobs.
 */
export async function getSkillOptions(): Promise<FilterOption[]> {
  const { data, error } = await client
    .from("jobs")
    .select("qualifications")
    .not("qualifications", "is", null);

  if (error || !data) {
    return [];
  }

  // Flatten all qualifications arrays and deduplicate
  const allSkills = data.flatMap((row) => row.qualifications ?? []);
  const uniqueSkills = [...new Set(allSkills)]
    .filter(
      (skill): skill is string => typeof skill === "string" && skill.length > 0
    )
    .sort();

  return uniqueSkills.map((skill) => ({
    id: skill,
    label: skill,
  }));
}

/**
 * Fetch all filter options in parallel for efficiency.
 */
export async function getFilterOptions(): Promise<JobFilterOptions> {
  const [locations, companies, skills] = await Promise.all([
    getLocationOptions(),
    getCompanyOptions(),
    getSkillOptions(),
  ]);

  return { locations, companies, skills };
}
