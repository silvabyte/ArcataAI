package arcata.api.http.auth

import java.security.{KeyPair, KeyPairGenerator}
import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.util.Date

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import utest.*

object JwtValidatorSuite extends TestSuite:
  // Generate test EC key pair (P-256)
  private val keyPair: KeyPair = {
    val generator = KeyPairGenerator.getInstance("EC")
    generator.initialize(256)
    generator.generateKeyPair()
  }

  private val testPublicKey = keyPair.getPublic.asInstanceOf[ECPublicKey]
  private val testPrivateKey = keyPair.getPrivate.asInstanceOf[ECPrivateKey]
  private val testKid = "test-key-id"

  // Mock JwksProvider that returns our test key
  private class TestJwksProvider extends JwksProvider("http://test"):
    override def getKey(kid: String): Either[JwksError, ECPublicKey] = {
      if kid == testKid then Right(testPublicKey)
      else Left(JwksError(s"Key with kid '$kid' not found in JWKS"))
    }

  // Mock that simulates JWKS endpoint being down
  private class UnavailableJwksProvider extends JwksProvider("http://test"):
    override def getKey(kid: String): Either[JwksError, ECPublicKey] =
      Left(JwksError("Failed to fetch JWKS: Connection refused"))

  private val validator = JwtValidator(TestJwksProvider())

  // Helper to create a valid ES256 JWT
  private def createToken(
    sub: String = "user-123",
    email: Option[String] = Some("test@example.com"),
    role: String = "authenticated",
    expiresIn: Long = 3600000,
    kid: String = testKid,
  ): String = {
    val algorithm = Algorithm.ECDSA256(testPublicKey, testPrivateKey)
    val now = System.currentTimeMillis()

    val builder = JWT
      .create()
      .withKeyId(kid)
      .withSubject(sub)
      .withClaim("role", role)
      .withAudience("authenticated")
      .withIssuedAt(new Date(now))
      .withExpiresAt(new Date(now + expiresIn))

    email.foreach(e => builder.withClaim("email", e))
    builder.sign(algorithm)
  }

  val tests = Tests {
    test("validate") {
      test("returns Valid for correct ES256 token") {
        val token = createToken()
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Valid(profileId, claims) =>
            assert(profileId == "user-123")
            assert(claims.sub == "user-123")
            assert(claims.email == Some("test@example.com"))
            assert(claims.role == "authenticated")
          case JwtValidationResult.Invalid(reason, _) =>
            throw new java.lang.AssertionError(
              s"Expected Valid, got Invalid: $reason"
            ) // scalafix:ok DisableSyntax.throw
      }

      test("returns Valid for token without email") {
        val token = createToken(email = None)
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Valid(_, claims) =>
            assert(claims.email == None)
          case JwtValidationResult.Invalid(reason, _) =>
            throw new java.lang.AssertionError(
              s"Expected Valid, got Invalid: $reason"
            ) // scalafix:ok DisableSyntax.throw
      }

      test("returns Invalid for expired token") {
        val token = createToken(expiresIn = -1000)
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Invalid(reason, statusCode) =>
            assert(reason.contains("Token validation failed"))
            assert(statusCode == 401)
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for expired token") // scalafix:ok DisableSyntax.throw
      }

      test("returns Invalid for unknown kid") {
        val token = createToken(kid = "unknown-key-id")
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Invalid(reason, _) =>
            assert(reason.contains("not found"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for unknown kid") // scalafix:ok DisableSyntax.throw
      }

      test("returns Invalid for malformed token") {
        val result = validator.validate("not-a-valid-jwt")

        result match
          case JwtValidationResult.Invalid(reason, _) =>
            assert(reason.contains("Malformed"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError(
              "Expected Invalid for malformed token"
            ) // scalafix:ok DisableSyntax.throw
      }

      test("returns 503 when JWKS unavailable") {
        val unavailableValidator = JwtValidator(UnavailableJwksProvider())
        val token = createToken()
        val result = unavailableValidator.validate(token)

        result match
          case JwtValidationResult.Invalid(reason, statusCode) =>
            assert(statusCode == 503)
            assert(reason.contains("unavailable"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError(
              "Expected Invalid when JWKS unavailable"
            ) // scalafix:ok DisableSyntax.throw
      }

      test("rejects HS256 tokens") {
        // Create an HS256 token (legacy format)
        val hs256Token = JWT
          .create()
          .withSubject("user-123")
          .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
          .sign(Algorithm.HMAC256("some-secret"))

        val result = validator.validate(hs256Token)

        result match
          case JwtValidationResult.Invalid(reason, _) =>
            assert(reason.contains("Unsupported algorithm") || reason.contains("HS256"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for HS256 token") // scalafix:ok DisableSyntax.throw
      }

      test("rejects token without kid header") {
        // Create ES256 token without kid
        val algorithm = Algorithm.ECDSA256(testPublicKey, testPrivateKey)
        val tokenWithoutKid = JWT
          .create()
          .withSubject("user-123")
          .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
          .sign(algorithm)

        val result = validator.validate(tokenWithoutKid)

        result match
          case JwtValidationResult.Invalid(reason, _) =>
            assert(reason.contains("kid"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError(
              "Expected Invalid for token without kid"
            ) // scalafix:ok DisableSyntax.throw
      }
    }

    test("validateAuthHeader") {
      test("strips Bearer prefix and validates") {
        val token = createToken()
        val result = validator.validateAuthHeader(s"Bearer $token")

        result match
          case JwtValidationResult.Valid(profileId, _) =>
            assert(profileId == "user-123")
          case JwtValidationResult.Invalid(reason, _) =>
            throw new java.lang.AssertionError(
              s"Expected Valid, got Invalid: $reason"
            ) // scalafix:ok DisableSyntax.throw
      }

      test("returns Invalid without Bearer prefix") {
        val token = createToken()
        val result = validator.validateAuthHeader(token)

        result match
          case JwtValidationResult.Invalid(reason, _) =>
            assert(reason.contains("Bearer"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError(
              "Expected Invalid without Bearer prefix"
            ) // scalafix:ok DisableSyntax.throw
      }

      test("returns Invalid for empty Bearer token") {
        val result = validator.validateAuthHeader("Bearer ")

        result match
          case JwtValidationResult.Invalid(_, _) => () // Expected
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for empty token") // scalafix:ok DisableSyntax.throw
      }
    }
  }
