package arcata.api.http.auth

import cask.model.Request

/**
 * Authenticated request context with user profile information.
 *
 * This is passed to route handlers via the authentication decorator. Contains the validated user
 * identity and the original request.
 *
 * @param profileId
 *   The user's Supabase profile ID (from JWT `sub` claim)
 * @param claims
 *   The parsed JWT claims
 * @param request
 *   The original HTTP request
 */
final case class AuthenticatedRequest(
    profileId: String,
    claims: JwtClaims,
    request: Request
)
