import { CompanyLogo } from "@arcata/components";
import type { JobStreamEntry } from "@arcata/db";
import { t } from "@arcata/translate";
import { ArrowRightIcon } from "@heroicons/react/24/outline";
import { JobCardActions } from "./JobCardActions";
import { JobMetadataBadge } from "./JobMetadataBadge";
import { MatchScoreIndicator } from "./MatchScoreIndicator";

export type JobCardProps = {
  job: JobStreamEntry;
  onTrack?: (jobId: number, streamId: number) => void;
};

function formatSalary(amount: number | null): string {
  if (amount === null) {
    return "";
  }
  return `$${(amount / 1000).toFixed(0)}k`;
}

function formatSalaryRange(
  min: number | null,
  max: number | null
): string | null {
  if (min === null && max === null) {
    return null;
  }
  if (min !== null && max !== null) {
    return `${formatSalary(min)} - ${formatSalary(max)}`;
  }
  if (min !== null) {
    return `${formatSalary(min)}+`;
  }
  return `Up to ${formatSalary(max)}`;
}

function getPostedDaysAgo(postedDate: string | null): number {
  if (!postedDate) {
    return 0;
  }
  const posted = new Date(postedDate);
  const now = new Date();
  const diffMs = now.getTime() - posted.getTime();
  return Math.floor(diffMs / (1000 * 60 * 60 * 24));
}

function mapExperienceLevel(
  level: string | null
): "entry" | "mid" | "senior" | "staff" | null {
  if (!level) {
    return null;
  }
  const map: Record<string, "entry" | "mid" | "senior" | "staff"> = {
    intern: "entry",
    early: "entry",
    mid: "mid",
    senior: "senior",
    advanced: "senior",
    director: "staff",
    principal: "staff",
  };
  return map[level] ?? null;
}

function mapJobType(
  type: string | null
): "full-time" | "part-time" | "contract" | "temporary" | "intern" | null {
  if (!type) {
    return null;
  }
  // DB stores normalized values that mostly match badge keys
  const validTypes = [
    "full-time",
    "part-time",
    "contract",
    "temporary",
    "intern",
    "internship",
  ];
  if (type === "internship") {
    return "intern";
  }
  if (validTypes.includes(type)) {
    return type as "full-time" | "part-time" | "contract" | "temporary";
  }
  return null;
}

export function JobCard({ job, onTrack }: JobCardProps) {
  const jobData = job.jobs;
  if (!jobData) {
    return null;
  }

  const company = jobData.companies;
  const companyName = company?.company_name ?? "Unknown Company";
  const salaryRange = formatSalaryRange(jobData.salary_min, jobData.salary_max);
  const postedDaysAgo = getPostedDaysAgo(jobData.posted_date);
  const matchScore = job.best_match_score
    ? Math.round(job.best_match_score * 100)
    : null;
  const experienceLevel = mapExperienceLevel(jobData.experience_level);
  const jobType = mapJobType(jobData.job_type);
  const location = jobData.is_remote
    ? "Remote"
    : (jobData.location ?? "Location not specified");

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md lg:p-6">
      {/* Header: Logo, Title, Actions, Match Score */}
      <div className="flex gap-3 lg:gap-4">
        {/* Company Logo */}
        <CompanyLogo
          className="!h-12 !w-12 lg:!h-16 lg:!w-16"
          name={companyName}
          size="lg"
        />

        {/* Main Content */}
        <div className="min-w-0 flex-1">
          {/* Title Row - stacks differently on mobile */}
          <div className="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between lg:gap-4">
            <div className="min-w-0 flex-1">
              <h3 className="font-semibold text-gray-900 text-lg lg:text-xl">
                {jobData.title}
              </h3>
              {/* Company name - hidden on mobile, shown on desktop */}
              <p className="hidden text-gray-500 text-sm lg:block">
                {companyName}
              </p>
              {salaryRange !== null && (
                <p className="text-gray-600 text-sm">{salaryRange}</p>
              )}
            </div>

            {/* Actions and Match Score - desktop only in header */}
            <div className="hidden flex-col items-end gap-2 lg:flex">
              <JobCardActions
                onTrack={() => onTrack?.(jobData.job_id, job.stream_id)}
              />
              {matchScore !== null && (
                <MatchScoreIndicator
                  postedDaysAgo={postedDaysAgo}
                  score={matchScore}
                />
              )}
            </div>
          </div>

          {/* Badges - shown on both mobile and desktop */}
          <div className="mt-3 flex flex-wrap gap-2">
            {experienceLevel !== null && (
              <JobMetadataBadge type="level" value={experienceLevel} />
            )}
            {jobType !== null && (
              <JobMetadataBadge type="jobType" value={jobType} />
            )}
            <JobMetadataBadge type="location" value={location} />
          </div>

          {/* Mobile Actions - full width at bottom */}
          <div className="mt-4 lg:hidden">
            <JobCardActions
              isMobile
              onTrack={() => onTrack?.(jobData.job_id, job.stream_id)}
            />
          </div>

          {/* Qualifications - desktop only */}
          {jobData.description !== null && jobData.description.length > 0 && (
            <div className="mt-4 hidden lg:block">
              <p className="line-clamp-3 text-gray-600 text-sm">
                {jobData.description}
              </p>
            </div>
          )}

          {/* Footer: Link - desktop only */}
          {jobData.application_url !== null &&
            jobData.application_url.length > 0 && (
              <div className="mt-4 hidden items-center justify-end lg:flex">
                <a
                  className="inline-flex items-center gap-1 font-medium text-gray-900 text-sm hover:underline"
                  href={jobData.application_url}
                  rel="noopener noreferrer"
                  target="_blank"
                >
                  {t("pages.jobStream.card.viewFullDescription")}
                  <ArrowRightIcon className="h-4 w-4" />
                </a>
              </div>
            )}
        </div>
      </div>
    </div>
  );
}
