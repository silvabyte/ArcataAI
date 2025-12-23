import { BottomSheet, useNotification } from "@arcata/components";
import {
  type JobStreamFilters as DbJobStreamFilters,
  getFilterOptions,
  type JobFilterOptions,
  type JobStreamEntry,
  listAllJobs,
  listJobStream,
} from "@arcata/db";
import { t } from "@arcata/translate";
import { useCallback, useState } from "react";
import {
  type LoaderFunctionArgs,
  useLoaderData,
  useSearchParams,
} from "react-router-dom";
import { useTrackJob } from "../jobs/useTrackJob";
import {
  type DegreeLevel,
  type ExperienceLevel,
  getDefaultFilters,
  hasActiveFilters,
  type JobStreamFilters,
  type JobType,
  type SortOption,
} from "./filterTypes";
import { JobCard } from "./JobCard/JobCard";
import { JobStreamFilterPanel } from "./JobStreamFilterPanel";
import { JobStreamHeader } from "./JobStreamHeader";

/**
 * Parse URL search params into JobStreamFilters format
 */
function parseUrlFilters(searchParams: URLSearchParams): JobStreamFilters {
  const defaults = getDefaultFilters();

  // Parse search string
  const search = searchParams.get("search") ?? defaults.search;

  // Parse array params using bracket notation (e.g., locations[]=sf&locations[]=nyc)
  const locations = searchParams.getAll("locations[]");
  const experience = searchParams.getAll("experience[]") as ExperienceLevel[];
  const skills = searchParams.getAll("skills[]");
  const degree = searchParams.getAll("degree[]") as DegreeLevel[];
  const jobTypes = searchParams.getAll("jobTypes[]") as JobType[];
  const organizationIds = searchParams
    .getAll("organizationIds[]")
    .map(Number)
    .filter((id) => !Number.isNaN(id));

  // Parse remote boolean
  const remoteParam = searchParams.get("remote");
  const remote = remoteParam === "true";

  // Parse sort option
  const sortParam = searchParams.get("sort");
  const sortBy: SortOption =
    sortParam === "date" || sortParam === "relevance" ? sortParam : "relevance";

  return {
    search,
    locations: locations.length > 0 ? locations : defaults.locations,
    remote,
    experience: experience.length > 0 ? experience : defaults.experience,
    skills: skills.length > 0 ? skills : defaults.skills,
    degree: degree.length > 0 ? degree : defaults.degree,
    jobTypes: jobTypes.length > 0 ? jobTypes : defaults.jobTypes,
    organizationIds:
      organizationIds.length > 0 ? organizationIds : defaults.organizationIds,
    sortBy,
  };
}

/**
 * Serialize JobStreamFilters to URLSearchParams
 */
function serializeFilters(filters: JobStreamFilters): URLSearchParams {
  const params = new URLSearchParams();

  // Add search if not empty
  if (filters.search) {
    params.set("search", filters.search);
  }

  // Add remote if true
  if (filters.remote) {
    params.set("remote", "true");
  }

  // Add array params using bracket notation
  for (const loc of filters.locations) {
    params.append("locations[]", loc);
  }

  for (const exp of filters.experience) {
    params.append("experience[]", exp);
  }

  for (const skill of filters.skills) {
    params.append("skills[]", skill);
  }

  for (const deg of filters.degree) {
    params.append("degree[]", deg);
  }

  for (const jt of filters.jobTypes) {
    params.append("jobTypes[]", jt);
  }

  for (const orgId of filters.organizationIds) {
    params.append("organizationIds[]", String(orgId));
  }

  // Add sort if not default
  if (filters.sortBy !== "relevance") {
    params.set("sort", filters.sortBy);
  }

  return params;
}

/**
 * Convert UI filters to DB filters format
 */
function toDbFilters(filters: JobStreamFilters): DbJobStreamFilters {
  return {
    search: filters.search || undefined,
    locations: filters.locations.length > 0 ? filters.locations : undefined,
    remote: filters.remote || undefined,
    experience: filters.experience.length > 0 ? filters.experience : undefined,
    education: filters.degree.length > 0 ? filters.degree : undefined,
    jobTypes: filters.jobTypes.length > 0 ? filters.jobTypes : undefined,
    organizationIds:
      filters.organizationIds.length > 0 ? filters.organizationIds : undefined,
    skills: filters.skills.length > 0 ? filters.skills : undefined,
    sort: filters.sortBy,
  };
}

type LoaderData = {
  filters: JobStreamFilters;
  jobs: JobStreamEntry[];
  filterOptions: JobFilterOptions;
  error: Error | null;
};

/**
 * Loader function for JobStreamPage
 * Parses URL params and fetches job stream data.
 * Falls back to all jobs if user has no job_stream entries (no profiles configured).
 */
export async function loader({
  request,
}: LoaderFunctionArgs): Promise<LoaderData> {
  const url = new URL(request.url);
  const filters = parseUrlFilters(url.searchParams);
  const dbFilters = toDbFilters(filters);

  // Fetch filter options and jobs in parallel
  const [filterOptions, jobStreamResult] = await Promise.all([
    getFilterOptions(),
    listJobStream(dbFilters),
  ]);

  const { data, error } = jobStreamResult;

  // If job_stream is empty (no profiles configured), fall back to all jobs
  if (!error && (!data || data.length === 0)) {
    const fallback = await listAllJobs(dbFilters);
    return {
      filters,
      jobs: fallback.data ?? [],
      filterOptions,
      error: fallback.error,
    };
  }

  return {
    filters,
    jobs: data ?? [],
    filterOptions,
    error,
  };
}

function getJobId(job: JobStreamEntry): number {
  return job.jobs?.job_id ?? job.stream_id;
}

type JobStreamContentProps = {
  error: Error | null;
  jobs: JobStreamEntry[];
  onTrack: (jobId: number, streamId: number) => void;
};

function JobStreamContent({ error, jobs, onTrack }: JobStreamContentProps) {
  if (error !== null) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <div className="rounded-lg bg-red-50 p-6">
          <h3 className="font-medium text-lg text-red-800">
            {t("pages.jobStream.error.title")}
          </h3>
          <p className="mt-2 text-red-600 text-sm">
            {t("pages.jobStream.error.description")}
          </p>
        </div>
      </div>
    );
  }

  if (jobs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <h3 className="font-medium text-gray-900 text-lg">
          {t("pages.jobStream.empty.title")}
        </h3>
        <p className="mt-2 text-gray-500 text-sm">
          {t("pages.jobStream.empty.description")}
        </p>
      </div>
    );
  }

  return (
    <>
      {jobs.map((job) => (
        <JobCard job={job} key={getJobId(job)} onTrack={onTrack} />
      ))}
    </>
  );
}

export function JobStreamPage() {
  const { jobs, error, filterOptions } = useLoaderData() as LoaderData;
  const [searchParams, setSearchParams] = useSearchParams();
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const { trackJob, isTracking } = useTrackJob();
  const { notify } = useNotification();

  // Derive current filters from URL params
  const filters = parseUrlFilters(searchParams);

  const handleTrack = useCallback(
    async (jobId: number, streamId: number) => {
      if (isTracking) {
        return;
      }

      try {
        await trackJob(jobId, streamId);
        notify(t("pages.hq.jobDetail.toast.trackSuccess"), "success");
      } catch (err) {
        const message =
          err instanceof Error ? err.message : t("common.errors.generic");
        notify(t("pages.hq.jobDetail.toast.trackError"), "error", message);
      }
    },
    [isTracking, notify, trackJob]
  );

  const handleFiltersChange = useCallback(
    (newFilters: JobStreamFilters) => {
      const newParams = serializeFilters(newFilters);
      setSearchParams(newParams, { replace: true });
    },
    [setSearchParams]
  );

  const handleClearFilters = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true });
  }, [setSearchParams]);

  const jobCount = jobs.length;
  const filtersActive = hasActiveFilters(filters);

  return (
    <>
      <div className="flex h-full">
        {/* Filter Panel - hidden on mobile */}
        <div className="hidden lg:block">
          <JobStreamFilterPanel
            filterOptions={filterOptions}
            filters={filters}
            hasActiveFilters={filtersActive}
            jobCount={jobCount}
            onClearFilters={handleClearFilters}
            onFiltersChange={handleFiltersChange}
          />
        </div>

        {/* Main Content */}
        <div className="flex min-w-0 flex-1 flex-col">
          <JobStreamHeader onFilterClick={() => setIsFilterOpen(true)} />

          {/* Job Cards List */}
          <div className="flex-1 space-y-4 overflow-y-auto bg-gray-100 p-4 pb-20 lg:p-6 lg:pb-6">
            <JobStreamContent error={error} jobs={jobs} onTrack={handleTrack} />
          </div>
        </div>
      </div>

      {/* Mobile Filter Bottom Sheet */}
      <BottomSheet
        isOpen={isFilterOpen}
        onClose={() => setIsFilterOpen(false)}
        title={t("pages.jobStream.filters.title")}
      >
        <JobStreamFilterPanel
          filterOptions={filterOptions}
          filters={filters}
          hasActiveFilters={filtersActive}
          isMobile
          jobCount={jobCount}
          onClearFilters={handleClearFilters}
          onFiltersChange={handleFiltersChange}
        />
      </BottomSheet>
    </>
  );
}
