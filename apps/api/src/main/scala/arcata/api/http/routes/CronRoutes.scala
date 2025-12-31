package arcata.api.http.routes

import arcata.api.etl.framework.WorkflowRun
import arcata.api.etl.workflows.{JobStatusInput, JobStatusWorkflow}
import arcata.api.http.middleware.CorsConfig
import boogieloops.schema.derivation.Schematic
import boogieloops.web.*
import boogieloops.web.Web.ValidatedRequestReader
import cask.model.Response
import upickle.default.*

/**
 * Request body for triggering job status check workflow.
 */
@Schematic.title("JobStatusCheckRequest")
@Schematic.description("Request to trigger the job status check workflow")
final case class JobStatusCheckRequest(
    @Schematic.description("Maximum number of jobs to check (default 100)")
    batchSize: Option[Int] = None,
    @Schematic.description("Only check jobs not verified in this many days (default 7)")
    olderThanDays: Option[Int] = None
) derives Schematic,
      ReadWriter

/**
 * Response when a workflow is accepted for async processing.
 */
@Schematic.title("WorkflowAcceptedResponse")
@Schematic.description("Response when a workflow is accepted for background processing")
final case class WorkflowAcceptedResponse(
    @Schematic.description("Whether the workflow was accepted")
    accepted: Boolean,
    @Schematic.description("Unique identifier for this workflow run")
    runId: String,
    @Schematic.description("Name of the workflow that was triggered")
    workflow: String,
    @Schematic.description("Human-readable message")
    message: String
) derives Schematic,
      ReadWriter

/**
 * Error response for cron endpoints.
 */
@Schematic.title("CronErrorResponse")
@Schematic.description("Error response for cron/workflow operations")
final case class CronErrorResponse(
    @Schematic.description("Whether the workflow was accepted (always false for errors)")
    accepted: Boolean,
    @Schematic.description("Error message")
    error: String
) derives Schematic,
      ReadWriter

/**
 * Routes for triggering cron/background workflows.
 *
 * These endpoints return 202 Accepted immediately. Actual processing happens asynchronously via
 * Castor actors. Results are logged but not returned to the caller.
 *
 * @param jobStatusWorkflow
 *   The job status checker workflow actor
 * @param corsConfig
 *   CORS configuration for response headers
 * @param cronSecret
 *   Optional secret key for authentication (X-Cron-Secret header)
 */
class CronRoutes(
    jobStatusWorkflow: JobStatusWorkflow,
    corsConfig: CorsConfig,
    cronSecret: Option[String] = None
) extends cask.Routes {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  private def withCors(request: cask.Request, headers: Seq[(String, String)]): Seq[(String, String)] = {
    val origin = request.headers.get("origin").flatMap(_.headOption).getOrElse("")
    headers ++ corsConfig.headersFor(origin)
  }

  /**
   * Validate the cron secret if configured.
   */
  private def validateCronSecret(request: cask.Request): Boolean = {
    cronSecret match
      case None => true // No secret configured, allow all
      case Some(secret) => request.headers.get("x-cron-secret").flatMap(_.headOption).contains(secret)
  }

  /**
   * POST /api/v1/cron/job-status-check - Trigger job status check workflow.
   *
   * Checks if tracked jobs are still active by re-fetching their URLs. Closed jobs are marked as
   * such and filtered from job streams.
   *
   * Returns 202 Accepted immediately; processing happens in background.
   */
  @Web.post(
    "/api/v1/cron/job-status-check",
    RouteSchema(
      summary = Some("Trigger job status check workflow"),
      description = Some(
        "Checks if tracked jobs are still active by re-fetching their URLs. " +
          "Closed jobs are marked as such and filtered from job streams. " +
          "Returns 202 Accepted immediately; processing happens in background."
      ),
      tags = List("Cron", "Workflows"),
      body = Some(Schematic[JobStatusCheckRequest]),
      responses = Map(
        202 -> ApiResponse("Workflow accepted", Schematic[WorkflowAcceptedResponse]),
        401 -> ApiResponse("Unauthorized - invalid cron secret", Schematic[CronErrorResponse])
      )
    )
  )
  def triggerJobStatusCheck(r: ValidatedRequest): Response[String] = {
    if !validateCronSecret(r.original) then
      val response = CronErrorResponse(
        accepted = false,
        error = "Invalid or missing X-Cron-Secret header"
      )
      return Response(write(response), 401, withCors(r.original, jsonHeaders))

    r.getBody[JobStatusCheckRequest] match {
      case Left(validationError) =>
        val response = CronErrorResponse(
          accepted = false,
          error = s"Invalid request body: ${validationError.message}"
        )
        Response(write(response), 400, withCors(r.original, jsonHeaders))

      case Right(body) =>
        val runId = java.util.UUID.randomUUID().toString
        val input = JobStatusInput(
          batchSize = body.batchSize.getOrElse(100),
          olderThanDays = body.olderThanDays.getOrElse(7)
        )

        // Fire and forget - send to actor
        jobStatusWorkflow.send(WorkflowRun(input, profileId = "system"))

        val response = WorkflowAcceptedResponse(
          accepted = true,
          runId = runId,
          workflow = "JobStatusWorkflow",
          message =
            s"Job status check workflow started with batchSize=${input.batchSize}, olderThanDays=${input.olderThanDays}"
        )
        Response(write(response), 202, withCors(r.original, jsonHeaders))
    }
  }

  initialize()
}

object CronRoutes:
  def apply(
      jobStatusWorkflow: JobStatusWorkflow,
      corsConfig: CorsConfig,
      cronSecret: Option[String] = None
  ): CronRoutes =
    new CronRoutes(jobStatusWorkflow, corsConfig, cronSecret)
