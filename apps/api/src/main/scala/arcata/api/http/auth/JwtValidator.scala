package arcata.api.http.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import upickle.default.*

import scala.util.{Failure, Success, Try}

/** Result of JWT validation. */
sealed trait JwtValidationResult

object JwtValidationResult:
  /** Token is valid and contains the user's profile ID. */
  final case class Valid(profileId: String, claims: JwtClaims) extends JwtValidationResult

  /** Token is invalid or expired. */
  final case class Invalid(reason: String) extends JwtValidationResult

/** Parsed JWT claims from a Supabase token. */
final case class JwtClaims(
    sub: String,
    email: Option[String],
    role: String,
    aud: String,
    exp: Long,
    iat: Long
) derives ReadWriter

/**
 * Validates Supabase JWT tokens.
 *
 * @param jwtSecret
 *   The Supabase JWT secret for HS256 validation
 */
final class JwtValidator(jwtSecret: String):
  private val algorithm = Algorithm.HMAC256(jwtSecret)
  private val verifier = JWT.require(algorithm).build()

  /**
   * Validate a JWT token and extract the user's profile ID.
   *
   * @param token
   *   The raw JWT token (without "Bearer " prefix)
   * @return
   *   Validation result with profile ID or error reason
   */
  def validate(token: String): JwtValidationResult = {
    Try(verifier.verify(token)) match
      case Success(decodedJwt) =>
        val claims = JwtClaims(
          sub = decodedJwt.getSubject,
          email = Option(decodedJwt.getClaim("email").asString()),
          role = Option(decodedJwt.getClaim("role").asString()).getOrElse("authenticated"),
          aud = Option(decodedJwt.getAudience).map(_.get(0)).getOrElse(""),
          exp = decodedJwt.getExpiresAt.getTime / 1000,
          iat = decodedJwt.getIssuedAt.getTime / 1000
        )
        JwtValidationResult.Valid(claims.sub, claims)

      case Failure(exception: JWTVerificationException) =>
        JwtValidationResult.Invalid(s"Token validation failed: ${exception.getMessage}")

      case Failure(exception) =>
        JwtValidationResult.Invalid(s"Unexpected error: ${exception.getMessage}")
  }

  /**
   * Validate a token from an Authorization header value.
   *
   * @param authHeader
   *   The full Authorization header value (e.g., "Bearer eyJ...")
   * @return
   *   Validation result
   */
  def validateAuthHeader(authHeader: String): JwtValidationResult = {
    val bearerPrefix = "Bearer "
    if authHeader.startsWith(bearerPrefix) then validate(authHeader.drop(bearerPrefix.length))
    else JwtValidationResult.Invalid("Authorization header must start with 'Bearer '")
  }

object JwtValidator:
  /**
   * Create a JwtValidator from configuration.
   *
   * @param jwtSecret
   *   The Supabase JWT secret
   */
  def apply(jwtSecret: String): JwtValidator =
    new JwtValidator(jwtSecret)
