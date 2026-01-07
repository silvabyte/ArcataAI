package arcata.api.http.auth

import cask.model.{Request, Response}
import cask.router.Result
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import utest.*

import java.io.ByteArrayOutputStream

/**
 * Tests for the authenticated decorator.
 *
 * Note: JWT validation tests are in JwtValidatorSuite with mocked EC keys. These tests focus on
 * decorator behavior and API key authentication which can be tested without external dependencies.
 */
object AuthenticatedDecoratorSuite extends TestSuite:

  // Create a mock request with specified headers using Undertow's exchange
  private def mockRequest(headers: Map[String, String]): Request = {
    // scalafix:off DisableSyntax.null
    val exchange = new HttpServerExchange(null) // Required by Undertow API for testing
    // scalafix:on DisableSyntax.null
    val headerMap = exchange.getRequestHeaders
    headers.foreach {
      case (k, v) =>
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

  val tests = Tests {
    test("authenticated decorator") {
      test("returns 401 when no auth headers present (API key auth)") {
        // Use API key auth type to avoid triggering JWKS provider
        val decorator = new authenticated(Vector(AuthType.ApiKey))
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
            throw java.lang.AssertionError("Expected Result.Success with 401") // scalafix:ok DisableSyntax.throw
      }

      test("returns 401 for invalid JWT format") {
        // JWT tests require SUPABASE_URL to be set (for JWKS provider)
        if sys.env.get("SUPABASE_URL").isEmpty then
          println("Skipping - SUPABASE_URL not set")
        else {
          val decorator = new authenticated()
          val request = mockRequest(Map("Authorization" -> "Bearer not-a-valid-jwt"))

          val delegate: decorator.Delegate = (_, _) =>
            Result.Success(Response("ok", 200, Seq.empty))

          val result = decorator.wrapFunction(request, delegate)

          result match
            case Result.Success(response) =>
              assert(response.statusCode == 401)
              assert(responseBody(response).contains("Invalid token"))
            case _ =>
              throw java.lang.AssertionError("Expected Result.Success with 401") // scalafix:ok DisableSyntax.throw
        }
      }

      test("returns 401 when Authorization header missing Bearer prefix") {
        // JWT tests require SUPABASE_URL to be set (for JWKS provider)
        if sys.env.get("SUPABASE_URL").isEmpty then
          println("Skipping - SUPABASE_URL not set")
        else {
          val decorator = new authenticated()
          val request = mockRequest(Map("Authorization" -> "some-token"))

          val delegate: decorator.Delegate = (_, _) =>
            Result.Success(Response("ok", 200, Seq.empty))

          val result = decorator.wrapFunction(request, delegate)

          result match
            case Result.Success(response) =>
              assert(response.statusCode == 401)
              assert(responseBody(response).contains("Bearer"))
            case _ =>
              throw java.lang.AssertionError("Expected Result.Success with 401") // scalafix:ok DisableSyntax.throw
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
            throw java.lang.AssertionError("Expected Result.Success with 401") // scalafix:ok DisableSyntax.throw
      }

      test("returns 401 when X-API-Key header missing") {
        val decorator = new authenticated(Vector(AuthType.ApiKey))
        val request = mockRequest(Map.empty)

        val delegate: decorator.Delegate = (_, _) =>
          Result.Success(Response("ok", 200, Seq.empty))

        val result = decorator.wrapFunction(request, delegate)

        result match
          case Result.Success(response) =>
            assert(response.statusCode == 401)
            assert(responseBody(response).contains("X-API-Key"))
          case _ =>
            throw java.lang.AssertionError("Expected Result.Success with 401") // scalafix:ok DisableSyntax.throw
      }

      test("accepts valid API key when ApiKey auth allowed") {
        val apiKeysConfigured = sys.env.get("API_KEYS").exists(_.nonEmpty)

        if !apiKeysConfigured then println("Skipping - API_KEYS not set")
        else {
          val validKey = AuthConfig.validApiKeys.head
          val decorator = new authenticated(Vector(AuthType.ApiKey))
          val request = mockRequest(Map("X-API-Key" -> validKey))

          var capturedAuthReq: Option[AuthenticatedRequest] = None // scalafix:ok DisableSyntax.var

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
              assert(capturedAuthReq.get.profileId == "service")
            case _ =>
              throw java.lang.AssertionError("Expected successful delegation") // scalafix:ok DisableSyntax.throw
        }
      }
    }

    test("authenticated with multiple auth types") {
      test("falls back to API key when JWT fails but ApiKey present") {
        val apiKeysConfigured = sys.env.get("API_KEYS").exists(_.nonEmpty)
        val supabaseConfigured = sys.env.get("SUPABASE_URL").isDefined

        if !apiKeysConfigured then println("Skipping - API_KEYS not set")
        else if !supabaseConfigured then println("Skipping - SUPABASE_URL not set")
        else {
          val validKey = AuthConfig.validApiKeys.head
          val decorator = new authenticated(Vector(AuthType.JWT, AuthType.ApiKey))
          val request = mockRequest(Map("X-API-Key" -> validKey))

          var capturedAuthReq: Option[AuthenticatedRequest] = None // scalafix:ok DisableSyntax.var

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
              throw java.lang.AssertionError("Expected successful delegation") // scalafix:ok DisableSyntax.throw
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

      test("Failure can have 503 status code for service unavailable") {
        val result = AuthResult.Failure("service unavailable", 503)
        assert(result.statusCode == 503)
      }
    }
  }
