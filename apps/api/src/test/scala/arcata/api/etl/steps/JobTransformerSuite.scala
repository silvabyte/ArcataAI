package arcata.api.etl.steps

import utest.*
import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.{PipelineContext, Transformed}
import arcata.api.extraction.CompletionState

object JobTransformerSuite extends TestSuite:
  val tests = Tests {
    test("trims whitespace from string fields") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "  Software Engineer  ",
          companyName = Some("  Acme Inc  "),
          location = Some("  Remote  ")
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.title == "Software Engineer")
        assert(output.transformed.value.companyName == Some("Acme Inc"))
        assert(output.transformed.value.location == Some("Remote"))
      }
    }

    test("converts empty strings to None") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "Engineer",
          companyName = Some(""),
          description = Some(""),
          postedDate = Some(""),
          closingDate = Some("")
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.companyName.isEmpty)
        assert(output.transformed.value.description.isEmpty)
        assert(output.transformed.value.postedDate.isEmpty)
        assert(output.transformed.value.closingDate.isEmpty)
      }
    }

    test("converts whitespace-only strings to None") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "Engineer",
          companyName = Some("   "),
          description = Some("\t\n")
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.companyName.isEmpty)
        assert(output.transformed.value.description.isEmpty)
      }
    }

    test("preserves valid non-empty strings") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "Senior Engineer",
          companyName = Some("Tech Corp"),
          description = Some("Build amazing things"),
          location = Some("New York, NY")
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.title == "Senior Engineer")
        assert(output.transformed.value.companyName == Some("Tech Corp"))
        assert(output.transformed.value.description == Some("Build amazing things"))
        assert(output.transformed.value.location == Some("New York, NY"))
      }
    }

    test("removes empty entries from lists") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "Engineer",
          qualifications = Some(List("Python", "", "  ", "JavaScript", "")),
          benefits = Some(List("  Health  ", "", "Dental"))
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.qualifications == Some(List("Python", "JavaScript")))
        assert(output.transformed.value.benefits == Some(List("Health", "Dental")))
      }
    }

    test("passes through Int fields unchanged") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "Engineer",
          salaryMin = Some(100000),
          salaryMax = Some(150000)
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.salaryMin == Some(100000))
        assert(output.transformed.value.salaryMax == Some(150000))
      }
    }

    test("passes through Boolean fields unchanged") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(
          title = "Engineer",
          isRemote = Some(true)
        ),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.transformed.value.isRemote == Some(true))
      }
    }

    test("never fails - always returns Right") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(title = "Any Job"),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
    }

    test("output is wrapped in Transformed") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(title = "Engineer"),
        sourceUrl = "https://example.com/job",
        objectId = None,
        completionState = CompletionState.Complete
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        // Verify the output has the Transformed wrapper
        val transformed: Transformed[ExtractedJobData] = output.transformed
        assert(transformed.value.title == "Engineer")
      }
    }

    test("preserves sourceUrl and objectId in output") {
      val input = JobTransformerInput(
        extracted = ExtractedJobData(title = "Engineer"),
        sourceUrl = "https://example.com/specific-job",
        objectId = Some("obj-abc-123"),
        completionState = CompletionState.Sufficient
      )
      val ctx = PipelineContext.create("test-profile")

      val result = JobTransformer.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.sourceUrl == "https://example.com/specific-job")
        assert(output.objectId == Some("obj-abc-123"))
        assert(output.completionState == CompletionState.Sufficient)
      }
    }
  }
