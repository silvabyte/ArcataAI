package arcata.api.http.routes

import scala.jdk.CollectionConverters.*

import arcata.api.domain.*
import arcata.api.etl.{ResumeParsingInput, ResumeParsingPipeline}
import arcata.api.http.auth.{AuthType, AuthenticatedRequest, authenticated}
import arcata.api.http.middleware.CorsConfig
import boogieloops.schema.derivation.Schematic
import boogieloops.web.*
import boogieloops.web.Web.ValidatedRequestReader
import cask.model.Response
import io.undertow.server.handlers.form.FormParserFactory
import upickle.default.*

/** Response for successful resume parsing. */
@Schematic.title("ParseResumeResponse")
@Schematic.description("Response from successful resume parsing")
final case class ParseResumeResponse(
    @Schematic.description("Whether the operation succeeded")
    success: Boolean,
    @Schematic.description("Extracted resume data")
    data: ExtractedResumeData,
    @Schematic.description("ObjectStorage ID of the original file")
    objectId: String,
    @Schematic.description("Original file name")
    fileName: String
) derives Schematic, ReadWriter

/** Error response for resume operations. */
@Schematic.title("ResumeErrorResponse")
@Schematic.description("Error response for resume operations")
final case class ResumeErrorResponse(
    @Schematic.description("Whether the operation succeeded (always false for errors)")
    success: Boolean,
    @Schematic.description("Error message")
    error: String,
    @Schematic.description("Additional error details")
    details: Option[String] = None
) derives Schematic, ReadWriter

/**
 * Routes for resume parsing API endpoints.
 *
 * @param basePath
 *   Base path prefix for all routes
 * @param pipeline
 *   The resume parsing pipeline
 * @param corsConfig
 *   CORS configuration for response headers
 */
class ResumeRoutes(
    basePath: String,
    pipeline: ResumeParsingPipeline,
    corsConfig: CorsConfig
) extends cask.Routes {

  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  private def withCors(request: cask.Request, headers: Seq[(String, String)]): Seq[(String, String)] = {
    val origin = request.headers.get("origin").flatMap(_.headOption).getOrElse("")
    headers ++ corsConfig.headersFor(origin)
  }

  /**
   * POST /api/v1/resumes/parse - Parse an uploaded resume file.
   *
   * Accepts a multipart/form-data request with a file upload. Extracts text from the file
   * (PDF/DOCX/TXT), uses AI to parse structured data, and returns the result.
   *
   * Form fields:
   *   - file: The resume file (required)
   */
  @authenticated(Vector(AuthType.JWT))
  @Web.post(
    s"$basePath/resumes/parse",
    RouteSchema(
      summary = Some("Parse resume file"),
      description = Some(
        "Uploads a resume file (PDF, DOCX, or TXT), extracts text, and uses AI to parse " +
          "structured resume data. Returns the extracted data suitable for the resume editor. " +
          "The file is also stored in ObjectStorage for later retrieval."
      ),
      tags = List("Resumes"),
      responses = Map(
        200 -> ApiResponse("Resume parsed successfully", Schematic[ParseResumeResponse]),
        400 -> ApiResponse("Invalid request or file", Schematic[ResumeErrorResponse]),
        401 -> ApiResponse("Unauthorized", Schematic[ResumeErrorResponse]),
        500 -> ApiResponse("Pipeline error", Schematic[ResumeErrorResponse])
      )
    )
  )
  def parseResume(
      r: ValidatedRequest
  )(authReq: AuthenticatedRequest): Response[String] = {
    // Parse multipart form data
    val parser = FormParserFactory.builder().build().createParser(r.original.exchange)
    val form = parser.parseBlocking()

    // Find the file upload
    var fileBytes: Option[Array[Byte]] = None
    var fileName: Option[String] = None
    var mimeType: Option[String] = None

    val it = form.iterator()
    while (it.hasNext) {
      val name = it.next()
      val vs = form.get(name).iterator()
      while (vs.hasNext) {
        val v = vs.next()
        if (v.isFileItem && name == "file") {
          val fi = v.getFileItem
          // Read file bytes
          val is = fi.getInputStream
          try {
            fileBytes = Some(is.readAllBytes())
          } finally {
            is.close()
          }
          // Get file name from FormValue
          fileName = Option(v.getFileName).filter(_.nonEmpty)
          // Get content type from headers if available
          mimeType = Option(v.getHeaders)
            .flatMap(h => Option(h.getFirst("Content-Type")))
            .filter(_.nonEmpty)
        }
      }
    }

    // Validate we have a file
    (fileBytes, fileName) match {
      case (None, _) =>
        val response = ResumeErrorResponse(
          success = false,
          error = "No file uploaded. Please provide a file in the 'file' field."
        )
        return Response(write(response), 400, withCors(r.original, jsonHeaders))

      case (_, None) =>
        val response = ResumeErrorResponse(
          success = false,
          error = "File name is required."
        )
        return Response(write(response), 400, withCors(r.original, jsonHeaders))

      case (Some(bytes), Some(name)) =>
        // Run the pipeline
        val input = ResumeParsingInput(
          fileBytes = bytes,
          fileName = name,
          claimedMimeType = mimeType,
          profileId = authReq.profileId
        )

        val result = pipeline.run(input, authReq.profileId)

        if (result.isSuccess) {
          val output = result.output.get
          val response = ParseResumeResponse(
            success = true,
            data = output.extractedData,
            objectId = output.objectId,
            fileName = output.fileName
          )
          Response(write(response), 200, withCors(r.original, jsonHeaders))
        } else {
          val error = result.error.get
          val response = ResumeErrorResponse(
            success = false,
            error = error.message,
            details = error.cause.map(_.getMessage)
          )
          // Use 400 for validation errors, 500 for other errors
          val statusCode = error.message.contains("not allowed") ||
            error.message.contains("exceeds") ||
            error.message.contains("empty") match {
            case true  => 400
            case false => 500
          }
          Response(write(response), statusCode, withCors(r.original, jsonHeaders))
        }
    }
  }

  initialize()
}

object ResumeRoutes {
  def apply(
      basePath: String,
      pipeline: ResumeParsingPipeline,
      corsConfig: CorsConfig
  ): ResumeRoutes =
    new ResumeRoutes(basePath, pipeline, corsConfig)
}
