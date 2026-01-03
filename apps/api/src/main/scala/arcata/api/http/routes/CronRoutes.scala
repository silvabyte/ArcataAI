package arcata.api.http.routes

import arcata.api.etl.framework.WorkflowRun
import arcata.api.etl.workflows.{JobDiscoveryInput, JobDiscoveryWorkflow, JobStatusInput, JobStatusWorkflow}
import arcata.api.http.auth.{AuthType, AuthenticatedRequest, authenticated}
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
 * Request body for triggering job discovery workflow.
 */
@Schematic.title("JobDiscoveryRequest")
@Schematic.description("Request to trigger the job discovery workflow")
final case class JobDiscoveryRequest(
    @Schematic.description(
      "Optional source ID to filter to a specific source (e.g., 'greenhouse'). If omitted, all sources are processed."
    )
    sourceId: Option[String] = None
) derives Schematic,
      ReadWriter

/**
 * Routes for triggering cron/background workflows.
 *
 * These endpoints return 202 Accepted immediately. Actual processing happens asynchronously via
 * Castor actors. Results are logged but not returned to the caller.
 *
 * Authentication is via API key (X-API-Key header). Configure valid keys via the API_KEYS
 * environment variable.
 *
 * @param jobStatusWorkflow
 *   The job status checker workflow actor
 * @param jobDiscoveryWorkflow
 *   The job discovery workflow actor
 * @param corsConfig
 *   CORS configuration for response headers
 */
class CronRoutes(
    jobStatusWorkflow: JobStatusWorkflow,
    jobDiscoveryWorkflow: JobDiscoveryWorkflow,
    corsConfig: CorsConfig
) extends cask.Routes {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  private def withCors(request: cask.Request, headers: Seq[(String, String)]): Seq[(String, String)] = {
    val origin = request.headers.get("origin").flatMap(_.headOption).getOrElse("")
    headers ++ corsConfig.headersFor(origin)
  }

  /**
   * POST /api/v1/cron/job-status-check - Trigger job status check workflow.
   *
   * Checks if tracked jobs are still active by re-fetching their URLs. Closed jobs are marked as
   * such and filtered from job streams.
   *
   * Returns 202 Accepted immediately; processing happens in background.
   */
  @authenticated(Vector(AuthType.ApiKey))
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
        401 -> ApiResponse("Unauthorized - invalid API key", Schematic[CronErrorResponse])
      )
    )
  )
  def triggerJobStatusCheck(r: ValidatedRequest)(authReq: AuthenticatedRequest): Response[String] = {
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
        jobStatusWorkflow.send(WorkflowRun(input, profileId = authReq.profileId))

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

  /**
   * POST /api/v1/cron/job-discovery - Trigger job discovery workflow.
   *
   * Discovers new jobs from registered ATS sources (e.g., Greenhouse). Jobs are added to the
   * system-level jobs table for later matching with user profiles.
   *
   * Returns 202 Accepted immediately; processing happens in background.
   */
  @authenticated(Vector(AuthType.ApiKey))
  @Web.post(
    "/api/v1/cron/job-discovery",
    RouteSchema(
      summary = Some("Trigger job discovery workflow"),
      description = Some(
        "Discovers new jobs from registered ATS sources (e.g., Greenhouse). " +
          "Jobs are added to the system-level jobs table for later matching with user profiles. " +
          "Returns 202 Accepted immediately; processing happens in background."
      ),
      tags = List("Cron", "Workflows"),
      body = Some(Schematic[JobDiscoveryRequest]),
      responses = Map(
        202 -> ApiResponse("Workflow accepted", Schematic[WorkflowAcceptedResponse]),
        401 -> ApiResponse("Unauthorized - invalid API key", Schematic[CronErrorResponse])
      )
    )
  )
  def triggerJobDiscovery(r: ValidatedRequest)(authReq: AuthenticatedRequest): Response[String] = {
    r.getBody[JobDiscoveryRequest] match {
      case Left(validationError) =>
        val response = CronErrorResponse(
          accepted = false,
          error = s"Invalid request body: ${validationError.message}"
        )
        Response(write(response), 400, withCors(r.original, jsonHeaders))

      case Right(body) =>
        val runId = java.util.UUID.randomUUID().toString
        val input = JobDiscoveryInput(sourceId = body.sourceId)

        // Fire and forget - send to actor
        jobDiscoveryWorkflow.send(WorkflowRun(input, profileId = authReq.profileId))

        val sourceMsg = body.sourceId match
          case Some(id) => s"source=$id"
          case None => "all sources"

        val response = WorkflowAcceptedResponse(
          accepted = true,
          runId = runId,
          workflow = "JobDiscoveryWorkflow",
          message = s"Job discovery workflow started for $sourceMsg"
        )
        Response(write(response), 202, withCors(r.original, jsonHeaders))
    }
  }

  initialize()
}

object CronRoutes:
  def apply(
      jobStatusWorkflow: JobStatusWorkflow,
      jobDiscoveryWorkflow: JobDiscoveryWorkflow,
      corsConfig: CorsConfig
  ): CronRoutes =
    new CronRoutes(jobStatusWorkflow, jobDiscoveryWorkflow, corsConfig)
