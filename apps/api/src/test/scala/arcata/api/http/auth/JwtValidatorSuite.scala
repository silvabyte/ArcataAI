package arcata.api.http.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import utest.*

import java.util.Date

object JwtValidatorSuite extends TestSuite:
  // Test secret for JWT signing
  private val testSecret = "test-jwt-secret-that-is-long-enough"
  private val validator = JwtValidator(testSecret)

  // Helper to create a valid JWT
  private def createToken(
      sub: String = "user-123",
      email: Option[String] = Some("test@example.com"),
      role: String = "authenticated",
      expiresIn: Long = 3600000 // 1 hour
  ): String =
    val algorithm = Algorithm.HMAC256(testSecret)
    val now = System.currentTimeMillis()

    val builder = JWT
      .create()
      .withSubject(sub)
      .withClaim("role", role)
      .withAudience("authenticated")
      .withIssuedAt(new Date(now))
      .withExpiresAt(new Date(now + expiresIn))

    email.foreach(e => builder.withClaim("email", e))
    builder.sign(algorithm)

  val tests = Tests {
    test("validate") {
      test("returns Valid for correct token") {
        val token = createToken()
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Valid(profileId, claims) =>
            assert(profileId == "user-123")
            assert(claims.sub == "user-123")
            assert(claims.email == Some("test@example.com"))
            assert(claims.role == "authenticated")
          case JwtValidationResult.Invalid(reason) =>
            throw new java.lang.AssertionError(s"Expected Valid, got Invalid: $reason")
      }

      test("returns Valid for token without email") {
        val token = createToken(email = None)
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Valid(_, claims) =>
            assert(claims.email == None)
          case JwtValidationResult.Invalid(reason) =>
            throw new java.lang.AssertionError(s"Expected Valid, got Invalid: $reason")
      }

      test("returns Invalid for expired token") {
        val token = createToken(expiresIn = -1000) // Already expired
        val result = validator.validate(token)

        result match
          case JwtValidationResult.Invalid(reason) =>
            assert(reason.contains("Token validation failed"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for expired token")
      }

      test("returns Invalid for wrong signature") {
        val wrongSecret = "wrong-secret-key-that-is-long-enough"
        val wrongAlgorithm = Algorithm.HMAC256(wrongSecret)
        val token = JWT
          .create()
          .withSubject("user-123")
          .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
          .sign(wrongAlgorithm)

        val result = validator.validate(token)

        result match
          case JwtValidationResult.Invalid(reason) =>
            assert(reason.contains("Token validation failed"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for wrong signature")
      }

      test("returns Invalid for malformed token") {
        val result = validator.validate("not-a-valid-jwt")

        result match
          case JwtValidationResult.Invalid(_) => () // Expected
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for malformed token")
      }
    }

    test("validateAuthHeader") {
      test("strips Bearer prefix and validates") {
        val token = createToken()
        val result = validator.validateAuthHeader(s"Bearer $token")

        result match
          case JwtValidationResult.Valid(profileId, _) =>
            assert(profileId == "user-123")
          case JwtValidationResult.Invalid(reason) =>
            throw new java.lang.AssertionError(s"Expected Valid, got Invalid: $reason")
      }

      test("returns Invalid without Bearer prefix") {
        val token = createToken()
        val result = validator.validateAuthHeader(token)

        result match
          case JwtValidationResult.Invalid(reason) =>
            assert(reason.contains("Bearer"))
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid without Bearer prefix")
      }

      test("returns Invalid for empty Bearer token") {
        val result = validator.validateAuthHeader("Bearer ")

        result match
          case JwtValidationResult.Invalid(_) => () // Expected
          case JwtValidationResult.Valid(_, _) =>
            throw new java.lang.AssertionError("Expected Invalid for empty token")
      }
    }
  }
