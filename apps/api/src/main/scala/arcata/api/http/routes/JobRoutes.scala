package arcata.api.http.routes

import arcata.api.etl.{JobIngestionInput, JobIngestionPipeline}
import arcata.api.http.auth.{AuthenticatedRequest, JwtClaims, JwtValidationResult, JwtValidator}
import arcata.api.http.middleware.{CorsConfig, cors}
import boogieloops.schema.derivation.Schematic
import boogieloops.web.*
import boogieloops.web.Web.ValidatedRequestReader
import cask.model.{Request, Response}
import cask.router.{RawDecorator, Result}
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
    notes: Option[String] = None
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
    message: String
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
    details: Option[String] = None
) derives Schematic, ReadWriter

/**
 * Routes for job-related API endpoints.
 *
 * @param basePath
 *   Base path prefix for all routes
 * @param pipeline
 *   The job ingestion pipeline
 * @param jwtValidator
 *   JWT validator for authentication
 * @param corsConfig
 *   CORS configuration for cross-origin requests
 */
class JobRoutes(
    basePath: String,
    pipeline: JobIngestionPipeline,
    jwtValidator: JwtValidator,
    corsConfig: CorsConfig
) extends cask.Routes {
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  /** Inner decorator class that validates JWT tokens. */
  class authenticated extends RawDecorator:
    override def wrapFunction(
        ctx: Request,
        delegate: Delegate
    ): Result[Response.Raw] = {
      val authHeader = ctx.headers.get("authorization").flatMap(_.headOption)

      authHeader match
        case None =>
          Result.Success(
            Response(
              data = """{"error": "Missing Authorization header"}""",
              statusCode = 401,
              headers = Seq("Content-Type" -> "application/json")
            )
          )

        case Some(header) =>
          jwtValidator.validateAuthHeader(header) match
            case JwtValidationResult.Valid(profileId, claims) =>
              val authReq = AuthenticatedRequest(profileId, claims, ctx)
              delegate(ctx, Map("authenticatedRequest" -> authReq))

            case JwtValidationResult.Invalid(reason) =>
              Result.Success(
                Response(
                  data = s"""{"error": "Invalid token: $reason"}""",
                  statusCode = 401,
                  headers = Seq("Content-Type" -> "application/json")
                )
              )
    }

  /**
   * POST /api/v1/jobs/ingest - Ingest a job from a URL.
   *
   * Fetches the job posting from the provided URL, extracts job details using AI, and adds it to
   * the user's job stream.
   */
  @cors(corsConfig)
  @authenticated()
  @Web.post(
    s"$basePath/jobs/ingest",
    RouteSchema(
      summary = Some("Ingest job from URL"),
      description = Some(
        "Fetches the job posting from the provided URL, extracts job details using AI, " +
          "and adds it to the user's job stream. Optionally creates a job application."
      ),
      tags = List("Jobs"),
      body = Some(Schematic[IngestJobRequest]),
      responses = Map(
        200 -> ApiResponse("Job ingested successfully", Schematic[IngestJobResponse]),
        400 -> ApiResponse("Invalid request", Schematic[JobErrorResponse]),
        401 -> ApiResponse("Unauthorized", Schematic[JobErrorResponse]),
        500 -> ApiResponse("Pipeline error", Schematic[JobErrorResponse])
      )
    )
  )
  def ingestJob(
      r: ValidatedRequest
  )(authenticatedRequest: AuthenticatedRequest): Response[String] = {
    val body = read[IngestJobRequest](r.original.text())

    val input = JobIngestionInput(
      url = body.url,
      profileId = authenticatedRequest.profileId,
      source = body.source.getOrElse("manual"),
      createApplication = body.createApplication.getOrElse(false),
      notes = body.notes
    )

    val result = pipeline.run(input, authenticatedRequest.profileId)

    if (result.isSuccess) {
      val output = result.output.get
      val response = IngestJobResponse(
        success = true,
        jobId = output.job.jobId.getOrElse(0L),
        streamId = output.streamEntry.flatMap(_.streamId),
        applicationId = output.application.flatMap(_.applicationId),
        message = s"Successfully ingested job: ${output.job.title}"
      )
      Response(write(response), 200, jsonHeaders)
    } else {
      val error = result.error.get
      val response = JobErrorResponse(
        success = false,
        error = error.message,
        details = error.cause.map(_.getMessage)
      )
      Response(write(response), 500, jsonHeaders)
    }
  }

  initialize()
}

object JobRoutes {
  def apply(
      basePath: String,
      pipeline: JobIngestionPipeline,
      jwtValidator: JwtValidator,
      corsConfig: CorsConfig
  ): JobRoutes =
    new JobRoutes(basePath, pipeline, jwtValidator, corsConfig)
}
