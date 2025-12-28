import type { Database } from "./database.types";

export type Company = Database["public"]["Tables"]["companies"]["Row"];
export type Job = Database["public"]["Tables"]["jobs"]["Row"];
export type Profile = Database["public"]["Tables"]["profiles"]["Row"];
export type JobProfile = Database["public"]["Tables"]["job_profiles"]["Row"];
export type ApplicationStatus =
  Database["public"]["Tables"]["application_statuses"]["Row"];
export type JobApplication =
  Database["public"]["Tables"]["job_applications"]["Row"];
export type Conversation = Database["public"]["Tables"]["conversations"]["Row"];
export type JobStream = Database["public"]["Tables"]["job_stream"]["Row"];
export type ApplicationAnswer =
  Database["public"]["Tables"]["application_answers"]["Row"];

// === Enum Types ===

/** Job profile status: draft (work in progress) or live (available for applications) */
export type JobProfileStatus =
  Database["public"]["Enums"]["job_profile_status"];

// === Joined Types (for queries with relations) ===

/** Job with nested company data */
export type JobWithCompany = Job & {
  companies: Company | null;
};

/** JobApplication with nested job and company data */
export type JobApplicationWithJob = JobApplication & {
  jobs: JobWithCompany | null;
};
