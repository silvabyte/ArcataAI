package arcata.api.http.routes

import arcata.api.etl.{JobIngestionInput, JobIngestionPipeline}
import arcata.api.http.auth.{AuthType, AuthenticatedRequest, authenticated}
import arcata.api.http.middleware.CorsConfig
import boogieloops.schema.derivation.Schematic
import boogieloops.web.*
import boogieloops.web.Web.ValidatedRequestReader
import cask.model.Response
import upickle.default.*

/** Request body for job ingestion. */
@Schematic.title("IngestJobRequest")
@Schematic.description("Request to ingest a job from a URL")
final case class IngestJobRequest(
  @Schematic.description("The job posting URL to fetch and parse")
  url: String,
  @Schematic.description("How the job was found (e.g., 'manual', 'extension', 'ai_discovery')")
  source: Option[String] = None,
  @Schematic.description("Whether to also create a job application in 'Saved' status")
  createApplication: Option[Boolean] = None,
  @Schematic.description("Notes to attach if creating an application")
  notes: Option[String] = None,
) derives Schematic, ReadWriter

/** Response for successful job ingestion. */
@Schematic.title("IngestJobResponse")
@Schematic.description("Response from successful job ingestion")
final case class IngestJobResponse(
  @Schematic.description("Whether the operation succeeded")
  success: Boolean,
  @Schematic.description("The created or found job ID")
  jobId: Long,
  @Schematic.description("The job stream entry ID")
  streamId: Option[Long],
  @Schematic.description("The application ID (if createApplication was true)")
  applicationId: Option[Long],
  @Schematic.description("Human-readable success message")
  message: String,
) derives Schematic, ReadWriter

/** Error response. */
@Schematic.title("JobErrorResponse")
@Schematic.description("Error response for job operations")
final case class JobErrorResponse(
  @Schematic.description("Whether the operation succeeded (always false for errors)")
  success: Boolean,
  @Schematic.description("Error message")
  error: String,
  @Schematic.description("Additional error details")
  details: Option[String] = None,
) derives Schematic, ReadWriter

/**
 * Routes for job-related API endpoints.
 *
 * @param basePath
 *   Base path prefix for all routes
 * @param pipeline
 *   The job ingestion pipeline
 * @param corsConfig
 *   CORS configuration for response headers
 */
class JobRoutes(
  basePath: String,
  pipeline: JobIngestionPipeline,
  corsConfig: CorsConfig,
) extends cask.Routes {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  private def withCors(request: cask.Request, headers: Seq[(String, String)]): Seq[(String, String)] = {
    val origin = request.headers.get("origin").flatMap(_.headOption).getOrElse("")
    headers ++ corsConfig.headersFor(origin)
  }

  /**
   * POST /api/v1/jobs/ingest - Ingest a job from a URL.
   *
   * Fetches the job posting from the provided URL, extracts job details using AI, and adds it to
   * the user's job stream. Supports both JWT (user) and API key (service) authentication.
   *
   * When authenticated via API key (service-to-service):
   *   - Only saves the job and company to the database
   *   - Skips job stream creation (no user to stream to)
   *   - Ignores createApplication flag
   */
  @authenticated(Vector(AuthType.JWT, AuthType.ApiKey))
  @Web.post(
    s"$basePath/jobs/ingest",
    RouteSchema(
      summary = Some("Ingest job from URL"),
      description = Some(
        "Fetches the job posting from the provided URL, extracts job details using AI, " +
          "and adds it to the user's job stream. Optionally creates a job application. " +
          "Supports both JWT (user) and API key (service) authentication. " +
          "Service calls skip stream/application creation."
      ),
      tags = List("Jobs"),
      body = Some(Schematic[IngestJobRequest]),
      responses = Map(
        200 -> ApiResponse("Job ingested successfully", Schematic[IngestJobResponse]),
        400 -> ApiResponse("Invalid request", Schematic[JobErrorResponse]),
        401 -> ApiResponse("Unauthorized", Schematic[JobErrorResponse]),
        500 -> ApiResponse("Pipeline error", Schematic[JobErrorResponse]),
      ),
    ),
  )
  def ingestJob(
    r: ValidatedRequest
  )(authReq: AuthenticatedRequest): Response[String] = {
    // Use getBody to access the already-validated body (stream is consumed by Web.post)
    r.getBody[IngestJobRequest] match {
      case Left(validationError) =>
        val response = JobErrorResponse(
          success = false,
          error = s"Invalid request body: ${validationError.message}",
          details = None,
        )
        Response(write(response), 400, withCors(r.original, jsonHeaders))

      case Right(body) =>
        // For API key auth (service calls), skip stream and application creation
        val isServiceCall = authReq.authType == AuthType.ApiKey

        val input = JobIngestionInput(
          url = body.url,
          profileId = authReq.profileId,
          source = body.source.getOrElse(if isServiceCall then "service" else "manual"),
          createApplication = if isServiceCall then false else body.createApplication.getOrElse(false),
          notes = if isServiceCall then None else body.notes,
          skipStream = isServiceCall,
        )

        val result = pipeline.run(input, authReq.profileId)

        if (result.isSuccess) {
          val output = result.output.get
          val response = IngestJobResponse(
            success = true,
            jobId = output.job.jobId.getOrElse(0L),
            streamId = output.streamEntry.flatMap(_.streamId),
            applicationId = output.application.flatMap(_.applicationId),
            message = s"Successfully ingested job: ${output.job.title}",
          )
          Response(write(response), 200, withCors(r.original, jsonHeaders))
        } else {
          val error = result.error.get
          val response = JobErrorResponse(
            success = false,
            error = error.message,
            details = error.cause.map(_.getMessage),
          )
          Response(write(response), 500, withCors(r.original, jsonHeaders))
        }
    }
  }

  initialize()
}

object JobRoutes {
  def apply(
    basePath: String,
    pipeline: JobIngestionPipeline,
    corsConfig: CorsConfig,
  ): JobRoutes =
    new JobRoutes(basePath, pipeline, corsConfig)
}
