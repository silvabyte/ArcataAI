package arcata.api.http.routes

import boogieloops.schema.derivation.Schematic
import boogieloops.web.*
import boogieloops.web.Web.ValidatedRequestReader
import boogieloops.web.openapi.config.OpenAPIConfig
import cask.model.Response
import upickle.default.*

/** Simple status response for health/ping endpoints */
@Schematic.title("StatusResponse")
@Schematic.description("Simple status response")
final case class StatusResponse(
    @Schematic.description("Status indicator")
    status: String
) derives Schematic, ReadWriter

/** Ping response */
@Schematic.title("PingResponse")
@Schematic.description("Ping response")
final case class PingResponse(
    @Schematic.description("Pong message")
    message: String = "pong"
) derives Schematic, ReadWriter

/** API info response */
@Schematic.title("ApiInfoResponse")
@Schematic.description("API information response")
final case class ApiInfoResponse(
    @Schematic.description("API name")
    name: String,
    @Schematic.description("API version")
    version: String,
    @Schematic.description("API status")
    status: String
) derives Schematic, ReadWriter

/**
 * Health and status endpoints for the Arcata API service.
 *
 * Provides basic health checks, ping endpoints, and OpenAPI spec generation.
 */
case class IndexRoutes() extends cask.Routes {
  private val apiPrefix = "/api/v1"
  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  /** Root endpoint - API info */
  @Web.get(
    "/",
    RouteSchema(
      summary = Some("API info"),
      description = Some("Returns API name, version, and status"),
      tags = List("Health"),
      responses = Map(
        200 -> ApiResponse("API is running", Schematic[ApiInfoResponse])
      )
    )
  )
  def index(r: ValidatedRequest): Response[String] = {
    Response(
      write(ApiInfoResponse("Arcata API", "1.0.0", "running")),
      200,
      jsonHeaders
    )
  }

  /** Ping endpoint for basic connectivity check */
  @Web.get(
    s"$apiPrefix/ping",
    RouteSchema(
      summary = Some("Ping"),
      description = Some("Simple ping endpoint for connectivity check"),
      tags = List("Health"),
      responses = Map(
        200 -> ApiResponse("Pong", Schematic[PingResponse])
      )
    )
  )
  def ping(r: ValidatedRequest): Response[String] = {
    Response(
      write(PingResponse()),
      200,
      jsonHeaders
    )
  }

  /** Health check endpoint for monitoring systems */
  @Web.get(
    s"$apiPrefix/health",
    RouteSchema(
      summary = Some("Health check"),
      description = Some("Health check endpoint for monitoring systems"),
      tags = List("Health"),
      responses = Map(
        200 -> ApiResponse("Service is healthy", Schematic[StatusResponse])
      )
    )
  )
  def health(r: ValidatedRequest): Response[String] = {
    Response(
      write(StatusResponse("ok")),
      200,
      jsonHeaders
    )
  }

  /** OpenAPI specification endpoint */
  @Web.swagger(
    "/openapi",
    OpenAPIConfig(
      title = "Arcata API",
      summary = Some("ETL service for job ingestion and enrichment"),
      description = "Specialized backend API for job URL parsing, normalization, and enrichment using AI.",
      version = "1.0.0"
    )
  )
  def openapi(): String = ""

  initialize()
}
