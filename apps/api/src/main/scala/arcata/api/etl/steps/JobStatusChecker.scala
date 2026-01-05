package arcata.api.etl.steps

import scala.util.{Failure, Success, Try}

import arcata.api.domain.Job
import arcata.api.etl.framework.*

/**
 * Input for JobStatusChecker step.
 *
 * @param jobs
 *   Sequence of jobs to check
 */
case class JobStatusCheckerInput(
  jobs: Seq[Job]
)

/**
 * Result of checking a single job's status.
 *
 * @param job
 *   The job that was checked
 * @param isOpen
 *   Whether the job is still open/active
 * @param reason
 *   Why the job was determined to be open or closed
 */
case class JobCheckResult(
  job: Job,
  isOpen: Boolean,
  reason: Option[String],
)

/**
 * Output from JobStatusChecker step.
 *
 * @param results
 *   Individual check results for each job
 * @param totalChecked
 *   Total number of jobs checked
 * @param openCount
 *   Number of jobs still open
 * @param closedCount
 *   Number of jobs determined to be closed
 */
case class JobStatusCheckerOutput(
  results: Seq[JobCheckResult],
  totalChecked: Int,
  openCount: Int,
  closedCount: Int,
)

/**
 * Re-fetches job URLs to detect if postings are still active.
 *
 * Detection strategies:
 *   1. HTTP 404/410 -> closed (page not found / gone)
 *   2. HTTP 3xx to different domain -> likely closed (redirected away)
 *   3. Page contains closure signals -> closed
 *   4. Otherwise -> still open
 *
 * Network errors do NOT mark jobs as closed (could be temporary).
 */
class JobStatusChecker extends BaseStep[JobStatusCheckerInput, JobStatusCheckerOutput]:

  val name = "JobStatusChecker"

  // Signals in page content that indicate a job is closed
  private val closureSignals = Seq(
    "position has been filled",
    "no longer accepting",
    "job has been closed",
    "this position is closed",
    "job is no longer available",
    "posting has expired",
    "this job has expired",
    "application period has ended",
    "position is no longer available",
    "job posting has been removed",
    "this role has been filled",
    "we are no longer accepting applications",
  )

  override def execute(
    input: JobStatusCheckerInput,
    ctx: PipelineContext,
  ): Either[StepError, JobStatusCheckerOutput] = {
    val results = input.jobs.map(checkJobStatus)

    val output = JobStatusCheckerOutput(
      results = results,
      totalChecked = results.size,
      openCount = results.count(_.isOpen),
      closedCount = results.count(!_.isOpen),
    )

    logger.info(
      s"[${ctx.runId}] Checked ${output.totalChecked} jobs: " +
        s"${output.openCount} open, ${output.closedCount} closed"
    )

    Right(output)
  }

  private def checkJobStatus(job: Job): JobCheckResult = {
    job.sourceUrl match
      case None =>
        // No source URL - can't check, assume still open
        JobCheckResult(job, isOpen = true, reason = Some("no source URL"))

      case Some(url) =>
        Try {
          val response = requests.get(
            url,
            check = false, // Don't throw on non-2xx
            maxRedirects = 5,
          )
          (response.statusCode, response.text())
        } match
          case Success((code, text)) =>
            checkResponse(job, url, code, text)

          case Failure(ex) =>
            // Network error - don't mark as closed, could be temporary
            logger.warn(s"Failed to fetch ${url}: ${ex.getMessage}")
            JobCheckResult(job, isOpen = true, reason = Some(s"fetch error: ${ex.getMessage}"))
  }

  private def checkResponse(job: Job, url: String, statusCode: Int, body: String): JobCheckResult = {
    statusCode match
      case 404 | 410 =>
        // Page not found or gone - job is closed
        JobCheckResult(job, isOpen = false, reason = Some(s"HTTP $statusCode"))

      case code if code >= 200 && code < 300 =>
        // Page exists - check content for closure signals
        val bodyLower = body.toLowerCase
        closureSignals.find(bodyLower.contains) match
          case Some(signal) =>
            JobCheckResult(job, isOpen = false, reason = Some(s"page contains: '$signal'"))
          case None =>
            JobCheckResult(job, isOpen = true, reason = None)

      case code if code >= 300 && code < 400 =>
        // Redirect (shouldn't happen with followRedirects, but handle it)
        JobCheckResult(job, isOpen = true, reason = Some(s"redirect (HTTP $code)"))

      case code if code >= 500 =>
        // Server error - don't mark as closed, could be temporary
        JobCheckResult(job, isOpen = true, reason = Some(s"server error (HTTP $code)"))

      case code =>
        // Other status codes (e.g., 403) - assume still open
        JobCheckResult(job, isOpen = true, reason = Some(s"HTTP $code"))
  }

object JobStatusChecker:
  def apply(): JobStatusChecker = new JobStatusChecker()
