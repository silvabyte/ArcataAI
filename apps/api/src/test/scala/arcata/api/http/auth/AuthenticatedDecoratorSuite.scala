package arcata.api.http.auth

import cask.model.{Request, Response}
import cask.router.Result
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.undertow.server.HttpServerExchange
import io.undertow.util.{HeaderMap, HttpString}
import utest.*

import java.io.ByteArrayOutputStream
import java.util.Date

/**
 * Tests for the authenticated decorator.
 *
 * Note: JWT validation requires SUPABASE_JWT_SECRET to be set. API key validation requires
 * API_KEYS to be set.
 */
object AuthenticatedDecoratorSuite extends TestSuite:
  // Test secret - must match what's in SUPABASE_JWT_SECRET for tests to pass
  private val testSecret =
    sys.env.getOrElse("SUPABASE_JWT_SECRET", "test-jwt-secret-for-unit-tests")

  // Create a mock request with specified headers using Undertow's exchange
  private def mockRequest(headers: Map[String, String]): Request = {
    val exchange = new HttpServerExchange(null)
    val headerMap = exchange.getRequestHeaders
    headers.foreach { case (k, v) =>
      headerMap.put(new HttpString(k), v)
    }
    Request(exchange, Seq.empty, Map.empty)
  }

  // Helper to extract response body as string
  private def responseBody(response: Response.Raw): String = {
    val out = new ByteArrayOutputStream()
    response.data.write(out)
    out.toString("UTF-8")
  }

  // Helper to create a valid JWT
  private def createToken(
      sub: String = "user-123",
      email: Option[String] = Some("test@example.com")
  ): String = {
    val algorithm = Algorithm.HMAC256(testSecret)
    val now = System.currentTimeMillis()

    val builder = JWT
      .create()
      .withSubject(sub)
      .withClaim("role", "authenticated")
      .withAudience("authenticated")
      .withIssuedAt(new Date(now))
      .withExpiresAt(new Date(now + 3600000))

    email.foreach(e => builder.withClaim("email", e))
    builder.sign(algorithm)
  }

  val tests = Tests {
    test("authenticated decorator") {
      test("returns 401 when no auth headers present") {
        val decorator = new authenticated()
        val request = mockRequest(Map.empty)

        // Create a simple delegate that returns success
        val delegate: decorator.Delegate = (_, _) =>
          Result.Success(Response("ok", 200, Seq.empty))

        val result = decorator.wrapFunction(request, delegate)

        result match
          case Result.Success(response) =>
            assert(response.statusCode == 401)
            assert(responseBody(response).contains("error"))
          case _ =>
            throw new java.lang.AssertionError("Expected Result.Success with 401")
      }

      test("returns 401 for invalid JWT") {
        // Skip if JWT secret not configured (can't validate tokens)
        if sys.env.get("SUPABASE_JWT_SECRET").isEmpty then
          println("Skipping - SUPABASE_JWT_SECRET not set")
        else {
          val decorator = new authenticated()
          val request = mockRequest(Map("Authorization" -> "Bearer invalid-token"))

          val delegate: decorator.Delegate = (_, _) =>
            Result.Success(Response("ok", 200, Seq.empty))

          val result = decorator.wrapFunction(request, delegate)

          result match
            case Result.Success(response) =>
              assert(response.statusCode == 401)
              assert(responseBody(response).contains("Invalid token"))
            case _ =>
              throw new java.lang.AssertionError("Expected Result.Success with 401")
        }
      }

      test("passes AuthenticatedRequest to delegate for valid JWT") {
        // Skip if JWT secret not configured
        if sys.env.get("SUPABASE_JWT_SECRET").isEmpty then
          println("Skipping - SUPABASE_JWT_SECRET not set")
        else {
          // Only run if we have the right secret configured
          val jwtSecretMatches =
            sys.env.get("SUPABASE_JWT_SECRET").contains(testSecret)

          if !jwtSecretMatches then println("Skipping - JWT secret mismatch")
          else {
            val decorator = new authenticated()
            val token = createToken(sub = "test-user-456")
            val request = mockRequest(Map("Authorization" -> s"Bearer $token"))

            var capturedAuthReq: Option[AuthenticatedRequest] = None

            val delegate: decorator.Delegate = (_, args) => {
              capturedAuthReq = args.get("authReq").map(_.asInstanceOf[AuthenticatedRequest])
              Result.Success(Response("ok", 200, Seq.empty))
            }

            val result = decorator.wrapFunction(request, delegate)

            result match
              case Result.Success(response) =>
                assert(response.statusCode == 200)
                assert(capturedAuthReq.isDefined)
                assert(capturedAuthReq.get.profileId == "test-user-456")
                assert(capturedAuthReq.get.authType == AuthType.JWT)
              case _ =>
                throw new java.lang.AssertionError("Expected successful delegation")
          }
        }
      }
    }

    test("authenticated with API key") {
      test("returns 401 for invalid API key when only ApiKey auth allowed") {
        val decorator = new authenticated(Vector(AuthType.ApiKey))
        val request = mockRequest(Map("X-API-Key" -> "invalid-key"))

        val delegate: decorator.Delegate = (_, _) =>
          Result.Success(Response("ok", 200, Seq.empty))

        val result = decorator.wrapFunction(request, delegate)

        result match
          case Result.Success(response) =>
            assert(response.statusCode == 401)
          case _ =>
            throw new java.lang.AssertionError("Expected Result.Success with 401")
      }

      test("accepts valid API key when ApiKey auth allowed") {
        val apiKeysConfigured = sys.env.get("API_KEYS").exists(_.nonEmpty)

        if !apiKeysConfigured then println("Skipping - API_KEYS not set")
        else {
          val validKey = AuthConfig.validApiKeys.head
          val decorator = new authenticated(Vector(AuthType.ApiKey))
          val request = mockRequest(Map("X-API-Key" -> validKey))

          var capturedAuthReq: Option[AuthenticatedRequest] = None

          val delegate: decorator.Delegate = (_, args) => {
            capturedAuthReq = args.get("authReq").map(_.asInstanceOf[AuthenticatedRequest])
            Result.Success(Response("ok", 200, Seq.empty))
          }

          val result = decorator.wrapFunction(request, delegate)

          result match
            case Result.Success(response) =>
              assert(response.statusCode == 200)
              assert(capturedAuthReq.isDefined)
              assert(capturedAuthReq.get.authType == AuthType.ApiKey)
            case _ =>
              throw new java.lang.AssertionError("Expected successful delegation")
        }
      }
    }

    test("authenticated with multiple auth types") {
      test("tries JWT first when both allowed") {
        // Skip if JWT secret not configured
        if sys.env.get("SUPABASE_JWT_SECRET").isEmpty then
          println("Skipping - SUPABASE_JWT_SECRET not set")
        else {
          // JWT secret must match for this test
          val jwtSecretMatches =
            sys.env.get("SUPABASE_JWT_SECRET").contains(testSecret)

          if !jwtSecretMatches then println("Skipping - JWT secret mismatch")
          else {
            val decorator = new authenticated(Vector(AuthType.JWT, AuthType.ApiKey))
            val token = createToken(sub = "jwt-user")
            val request = mockRequest(Map("Authorization" -> s"Bearer $token"))

            var capturedAuthReq: Option[AuthenticatedRequest] = None

            val delegate: decorator.Delegate = (_, args) => {
              capturedAuthReq = args.get("authReq").map(_.asInstanceOf[AuthenticatedRequest])
              Result.Success(Response("ok", 200, Seq.empty))
            }

            val result = decorator.wrapFunction(request, delegate)

            result match
              case Result.Success(response) =>
                assert(response.statusCode == 200)
                assert(capturedAuthReq.get.authType == AuthType.JWT)
              case _ =>
                throw new java.lang.AssertionError("Expected successful delegation")
          }
        }
      }

      test("falls back to API key when JWT missing but ApiKey present") {
        val apiKeysConfigured = sys.env.get("API_KEYS").exists(_.nonEmpty)

        if !apiKeysConfigured then println("Skipping - API_KEYS not set")
        else {
          val validKey = AuthConfig.validApiKeys.head
          val decorator = new authenticated(Vector(AuthType.JWT, AuthType.ApiKey))
          val request = mockRequest(Map("X-API-Key" -> validKey))

          var capturedAuthReq: Option[AuthenticatedRequest] = None

          val delegate: decorator.Delegate = (_, args) => {
            capturedAuthReq = args.get("authReq").map(_.asInstanceOf[AuthenticatedRequest])
            Result.Success(Response("ok", 200, Seq.empty))
          }

          val result = decorator.wrapFunction(request, delegate)

          result match
            case Result.Success(response) =>
              assert(response.statusCode == 200)
              assert(capturedAuthReq.get.authType == AuthType.ApiKey)
            case _ =>
              throw new java.lang.AssertionError("Expected successful delegation")
        }
      }
    }

    test("AuthType enum") {
      test("has JWT and ApiKey values") {
        assert(AuthType.values.contains(AuthType.JWT))
        assert(AuthType.values.contains(AuthType.ApiKey))
        assert(AuthType.values.length == 2)
      }
    }

    test("AuthResult") {
      test("Success holds AuthenticatedRequest") {
        val mockReq = mockRequest(Map.empty)
        val authReq = AuthenticatedRequest("user-1", None, AuthType.ApiKey, mockReq)
        val result = AuthResult.Success(authReq)

        assert(result.request == authReq)
      }

      test("Failure has default status code 401") {
        val result = AuthResult.Failure("test error")
        assert(result.statusCode == 401)
      }

      test("Failure can have custom status code") {
        val result = AuthResult.Failure("forbidden", 403)
        assert(result.statusCode == 403)
      }
    }
  }
