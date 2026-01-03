package arcata.api.etl.greenhouse

import arcata.api.etl.framework.PipelineContext
import arcata.api.extraction.CompletionState
import utest.*

object GreenhouseJobParserSuite extends TestSuite:

  // Sample Greenhouse API responses for testing
  val fullJobJson = """
    {
      "id": 4974513007,
      "title": "Staff Product Designer, Observability",
      "updated_at": "2025-01-02T10:30:00Z",
      "requisition_id": "REQ-001",
      "location": {
        "name": "San Francisco, CA"
      },
      "content": "&lt;p&gt;We are looking for a talented designer.&lt;/p&gt;&lt;p&gt;Requirements:&lt;/p&gt;",
      "absolute_url": "https://boards.greenhouse.io/temporal/jobs/4974513007",
      "internal_job_id": 12345,
      "pay_input_ranges": [
        {
          "min_cents": 15000000,
          "max_cents": 20000000,
          "currency_type": "USD",
          "title": "Base Salary"
        }
      ]
    }
  """

  val remoteJobJson = """
    {
      "id": 123456,
      "title": "Senior Software Engineer",
      "location": {
        "name": "USA - Remote"
      },
      "content": "Join our distributed team!",
      "absolute_url": "https://boards.greenhouse.io/acme/jobs/123456"
    }
  """

  val minimalJobJson = """
    {
      "id": 789,
      "title": "Engineer",
      "content": "",
      "absolute_url": "https://example.com/jobs/789"
    }
  """

  val noSalaryJson = """
    {
      "id": 456,
      "title": "Product Manager",
      "location": {
        "name": "New York, NY"
      },
      "content": "<p>Great opportunity!</p>",
      "absolute_url": "https://boards.greenhouse.io/company/jobs/456"
    }
  """

  val tests = Tests {
    val ctx = PipelineContext.create("test-profile")

    test("GreenhouseJobParser") {
      test("parses full job with all fields") {
        val input = GreenhouseJobParserInput(
          json = fullJobJson,
          apiUrl = "https://boards-api.greenhouse.io/v1/boards/temporal/jobs/4974513007",
          sourceUrl = "https://boards.greenhouse.io/temporal/jobs/4974513007",
          companyId = Some(100L)
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val output = result.toOption.get
        val data = output.extracted

        // Basic fields
        assert(data.title == "Staff Product Designer, Observability")
        assert(data.location == Some("San Francisco, CA"))
        assert(data.applicationUrl == Some("https://boards.greenhouse.io/temporal/jobs/4974513007"))

        // Description should have HTML entities unescaped
        assert(data.description.isDefined)
        assert(data.description.get.contains("<p>"))
        assert(!data.description.get.contains("&lt;"))

        // Salary (converted from cents to dollars)
        assert(data.salaryMin == Some(150000))
        assert(data.salaryMax == Some(200000))
        assert(data.salaryCurrency == Some("USD"))

        // Not remote (San Francisco, CA)
        assert(data.isRemote == Some(false))

        // Company ID passed through
        assert(output.companyId == Some(100L))

        // Completion state should be Complete (has salary)
        assert(output.completionState == CompletionState.Complete)
      }

      test("detects remote jobs from location") {
        val input = GreenhouseJobParserInput(
          json = remoteJobJson,
          apiUrl = "https://boards-api.greenhouse.io/v1/boards/acme/jobs/123456",
          sourceUrl = "https://boards.greenhouse.io/acme/jobs/123456",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val data = result.toOption.get.extracted
        assert(data.isRemote == Some(true))
        assert(data.location == Some("USA - Remote"))
      }

      test("handles missing salary gracefully") {
        val input = GreenhouseJobParserInput(
          json = noSalaryJson,
          apiUrl = "https://boards-api.greenhouse.io/v1/boards/company/jobs/456",
          sourceUrl = "https://boards.greenhouse.io/company/jobs/456",
          companyId = Some(200L)
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val output = result.toOption.get
        val data = output.extracted

        assert(data.salaryMin == None)
        assert(data.salaryMax == None)
        assert(data.salaryCurrency == None)

        // Without salary, should be Sufficient (has description, location, URL)
        assert(output.completionState == CompletionState.Sufficient)
      }

      test("handles minimal job with empty content") {
        val input = GreenhouseJobParserInput(
          json = minimalJobJson,
          apiUrl = "https://boards-api.greenhouse.io/v1/boards/test/jobs/789",
          sourceUrl = "https://example.com/jobs/789",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val output = result.toOption.get
        val data = output.extracted

        assert(data.title == "Engineer")
        assert(data.description == Some(""))
        assert(data.location == None)
        assert(data.isRemote == None)

        // Minimal - only has title
        assert(output.completionState == CompletionState.Minimal)
      }

      test("fails on invalid JSON") {
        val input = GreenhouseJobParserInput(
          json = "not valid json",
          apiUrl = "https://example.com/api",
          sourceUrl = "https://example.com/job",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isLeft)
        assert(result.swap.toOption.get.message.contains("Failed to parse Greenhouse JSON"))
      }

      test("fails on JSON missing required fields") {
        val incompleteJson = """{"id": 123}"""
        val input = GreenhouseJobParserInput(
          json = incompleteJson,
          apiUrl = "https://example.com/api",
          sourceUrl = "https://example.com/job",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isLeft)
      }
    }

    test("HTML entity unescaping") {
      test("unescapes common HTML entities") {
        val jsonWithEntities = """
          {
            "id": 1,
            "title": "Test &amp; Job",
            "content": "&lt;p&gt;Hello &quot;world&quot;&lt;/p&gt;&nbsp;More text",
            "absolute_url": "https://example.com/job"
          }
        """

        val input = GreenhouseJobParserInput(
          json = jsonWithEntities,
          apiUrl = "https://example.com/api",
          sourceUrl = "https://example.com/job",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val description = result.toOption.get.extracted.description.get
        assert(description.contains("<p>"))
        assert(description.contains("</p>"))
        assert(description.contains("\"world\""))
        assert(!description.contains("&lt;"))
        assert(!description.contains("&gt;"))
        assert(!description.contains("&quot;"))
      }
    }

    test("salary conversion") {
      test("converts cents to dollars correctly") {
        // $150,000 = 15,000,000 cents
        val jsonWith150k = """
          {
            "id": 1,
            "title": "Test",
            "content": "desc",
            "absolute_url": "https://example.com/job",
            "pay_input_ranges": [{"min_cents": 15000000, "max_cents": 20000000, "currency_type": "USD"}]
          }
        """

        val input = GreenhouseJobParserInput(
          json = jsonWith150k,
          apiUrl = "https://example.com/api",
          sourceUrl = "https://example.com/job",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val data = result.toOption.get.extracted
        assert(data.salaryMin == Some(150000))
        assert(data.salaryMax == Some(200000))
      }

      test("handles partial salary ranges") {
        // Only min_cents defined
        val jsonWithMinOnly = """
          {
            "id": 1,
            "title": "Test",
            "content": "desc",
            "absolute_url": "https://example.com/job",
            "pay_input_ranges": [{"min_cents": 10000000}]
          }
        """

        val input = GreenhouseJobParserInput(
          json = jsonWithMinOnly,
          apiUrl = "https://example.com/api",
          sourceUrl = "https://example.com/job",
          companyId = None
        )

        val result = GreenhouseJobParser.run(input, ctx)
        assert(result.isRight)

        val data = result.toOption.get.extracted
        assert(data.salaryMin == Some(100000))
        assert(data.salaryMax == None)
        assert(data.salaryCurrency == None)
      }
    }

    test("isRemote detection") {
      test("detects various remote patterns") {
        val remotePatterns = Seq(
          "Remote",
          "USA - Remote",
          "Remote - San Francisco",
          "Fully Remote",
          "REMOTE",
          "remote opportunity"
        )

        for pattern <- remotePatterns do
          val json = s"""
            {
              "id": 1,
              "title": "Test",
              "content": "desc",
              "absolute_url": "https://example.com/job",
              "location": {"name": "$pattern"}
            }
          """

          val input = GreenhouseJobParserInput(
            json = json,
            apiUrl = "https://example.com/api",
            sourceUrl = "https://example.com/job",
            companyId = None
          )

          val result = GreenhouseJobParser.run(input, ctx)
          assert(result.isRight)
          assert(result.toOption.get.extracted.isRemote == Some(true))
      }

      test("correctly identifies non-remote jobs") {
        val nonRemotePatterns = Seq(
          "San Francisco, CA",
          "New York, NY",
          "London, UK",
          "Austin, TX"
        )

        for pattern <- nonRemotePatterns do
          val json = s"""
            {
              "id": 1,
              "title": "Test",
              "content": "desc",
              "absolute_url": "https://example.com/job",
              "location": {"name": "$pattern"}
            }
          """

          val input = GreenhouseJobParserInput(
            json = json,
            apiUrl = "https://example.com/api",
            sourceUrl = "https://example.com/job",
            companyId = None
          )

          val result = GreenhouseJobParser.run(input, ctx)
          assert(result.isRight)
          assert(result.toOption.get.extracted.isRemote == Some(false))
      }
    }
  }
