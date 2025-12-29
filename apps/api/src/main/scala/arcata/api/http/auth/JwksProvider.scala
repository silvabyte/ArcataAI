package arcata.api.http.auth

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.{ECParameterSpec, ECPoint, ECPublicKeySpec}
import java.util.Base64

import scala.util.{Failure, Success, Try}

import scribe.Logging
import upickle.default.*

/** A single key from a JWKS response. */
final case class JwkKey(
    kid: String,
    kty: String,
    alg: String,
    crv: String,
    x: String,
    y: String
) derives ReadWriter

/** JWKS response containing public keys. */
final case class JwksResponse(keys: Seq[JwkKey]) derives ReadWriter

/** Error when JWKS operations fail. */
final case class JwksError(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

/**
 * Fetches and parses JWKS (JSON Web Key Set) from Supabase.
 *
 * Simple implementation - fetches fresh keys on each call (no caching).
 */
class JwksProvider(jwksUrl: String) extends Logging:

  logger.info(s"JwksProvider initialized with URL: $jwksUrl")

  /** Fetch keys and find the one matching the given kid. */
  def getKey(kid: String): Either[JwksError, ECPublicKey] =
    fetchKeys().flatMap { keys =>
      logger.debug(s"JWKS contains ${keys.size} keys: ${keys.map(_.kid).mkString(", ")}")
      keys.find(_.kid == kid) match
        case Some(jwk) =>
          logger.debug(s"Found matching key for kid: $kid")
          parseEcPublicKey(jwk)
        case None =>
          logger.warn(s"Key with kid '$kid' not found in JWKS. Available kids: ${keys.map(_.kid).mkString(", ")}")
          Left(JwksError(s"Key with kid '$kid' not found in JWKS"))
    }

  /** Fetch all keys from the JWKS endpoint. */
  def fetchKeys(): Either[JwksError, Seq[JwkKey]] =
    logger.debug(s"Fetching JWKS from: $jwksUrl")
    Try(requests.get(jwksUrl, readTimeout = 5000, connectTimeout = 5000)) match
      case Success(response) if response.is2xx =>
        logger.debug(s"JWKS fetch successful, parsing response...")
        Try(read[JwksResponse](response.text())) match
          case Success(jwks) =>
            logger.debug(s"Parsed ${jwks.keys.size} keys from JWKS")
            Right(jwks.keys)
          case Failure(e) =>
            logger.error(s"Failed to parse JWKS response: ${e.getMessage}")
            Left(JwksError(s"Failed to parse JWKS: ${e.getMessage}", Some(e)))
      case Success(response) =>
        logger.error(s"JWKS endpoint returned non-2xx status: ${response.statusCode}")
        Left(JwksError(s"JWKS endpoint returned ${response.statusCode}"))
      case Failure(e) =>
        logger.error(s"Failed to fetch JWKS: ${e.getMessage}", e)
        Left(JwksError(s"Failed to fetch JWKS: ${e.getMessage}", Some(e)))

  /** Convert a JWK to an ECPublicKey. */
  private def parseEcPublicKey(jwk: JwkKey): Either[JwksError, ECPublicKey] =
    Try {
      require(jwk.kty == "EC", s"Expected kty=EC, got ${jwk.kty}")
      require(jwk.crv == "P-256", s"Expected crv=P-256, got ${jwk.crv}")

      // Decode base64url-encoded coordinates
      val decoder = Base64.getUrlDecoder
      val xBytes = decoder.decode(jwk.x)
      val yBytes = decoder.decode(jwk.y)

      val x = new BigInteger(1, xBytes)
      val y = new BigInteger(1, yBytes)

      // Get P-256 curve parameters
      val ecParams = java.security.AlgorithmParameters.getInstance("EC")
      ecParams.init(new java.security.spec.ECGenParameterSpec("secp256r1"))
      val params = ecParams.getParameterSpec(classOf[ECParameterSpec])

      val point = new ECPoint(x, y)
      val spec = new ECPublicKeySpec(point, params)

      KeyFactory.getInstance("EC").generatePublic(spec).asInstanceOf[ECPublicKey]
    } match
      case Success(key) => Right(key)
      case Failure(e)   => Left(JwksError(s"Failed to parse EC key: ${e.getMessage}", Some(e)))

object JwksProvider:
  def apply(jwksUrl: String): JwksProvider = new JwksProvider(jwksUrl)

  /** Derive JWKS URL from Supabase project URL. */
  def jwksUrlFor(supabaseUrl: String): String =
    s"$supabaseUrl/auth/v1/.well-known/jwks.json"
