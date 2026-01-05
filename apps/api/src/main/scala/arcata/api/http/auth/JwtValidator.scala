package arcata.api.http.auth

import java.security.interfaces.ECPublicKey

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import scribe.Logging
import upickle.default.*

/** Result of JWT validation. */
sealed trait JwtValidationResult

object JwtValidationResult:
  /** Token is valid and contains the user's profile ID. */
  final case class Valid(profileId: String, claims: JwtClaims) extends JwtValidationResult

  /** Token is invalid or expired. */
  final case class Invalid(reason: String, statusCode: Int = 401) extends JwtValidationResult

/** Parsed JWT claims from a Supabase token. */
final case class JwtClaims(
  sub: String,
  email: Option[String],
  role: String,
  aud: String,
  exp: Long,
  iat: Long,
) derives ReadWriter

/**
 * Validates Supabase JWT tokens using JWKS (ES256).
 *
 * @param jwksProvider
 *   Provider for fetching public keys from Supabase JWKS endpoint
 */
final class JwtValidator(jwksProvider: JwksProvider) extends Logging:

  /**
   * Validate a JWT token and extract the user's profile ID.
   *
   * @param token
   *   The raw JWT token (without "Bearer " prefix)
   * @return
   *   Validation result with profile ID or error reason
   */
  def validate(token: String): JwtValidationResult = {
    logger.debug(s"Validating JWT token (length: ${token.length})")

    // First, decode without verification to get the kid
    Try(JWT.decode(token)) match
      case Failure(e) =>
        logger.warn(s"Failed to decode JWT: ${e.getMessage}")
        JwtValidationResult.Invalid(s"Malformed token: ${e.getMessage}")

      case Success(decoded) =>
        val kid = Option(decoded.getKeyId)
        val alg = Option(decoded.getAlgorithm)
        logger.debug(s"JWT decoded - alg: $alg, kid: $kid, sub: ${decoded.getSubject}")

        // Verify algorithm is ES256
        alg match
          case Some("ES256") => validateWithKey(token, kid)
          case Some(other) =>
            logger.warn(s"Unsupported JWT algorithm: $other (expected ES256)")
            JwtValidationResult.Invalid(s"Unsupported algorithm: $other (expected ES256)")
          case None =>
            logger.warn("JWT missing algorithm header")
            JwtValidationResult.Invalid("Token missing algorithm header")
  }

  private def validateWithKey(token: String, kid: Option[String]): JwtValidationResult = {
    kid match
      case None =>
        logger.warn("JWT missing kid header")
        JwtValidationResult.Invalid("Token missing kid header")

      case Some(keyId) =>
        logger.debug(s"Fetching JWKS key for kid: $keyId")
        jwksProvider.getKey(keyId) match
          case Left(error) if error.getMessage.contains("Failed to fetch") =>
            // JWKS endpoint unreachable - 503
            logger.error(s"JWKS fetch failed: ${error.getMessage}")
            JwtValidationResult.Invalid(
              s"Authentication service unavailable: ${error.getMessage}",
              statusCode = 503,
            )

          case Left(error) =>
            // Key not found or parse error - 401
            logger.warn(s"JWKS key lookup failed: ${error.getMessage}")
            JwtValidationResult.Invalid(error.getMessage)

          case Right(publicKey) =>
            logger.debug(s"Got public key for kid: $keyId, verifying signature...")
            verifySignature(token, publicKey)
  }

  private def verifySignature(token: String, publicKey: ECPublicKey): JwtValidationResult = {
    Try {
      // scalafix:off DisableSyntax.null
      val algorithm = Algorithm.ECDSA256(publicKey, null) // null = no private key (verify only)
      // scalafix:on DisableSyntax.null
      val verifier = JWT.require(algorithm).build()
      verifier.verify(token)
    } match
      case Success(verified) =>
        val claims = JwtClaims(
          sub = verified.getSubject,
          email = Option(verified.getClaim("email").asString()),
          role = Option(verified.getClaim("role").asString()).getOrElse("authenticated"),
          aud = Option(verified.getAudience).flatMap(_.asScala.headOption).getOrElse(""),
          exp = verified.getExpiresAt.getTime / 1000,
          iat = verified.getIssuedAt.getTime / 1000,
        )
        logger.info(s"JWT validated successfully for user: ${claims.sub} (email: ${claims.email.getOrElse("none")})")
        JwtValidationResult.Valid(claims.sub, claims)

      case Failure(e: JWTVerificationException) =>
        logger.warn(s"JWT signature verification failed: ${e.getMessage}")
        JwtValidationResult.Invalid(s"Token validation failed: ${e.getMessage}")

      case Failure(e) =>
        logger.error(s"Unexpected error during JWT verification: ${e.getMessage}", e)
        JwtValidationResult.Invalid(s"Unexpected error: ${e.getMessage}")
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
  def apply(jwksProvider: JwksProvider): JwtValidator = new JwtValidator(jwksProvider)
