package arcata.api.etl.steps

import utest.*
import arcata.api.etl.framework.*

object HtmlCleanerSuite extends TestSuite:

  val tests = Tests {

    test("step produces markdown output") {
      val cleaner = HtmlCleaner()
      val input = HtmlCleanerInput(
        html = "<html><body><h1>Job Title</h1><p>Description</p></body></html>",
        url = "https://example.com/job",
        objectId = Some("obj-123"),
      )
      val ctx = PipelineContext.create("test-profile")

      val result = cleaner.run(input, ctx)

      assert(result.isRight)
      result.foreach {
        output =>
          assert(output.markdown.contains("Job Title"))
          assert(output.url == "https://example.com/job")
          assert(output.objectId == Some("obj-123"))
      }
    }

    test("step passes through url and objectId") {
      val cleaner = HtmlCleaner()
      val input = HtmlCleanerInput(
        html = "<p>test</p>",
        url = "https://test.com",
        objectId = None,
      )
      val ctx = PipelineContext.create("test-profile")

      val result = cleaner.run(input, ctx)

      assert(result.isRight)
      result.foreach {
        output =>
          assert(output.url == "https://test.com")
          assert(output.objectId.isEmpty)
      }
    }

    test("step removes script tags from output") {
      val cleaner = HtmlCleaner()
      val input = HtmlCleanerInput(
        html = "<html><body><h1>Title</h1><script>evil()</script></body></html>",
        url = "https://example.com",
        objectId = None,
      )
      val ctx = PipelineContext.create("test-profile")

      val result = cleaner.run(input, ctx)

      assert(result.isRight)
      result.foreach {
        output =>
          assert(!output.markdown.contains("script"))
          assert(!output.markdown.contains("evil"))
          assert(output.markdown.contains("Title"))
      }
    }

    test("step handles empty html") {
      val cleaner = HtmlCleaner()
      val input = HtmlCleanerInput(
        html = "",
        url = "https://example.com",
        objectId = None,
      )
      val ctx = PipelineContext.create("test-profile")

      val result = cleaner.run(input, ctx)

      // Should succeed, not error
      assert(result.isRight)
    }
  }
