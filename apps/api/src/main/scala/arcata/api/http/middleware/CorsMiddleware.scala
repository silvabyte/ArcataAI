package arcata.api.http.middleware

import cask.model.Response

/**
 * CORS configuration for the API.
 */
final case class CorsConfig(
    allowedOrigins: List[String],
    allowedMethods: String = "GET, POST, PUT, DELETE, OPTIONS, PATCH",
    allowedHeaders: String = "Content-Type, Authorization, X-Requested-With",
    maxAge: String = "86400"
):
  /** Check if an origin is allowed */
  def isAllowed(origin: String): Boolean =
    allowedOrigins.contains(origin) || allowedOrigins.contains("*")

  /** Get CORS headers for an allowed origin */
  def headersFor(origin: String): Seq[(String, String)] = {
    if isAllowed(origin) then
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
    getOrigin(request).filter(config.isAllowed) match
      case Some(origin) => Response("", statusCode = 204, headers = config.headersFor(origin))
      case None => Response("", statusCode = 204, headers = Seq.empty)
  }

  // Handle OPTIONS for API paths
  @cask.route("/api/v1/jobs/ingest", methods = Seq("options"))
  def optionsJobsIngest(request: cask.Request): Response[String] = preflightResponse(request)

  @cask.route("/api/v1/ping", methods = Seq("options"))
  def optionsPing(request: cask.Request): Response[String] = preflightResponse(request)

  @cask.route("/api/v1/health", methods = Seq("options"))
  def optionsHealth(request: cask.Request): Response[String] = preflightResponse(request)

  initialize()

object CorsRoutes:
  def apply(config: CorsConfig): CorsRoutes = new CorsRoutes(config)
