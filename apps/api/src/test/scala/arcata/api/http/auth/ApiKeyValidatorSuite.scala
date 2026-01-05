package arcata.api.http.auth

import cask.model.Request
import io.undertow.server.HttpServerExchange
import io.undertow.util.{HeaderMap, HttpString}
import utest.*

/**
 * Tests for ApiKeyValidator.
 *
 * Note: These tests require API_KEYS to be set in the environment. The test runner should set this
 * via build configuration or the tests will be skipped if not configured.
 */
object ApiKeyValidatorSuite extends TestSuite:
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

  val tests = Tests {
    test("validate") {
      // Skip tests if API_KEYS not configured
      val apiKeysConfigured = sys.env.get("API_KEYS").exists(_.nonEmpty)

      test("returns Failure when X-API-Key header is missing") {
        val request = mockRequest(Map.empty)
        val result = ApiKeyValidator.validate(request)

        result match
          case AuthResult.Failure(reason, _) =>
            assert(reason.contains("Missing X-API-Key"))
          case AuthResult.Success(_) =>
            throw new java.lang.AssertionError("Expected Failure for missing header") // scalafix:ok DisableSyntax.throw
      }

      test("returns Failure for invalid API key") {
        val request = mockRequest(Map("X-API-Key" -> "invalid-key-12345"))
        val result = ApiKeyValidator.validate(request)

        result match
          case AuthResult.Failure(reason, _) =>
            assert(reason.contains("Invalid API key"))
          case AuthResult.Success(_) =>
            throw new java.lang.AssertionError("Expected Failure for invalid key") // scalafix:ok DisableSyntax.throw
      }

      test("returns Success for valid API key") {
        if !apiKeysConfigured then
          // Skip test - no API keys configured
          println("Skipping valid API key test - API_KEYS not set")
        else {
          // Use the first configured API key
          val validKey = AuthConfig.validApiKeys.head
          val request = mockRequest(Map("X-API-Key" -> validKey))
          val result = ApiKeyValidator.validate(request)

          result match
            case AuthResult.Success(authReq) =>
              assert(authReq.profileId == "service")
              assert(authReq.authType == AuthType.ApiKey)
              assert(authReq.claims == None)
            case AuthResult.Failure(reason, _) =>
              throw new java.lang.AssertionError(
                s"Expected Success, got Failure: $reason"
              ) // scalafix:ok DisableSyntax.throw
        }
      }

      test("header lookup is case-insensitive") {
        val request = mockRequest(Map("x-api-key" -> "invalid-key"))
        val result = ApiKeyValidator.validate(request)

        // Should find the header (even though invalid key)
        result match
          case AuthResult.Failure(reason, _) =>
            assert(reason.contains("Invalid API key")) // Not "Missing"
          case AuthResult.Success(_) =>
            throw new java.lang.AssertionError("Expected Failure for invalid key") // scalafix:ok DisableSyntax.throw
      }
    }

    test("AuthConfig.apiKeysConfigured") {
      test("returns true when API_KEYS is set and non-empty") {
        val configured = sys.env.get("API_KEYS").exists(_.nonEmpty)
        assert(AuthConfig.apiKeysConfigured == configured)
      }
    }
  }
