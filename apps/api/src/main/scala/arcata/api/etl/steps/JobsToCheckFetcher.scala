package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.domain.Job
import arcata.api.etl.framework.*

/**
 * Input for JobsToCheckFetcher step.
 *
 * @param batchSize
 *   Maximum number of jobs to fetch
 * @param olderThanDays
 *   Only include jobs not checked in this many days (or never checked)
 */
case class JobsToCheckInput(
  batchSize: Int = 100,
  olderThanDays: Int = 7,
)

/**
 * Output from JobsToCheckFetcher step.
 *
 * @param jobs
 *   Sequence of jobs that need status checking
 */
case class JobsToCheckOutput(
  jobs: Seq[Job]
)

/**
 * Fetches open jobs that need their status checked.
 *
 * Queries Supabase for jobs where:
 *   - status = 'open' (not already closed)
 *   - source_url IS NOT NULL (can be re-fetched)
 *   - last_status_check is NULL OR older than threshold
 *
 * Jobs are ordered by last_status_check ascending (nulls first), ensuring jobs that have never
 * been checked or were checked longest ago are processed first.
 */
class JobsToCheckFetcher(supabaseClient: SupabaseClient) extends BaseStep[JobsToCheckInput, JobsToCheckOutput]:

  val name = "JobsToCheckFetcher"

  override def execute(
    input: JobsToCheckInput,
    ctx: PipelineContext,
  ): Either[StepError, JobsToCheckOutput] = {
    val jobs = supabaseClient.findJobsToCheck(input.batchSize, input.olderThanDays)
    logger.info(s"[${ctx.runId}] Found ${jobs.size} jobs to check")
    Right(JobsToCheckOutput(jobs))
  }

object JobsToCheckFetcher:
  def apply(supabaseClient: SupabaseClient): JobsToCheckFetcher =
    new JobsToCheckFetcher(supabaseClient)
