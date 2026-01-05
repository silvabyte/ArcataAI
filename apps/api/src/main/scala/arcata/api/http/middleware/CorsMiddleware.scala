package arcata.api.http.middleware

import arcata.api.logging.Log
import cask.model.Response

/**
 * CORS configuration for the API.
 */
final case class CorsConfig private (
    allowedOrigins: List[String],
    allowedMethods: String = "GET, POST, PUT, DELETE, OPTIONS, PATCH",
    allowedHeaders: String = "Content-Type, Authorization, X-Requested-With, X-API-Key",
    maxAge: String = "86400"
):
  /** Check if an origin is allowed */
  def isAllowed(origin: String): Boolean =
    allowedOrigins.contains(origin) || allowedOrigins.contains("*")

  /** Get CORS headers for an allowed origin */
  def headersFor(origin: String): Seq[(String, String)] = {
    val allowed = isAllowed(origin)
    Log.debug(
      "CORS headers check",
      Map(
        "origin" -> origin,
        "allowed" -> allowed.toString,
        "allowedOrigins" -> allowedOrigins.mkString(", ")
      )
    )
    if allowed then
      Seq(
        "Access-Control-Allow-Origin" -> origin,
        "Access-Control-Allow-Methods" -> allowedMethods,
        "Access-Control-Allow-Headers" -> allowedHeaders,
        "Access-Control-Max-Age" -> maxAge,
        "Access-Control-Allow-Credentials" -> "true"
      )
    else Seq.empty
  }

/**
 * Simple CORS routes that handle OPTIONS preflight requests.
 */
class CorsRoutes(config: CorsConfig) extends cask.Routes:

  private def getOrigin(request: cask.Request): Option[String] =
    request.headers.get("origin").flatMap(_.headOption)

  private def preflightResponse(request: cask.Request): Response[String] = {
    val origin = getOrigin(request)
    val path = request.exchange.getRequestPath
    Log.debug(
      "CORS preflight request",
      Map(
        "path" -> path,
        "origin" -> origin.getOrElse("(none)"),
        "allowed" -> origin.exists(config.isAllowed).toString,
        "allowedOrigins" -> config.allowedOrigins.mkString(", ")
      )
    )
    origin.filter(config.isAllowed) match
      case Some(o) => Response("", statusCode = 204, headers = config.headersFor(o))
      case None => Response("", statusCode = 204, headers = Seq.empty)
  }

  // Handle OPTIONS preflight for API paths
  // Each route that accepts POST/PUT/DELETE needs an OPTIONS handler for CORS preflight

  // Jobs
  @cask.route("/api/v1/jobs/ingest", methods = Seq("options"))
  def optionsJobsIngest(request: cask.Request): Response[String] = preflightResponse(request)

  // Resumes
  @cask.route("/api/v1/resumes/parse", methods = Seq("options"))
  def optionsResumesParse(request: cask.Request): Response[String] = preflightResponse(request)

  // Cron endpoints
  @cask.route("/api/v1/cron/check-job-status", methods = Seq("options"))
  def optionsCronJobStatus(request: cask.Request): Response[String] = preflightResponse(request)

  @cask.route("/api/v1/cron/discover-jobs", methods = Seq("options"))
  def optionsCronDiscoverJobs(request: cask.Request): Response[String] = preflightResponse(request)

  // Health/ping (GET only, but adding for completeness)
  @cask.route("/api/v1/ping", methods = Seq("options"))
  def optionsPing(request: cask.Request): Response[String] = preflightResponse(request)

  @cask.route("/api/v1/health", methods = Seq("options"))
  def optionsHealth(request: cask.Request): Response[String] = preflightResponse(request)

  initialize()

object CorsConfig:
  def apply(
      allowedOrigins: List[String],
      allowedMethods: String = "GET, POST, PUT, DELETE, OPTIONS, PATCH",
      allowedHeaders: String = "Content-Type, Authorization, X-Requested-With, X-API-Key",
      maxAge: String = "86400"
  ): CorsConfig = {
    Log.info(
      "CORS config initialized",
      Map("allowedOrigins" -> allowedOrigins.mkString(", "))
    )
    new CorsConfig(allowedOrigins, allowedMethods, allowedHeaders, maxAge)
  }

object CorsRoutes:
  def apply(config: CorsConfig): CorsRoutes = new CorsRoutes(config)
