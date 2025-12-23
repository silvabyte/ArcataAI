package arcata.api.http.auth

import cask.model.{Request, Response}
import cask.router.{RawDecorator, Result}

/**
 * Authentication decorator that validates requests using configured auth methods.
 *
 * This decorator is self-contained - it uses AuthConfig internally to access JWT validation and API
 * key configuration. Routes can simply import and use this decorator without passing dependencies.
 *
 * Usage:
 * {{{
 * import arcata.api.http.auth.{authenticated, AuthType, AuthenticatedRequest}
 *
 * @authenticated()                                      // JWT only (default)
 * @authenticated(Vector(AuthType.JWT, AuthType.ApiKey)) // Either JWT or API key
 * @cask.post("/api/v1/protected")
 * def protectedEndpoint(authReq: AuthenticatedRequest) = ...
 * }}}
 *
 * The authenticated request is passed to the handler as `authReq: AuthenticatedRequest`.
 *
 * @param allowedTypes
 *   Authentication methods to accept. Defaults to JWT only. The decorator tries each type in order
 *   until one succeeds.
 */
class authenticated(
    allowedTypes: Vector[AuthType] = Vector(AuthType.JWT)
) extends RawDecorator:

  private val jsonHeaders = Seq("Content-Type" -> "application/json")

  override def wrapFunction(
      ctx: Request,
      delegate: Delegate
  ): Result[Response.Raw] =
    // Try each allowed auth type until one succeeds
    val results = allowedTypes.map(tryAuth(_, ctx))

    results.collectFirst { case s: AuthResult.Success => s } match
      case Some(AuthResult.Success(authReq)) =>
        delegate(ctx, Map("authReq" -> authReq))

      case None =>
        // All auth methods failed - return appropriate error
        val failure = results
          .collectFirst { case f: AuthResult.Failure => f }
          .getOrElse(AuthResult.Failure("No authentication provided"))

        Result.Success(
          Response(
            data = s"""{"error": "${failure.reason}"}""",
            statusCode = failure.statusCode,
            headers = jsonHeaders
          )
        )

  private def tryAuth(authType: AuthType, ctx: Request): AuthResult =
    authType match
      case AuthType.JWT    => tryJwtAuth(ctx)
      case AuthType.ApiKey => ApiKeyValidator.validate(ctx)

  private def tryJwtAuth(ctx: Request): AuthResult =
    ctx.headers.get("authorization").flatMap(_.headOption) match
      case None =>
        AuthResult.Failure("Missing Authorization header")

      case Some(header) =>
        AuthConfig.jwtValidator.validateAuthHeader(header) match
          case JwtValidationResult.Valid(profileId, claims) =>
            AuthResult.Success(
              AuthenticatedRequest(
                profileId = profileId,
                claims = Some(claims),
                authType = AuthType.JWT,
                request = ctx
              )
            )

          case JwtValidationResult.Invalid(reason) =>
            AuthResult.Failure(s"Invalid token: $reason")
