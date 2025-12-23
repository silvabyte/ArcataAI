package arcata.api.http.auth

import cask.model.Request

/** Supported authentication methods. */
enum AuthType:
  /** JWT token authentication (user auth via Supabase). */
  case JWT

  /** API key authentication (service-to-service auth). */
  case ApiKey

/** Result of an authentication attempt. */
sealed trait AuthResult

object AuthResult:
  /** Authentication succeeded. */
  final case class Success(request: AuthenticatedRequest) extends AuthResult

  /** Authentication failed. */
  final case class Failure(reason: String, statusCode: Int = 401) extends AuthResult

/**
 * Authenticated request context with user/service identity.
 *
 * This is passed to route handlers via the authentication decorator. Contains the validated
 * identity and the original request.
 *
 * @param profileId
 *   The user's profile ID (from JWT sub claim) or service identifier for API key auth
 * @param claims
 *   The parsed JWT claims (None for API key auth)
 * @param authType
 *   How the request was authenticated
 * @param request
 *   The original HTTP request
 */
final case class AuthenticatedRequest(
    profileId: String,
    claims: Option[JwtClaims],
    authType: AuthType,
    request: Request
)
