package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.etl.framework.*

/**
 * Input for JobStatusUpdater step.
 *
 * @param results
 *   Check results from JobStatusChecker
 */
case class JobStatusUpdaterInput(
    results: Seq[JobCheckResult]
)

/**
 * Output from JobStatusUpdater step.
 *
 * @param updatedCount
 *   Number of jobs successfully updated
 * @param failedCount
 *   Number of jobs that failed to update
 * @param closedCount
 *   Number of jobs marked as closed
 */
case class JobStatusUpdaterOutput(
    updatedCount: Int,
    failedCount: Int,
    closedCount: Int
)

/**
 * Updates job status in Supabase based on check results.
 *
 * For closed jobs:
 *   - Sets status = 'closed'
 *   - Sets closed_reason = detection reason
 *   - Sets closed_at = now()
 *   - Sets last_status_check = now()
 *
 * For open jobs:
 *   - Sets last_status_check = now() (to prevent re-checking too soon)
 */
class JobStatusUpdater(supabaseClient: SupabaseClient) extends BaseStep[JobStatusUpdaterInput, JobStatusUpdaterOutput]:

  val name = "JobStatusUpdater"

  override def execute(
      input: JobStatusUpdaterInput,
      ctx: PipelineContext
  ): Either[StepError, JobStatusUpdaterOutput] = {
    // Process results and accumulate counts using fold (immutable)
    val (updatedCount, failedCount, closedCount) = input.results.foldLeft((0, 0, 0)) {
      case ((updated, failed, closed), result) =>
        result.job.jobId match
          case None =>
            // Job has no ID - shouldn't happen, skip it
            logger.warn(s"[${ctx.runId}] Skipping job with no ID")
            (updated, failed + 1, closed)

          case Some(jobId) =>
            val isClosed = !result.isOpen
            val success = if isClosed then
              supabaseClient.updateJobStatusClosed(jobId, result.reason)
            else
              supabaseClient.updateJobLastCheck(jobId)

            if success then (updated + 1, failed, if isClosed then closed + 1 else closed)
            else {
              logger.error(s"[${ctx.runId}] Failed to update job $jobId")
              (updated, failed + 1, closed)
            }
    }

    logger.info(
      s"[${ctx.runId}] Updated $updatedCount jobs " +
        s"($closedCount marked closed, $failedCount failed)"
    )

    Right(JobStatusUpdaterOutput(updatedCount, failedCount, closedCount))
  }

object JobStatusUpdater:
  def apply(supabaseClient: SupabaseClient): JobStatusUpdater =
    new JobStatusUpdater(supabaseClient)
