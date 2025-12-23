package arcata.api.http.middleware

import utest.*

object CorsMiddlewareSuite extends TestSuite:
  val tests = Tests {
    test("CorsConfig") {
      test("isAllowed returns true for listed origin") {
        val config = CorsConfig(allowedOrigins = List("http://localhost:4201"))
        assert(config.isAllowed("http://localhost:4201"))
      }

      test("isAllowed returns false for unlisted origin") {
        val config = CorsConfig(allowedOrigins = List("http://localhost:4201"))
        assert(!config.isAllowed("http://evil.com"))
      }

      test("isAllowed returns true for any origin when wildcard is present") {
        val config = CorsConfig(allowedOrigins = List("*"))
        assert(config.isAllowed("http://anything.com"))
        assert(config.isAllowed("http://localhost:9999"))
      }

      test("isAllowed supports multiple origins") {
        val config = CorsConfig(allowedOrigins = List("http://localhost:4200", "http://localhost:4201"))
        assert(config.isAllowed("http://localhost:4200"))
        assert(config.isAllowed("http://localhost:4201"))
        assert(!config.isAllowed("http://localhost:4202"))
      }

      test("headersFor returns CORS headers for allowed origin") {
        val config = CorsConfig(allowedOrigins = List("http://localhost:4201"))
        val headers = config.headersFor("http://localhost:4201")

        assert(headers.nonEmpty)
        assert(headers.exists { case (k, v) => k == "Access-Control-Allow-Origin" && v == "http://localhost:4201" })
        assert(headers.exists { case (k, _) => k == "Access-Control-Allow-Methods" })
        assert(headers.exists { case (k, _) => k == "Access-Control-Allow-Headers" })
        assert(headers.exists { case (k, _) => k == "Access-Control-Max-Age" })
        assert(headers.exists { case (k, v) => k == "Access-Control-Allow-Credentials" && v == "true" })
      }

      test("headersFor returns empty for disallowed origin") {
        val config = CorsConfig(allowedOrigins = List("http://localhost:4201"))
        val headers = config.headersFor("http://evil.com")

        assert(headers.isEmpty)
      }

      test("has sensible defaults") {
        val config = CorsConfig(allowedOrigins = List("http://localhost:4201"))

        assert(config.allowedMethods.contains("GET"))
        assert(config.allowedMethods.contains("POST"))
        assert(config.allowedMethods.contains("OPTIONS"))
        assert(config.allowedHeaders.contains("Content-Type"))
        assert(config.allowedHeaders.contains("Authorization"))
      }
    }
  }
