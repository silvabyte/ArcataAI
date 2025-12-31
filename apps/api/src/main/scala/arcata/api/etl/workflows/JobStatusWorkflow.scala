package arcata.api.etl.workflows

import arcata.api.clients.SupabaseClient
import arcata.api.etl.framework.*
import arcata.api.etl.steps.*

/**
 * Input for JobStatusWorkflow.
 *
 * @param batchSize
 *   Maximum number of jobs to check per run
 * @param olderThanDays
 *   Only check jobs not verified in this many days
 */
case class JobStatusInput(
    batchSize: Int = 1000,
    olderThanDays: Int = 7
)

/**
 * Output from JobStatusWorkflow.
 *
 * @param totalChecked
 *   Total number of jobs checked
 * @param openCount
 *   Number of jobs still open
 * @param closedCount
 *   Number of jobs marked as closed
 * @param updatedCount
 *   Number of database updates succeeded
 */
case class JobStatusOutput(
    totalChecked: Int,
    openCount: Int,
    closedCount: Int,
    updatedCount: Int
)

/**
 * Async workflow that checks job posting status.
 *
 * This workflow:
 *   1. Fetches open jobs that haven't been checked recently
 *   2. Re-fetches each job's source URL to check if still active
 *   3. Updates job status in database (closed jobs filtered from streams)
 *
 * Triggered via HTTP POST to /api/v1/cron/job-status-check, runs in background. The endpoint
 * returns 202 Accepted immediately; actual processing happens asynchronously.
 *
 * Usage:
 * {{{
 * // In ApiApp or routes
 * given actorContext: castor.Context = ...
 * val workflow = JobStatusWorkflow(supabaseClient)
 *
 * // Trigger (fire-and-forget)
 * workflow.send(WorkflowRun(JobStatusInput(batchSize = 50), "system"))
 * }}}
 */
class JobStatusWorkflow(
    supabaseClient: SupabaseClient
)(using ac: castor.Context)
    extends BaseWorkflow[JobStatusInput, JobStatusOutput]:

  val name = "JobStatusWorkflow"

  // Pipeline steps
  private val jobsFetcher = JobsToCheckFetcher(supabaseClient)
  private val statusChecker = JobStatusChecker()
  private val statusUpdater = JobStatusUpdater(supabaseClient)

  override def execute(
      input: JobStatusInput,
      ctx: PipelineContext
  ): Either[StepError, JobStatusOutput] = {
    for
      // Step 1: Fetch jobs to check
      fetchResult <- jobsFetcher.run(
        JobsToCheckInput(input.batchSize, input.olderThanDays),
        ctx
      )

      // Step 2: Check each job's status by re-fetching URLs
      checkResult <- statusChecker.run(
        JobStatusCheckerInput(fetchResult.jobs),
        ctx
      )

      // Step 3: Update job statuses in database
      updateResult <- statusUpdater.run(
        JobStatusUpdaterInput(checkResult.results),
        ctx
      )
    yield JobStatusOutput(
      totalChecked = checkResult.totalChecked,
      openCount = checkResult.openCount,
      closedCount = checkResult.closedCount,
      updatedCount = updateResult.updatedCount
    )
  }

  // Override for detailed success logging
  override def onSuccess(result: PipelineResult[JobStatusOutput]): Unit = {
    result.output.foreach { out =>
      logger.info(
        s"[$name] Completed in ${result.durationMs}ms: " +
          s"checked=${out.totalChecked}, open=${out.openCount}, " +
          s"closed=${out.closedCount}, updated=${out.updatedCount}"
      )
    }
  }

object JobStatusWorkflow:
  def apply(supabaseClient: SupabaseClient)(using ac: castor.Context): JobStatusWorkflow =
    new JobStatusWorkflow(supabaseClient)
