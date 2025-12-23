/**
 * Full job detail data including company information.
 * Used for the job detail panel display.
 */
export type JobDetail = {
  jobId: number;
  companyId: number;
  title: string;
  description: string | null;
  location: string | null;
  jobType: string | null;
  category: string | null;
  experienceLevel: string | null;
  educationLevel: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  qualifications: string[] | null;
  preferredQualifications: string[] | null;
  responsibilities: string[] | null;
  benefits: string[] | null;
  sourceUrl: string | null;
  applicationUrl: string | null;
  isRemote: boolean | null;
  postedDate: string | null;
  closingDate: string | null;
  // Company fields (joined)
  companyName: string | null;
  companyDomain: string | null;
};

/**
 * Job stream entry with application tracking info.
 * Extends the basic job stream with application_id for tracking state.
 */
export type JobStreamEntryWithTracking = {
  streamId: number;
  jobId: number;
  applicationId: number | null;
  bestMatchScore: number | null;
  status: string | null;
};
