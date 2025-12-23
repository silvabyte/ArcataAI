package arcata.api.http.middleware

import cask.model.{Request, Response}
import cask.router.{RawDecorator, Result}

/**
 * CORS configuration for the API.
 *
 * @param allowedOrigins
 *   List of allowed origins (e.g., "http://localhost:4201")
 * @param allowedMethods
 *   HTTP methods allowed for CORS requests
 * @param allowedHeaders
 *   Headers allowed in CORS requests
 * @param maxAge
 *   How long browsers should cache preflight responses (in seconds)
 */
final case class CorsConfig(
    allowedOrigins: List[String],
    allowedMethods: List[String] = List("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"),
    allowedHeaders: List[String] = List("Content-Type", "Authorization", "X-Requested-With"),
    maxAge: Int = 86400
)

/**
 * CORS decorator that adds Cross-Origin Resource Sharing headers to responses.
 *
 * This decorator checks the Origin header against allowed origins and adds appropriate CORS
 * headers. If the origin is not allowed, no CORS headers are added.
 */
class cors(config: CorsConfig) extends RawDecorator:

  private val methodsStr = config.allowedMethods.mkString(", ")
  private val headersStr = config.allowedHeaders.mkString(", ")

  override def wrapFunction(
      ctx: Request,
      delegate: Delegate
  ): Result[Response.Raw] = {
    val origin = ctx.headers.get("origin").flatMap(_.headOption)

    // Check if origin is allowed
    val allowedOrigin = origin.filter(o => config.allowedOrigins.contains(o) || config.allowedOrigins.contains("*"))

    // Build CORS headers
    val corsHeaders: Seq[(String, String)] = allowedOrigin match
      case Some(o) =>
        Seq(
          "Access-Control-Allow-Origin" -> o,
          "Access-Control-Allow-Methods" -> methodsStr,
          "Access-Control-Allow-Headers" -> headersStr,
          "Access-Control-Max-Age" -> config.maxAge.toString,
          "Access-Control-Allow-Credentials" -> "true"
        )
      case None =>
        Seq.empty

    // Execute the delegate and add CORS headers to response
    delegate(ctx, Map.empty) match
      case Result.Success(response) =>
        Result.Success(
          response.copy(headers = response.headers ++ corsHeaders)
        )
      case other => other
  }

/**
 * Routes for handling CORS preflight (OPTIONS) requests.
 *
 * This should be added to allRoutes to handle preflight requests for all paths.
 */
class CorsPreflightRoutes(config: CorsConfig) extends cask.Routes:

  private val methodsStr = config.allowedMethods.mkString(", ")
  private val headersStr = config.allowedHeaders.mkString(", ")

  /**
   * Handle OPTIONS preflight requests for any path.
   *
   * The subpath captures the entire request path.
   */
  @cask.options("/:path", subpath = true)
  def handlePreflight(path: String, request: Request): Response[String] = {
    val origin = request.headers.get("origin").flatMap(_.headOption)

    val allowedOrigin =
      origin.filter(o => config.allowedOrigins.contains(o) || config.allowedOrigins.contains("*"))

    val corsHeaders: Seq[(String, String)] = allowedOrigin match
      case Some(o) =>
        Seq(
          "Access-Control-Allow-Origin" -> o,
          "Access-Control-Allow-Methods" -> methodsStr,
          "Access-Control-Allow-Headers" -> headersStr,
          "Access-Control-Max-Age" -> config.maxAge.toString,
          "Access-Control-Allow-Credentials" -> "true"
        )
      case None =>
        Seq.empty

    Response("", statusCode = 204, headers = corsHeaders)
  }

  initialize()

object CorsPreflightRoutes:
  def apply(config: CorsConfig): CorsPreflightRoutes =
    new CorsPreflightRoutes(config)
