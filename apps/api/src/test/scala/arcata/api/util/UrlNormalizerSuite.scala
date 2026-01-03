package arcata.api.util

import utest.*

object UrlNormalizerSuite extends TestSuite:
  val tests = Tests {
    test("normalize") {
      test("removes query parameters") {
        val result = UrlNormalizer.normalize("https://example.com/job/123?utm_source=linkedin&ref=abc")
        assert(result == "https://example.com/job/123")
      }

      test("removes fragment") {
        val result = UrlNormalizer.normalize("https://example.com/job/123#apply-now")
        assert(result == "https://example.com/job/123")
      }

      test("lowercases host") {
        val result = UrlNormalizer.normalize("https://EXAMPLE.COM/Job/123")
        // Note: path case is preserved, only host is lowercased
        assert(result == "https://example.com/Job/123")
      }

      test("removes trailing slash") {
        val result = UrlNormalizer.normalize("https://example.com/job/123/")
        assert(result == "https://example.com/job/123")
      }

      test("removes multiple trailing slashes") {
        val result = UrlNormalizer.normalize("https://example.com/job/123///")
        assert(result == "https://example.com/job/123")
      }

      test("handles URL with no path") {
        val result = UrlNormalizer.normalize("https://example.com")
        assert(result == "https://example.com")
      }

      test("handles URL with root path") {
        val result = UrlNormalizer.normalize("https://example.com/")
        assert(result == "https://example.com")
      }

      test("preserves non-standard ports") {
        val result = UrlNormalizer.normalize("https://example.com:8080/job/123")
        assert(result == "https://example.com:8080/job/123")
      }

      test("removes standard HTTPS port") {
        val result = UrlNormalizer.normalize("https://example.com:443/job/123")
        assert(result == "https://example.com/job/123")
      }

      test("removes standard HTTP port") {
        val result = UrlNormalizer.normalize("http://example.com:80/job/123")
        assert(result == "http://example.com/job/123")
      }

      test("handles Greenhouse URLs with tracking params") {
        val result = UrlNormalizer.normalize(
          "https://job-boards.greenhouse.io/temporal/jobs/123?gh_jid=456&source=linkedin"
        )
        assert(result == "https://job-boards.greenhouse.io/temporal/jobs/123")
      }

      test("returns original on invalid URL") {
        val invalid = "not a valid url"
        val result = UrlNormalizer.normalize(invalid)
        assert(result == invalid)
      }

      test("trims whitespace") {
        val result = UrlNormalizer.normalize("  https://example.com/job/123  ")
        assert(result == "https://example.com/job/123")
      }
    }

    test("extractGreenhouseCompanyId") {
      test("extracts from job-boards.greenhouse.io") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://job-boards.greenhouse.io/temporaltechnologies"
        )
        assert(result == Some("temporaltechnologies"))
      }

      test("extracts from job-boards.greenhouse.io with job path") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://job-boards.greenhouse.io/temporaltechnologies/jobs/12345"
        )
        assert(result == Some("temporaltechnologies"))
      }

      test("extracts from boards.greenhouse.io") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://boards.greenhouse.io/stripe"
        )
        assert(result == Some("stripe"))
      }

      test("extracts from boards-api.greenhouse.io") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://boards-api.greenhouse.io/v1/boards/acme/jobs"
        )
        assert(result == Some("acme"))
      }

      test("handles URL with query params") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://job-boards.greenhouse.io/temporal?ref=linkedin"
        )
        assert(result == Some("temporal"))
      }

      test("returns None for non-greenhouse URL") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://example.com/careers"
        )
        assert(result == None)
      }

      test("returns None for lever URL") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://jobs.lever.co/stripe"
        )
        assert(result == None)
      }

      test("returns None for empty path on greenhouse") {
        val result = UrlNormalizer.extractGreenhouseCompanyId(
          "https://boards.greenhouse.io/"
        )
        assert(result == None)
      }

      test("returns None for invalid URL") {
        val result = UrlNormalizer.extractGreenhouseCompanyId("not a url")
        assert(result == None)
      }
    }

    test("isGreenhouseUrl") {
      test("returns true for boards.greenhouse.io") {
        assert(UrlNormalizer.isGreenhouseUrl("https://boards.greenhouse.io/stripe") == true)
      }

      test("returns true for job-boards.greenhouse.io") {
        assert(UrlNormalizer.isGreenhouseUrl("https://job-boards.greenhouse.io/temporal") == true)
      }

      test("returns true for boards-api.greenhouse.io") {
        assert(UrlNormalizer.isGreenhouseUrl("https://boards-api.greenhouse.io/v1/boards/x/jobs") == true)
      }

      test("returns false for lever") {
        assert(UrlNormalizer.isGreenhouseUrl("https://jobs.lever.co/stripe") == false)
      }

      test("returns false for random URL") {
        assert(UrlNormalizer.isGreenhouseUrl("https://example.com/greenhouse") == false)
      }
    }
  }
