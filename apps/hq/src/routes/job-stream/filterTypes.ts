export type ExperienceLevel =
  | "intern"
  | "early"
  | "mid"
  | "senior"
  | "advanced"
  | "director"
  | "principal"
  | "other";

export type DegreeLevel =
  | "none"
  | "pursuing"
  | "associate"
  | "bachelors"
  | "masters"
  | "phd"
  | "bootcamp"
  | "certification"
  | "other";

export type JobType =
  | "full-time"
  | "part-time"
  | "contract"
  | "temporary"
  | "intern"
  | "freelance"
  | "other";

export type SortOption = "relevance" | "date";

export type JobStreamFilters = {
  search: string;
  locations: string[];
  remote: boolean;
  experience: ExperienceLevel[];
  skills: string[];
  degree: DegreeLevel[];
  jobTypes: JobType[];
  organizationIds: number[];
  sortBy: SortOption;
};

export function getDefaultFilters(): JobStreamFilters {
  return {
    search: "",
    locations: [],
    remote: false,
    experience: [],
    skills: [],
    degree: [],
    jobTypes: [],
    organizationIds: [],
    sortBy: "relevance",
  };
}

export function hasActiveFilters(filters: JobStreamFilters): boolean {
  return (
    filters.search.length > 0 ||
    filters.locations.length > 0 ||
    filters.remote === true ||
    filters.experience.length > 0 ||
    filters.skills.length > 0 ||
    filters.degree.length > 0 ||
    filters.jobTypes.length > 0 ||
    filters.organizationIds.length > 0
  );
}
