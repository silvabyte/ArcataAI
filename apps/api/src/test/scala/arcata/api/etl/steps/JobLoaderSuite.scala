package arcata.api.etl.steps

import utest.*
import arcata.api.domain.*
import arcata.api.etl.framework.*
import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig

object JobLoaderSuite extends TestSuite:
  // Test helper: create a mock SupabaseClient that returns predictable results
  class MockSupabaseClient(
      findJobResult: Option[Job] = None,
      insertJobResult: Option[Job] = None
  ) extends SupabaseClient(
        SupabaseConfig(
          url = "http://test",
          anonKey = "test",
          serviceRoleKey = "test",
          jwtSecret = "test"
        )
      ):
    override def findJobBySourceUrl(sourceUrl: String): Option[Job] = findJobResult
    override def insertJob(job: Job): Option[Job] = insertJobResult

  val tests = Tests {
    test("returns existing job when found by source URL") {
      val existingJob = Job(
        jobId = Some(123L),
        companyId = 1L,
        title = "Existing Job",
        sourceUrl = Some("https://example.com/job")
      )
      val client = MockSupabaseClient(findJobResult = Some(existingJob))
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(ExtractedJobData(title = "New Job")),
        company = Company(companyId = Some(1L)),
        url = "https://example.com/job",
        objectId = None
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.job.jobId == Some(123L))
        assert(output.job.title == "Existing Job")
      }
    }

    test("creates new job when not found") {
      val createdJob = Job(
        jobId = Some(456L),
        companyId = 1L,
        title = "Software Engineer"
      )
      val client = MockSupabaseClient(
        findJobResult = None,
        insertJobResult = Some(createdJob)
      )
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(
          ExtractedJobData(
            title = "Software Engineer",
            description = Some("Build things"),
            location = Some("Remote")
          )
        ),
        company = Company(companyId = Some(1L)),
        url = "https://example.com/new-job",
        objectId = Some("obj-123")
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.job.jobId == Some(456L))
      }
    }

    test("fails when company has no ID") {
      val client = MockSupabaseClient()
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(ExtractedJobData(title = "Job")),
        company = Company(), // No companyId
        url = "https://example.com/job",
        objectId = None
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isLeft)
      result.left.foreach { error =>
        assert(error.message.contains("Company must have an ID"))
      }
    }

    test("fails when job insert fails") {
      val client = MockSupabaseClient(
        findJobResult = None,
        insertJobResult = None // Insert fails
      )
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(ExtractedJobData(title = "Job")),
        company = Company(companyId = Some(1L)),
        url = "https://example.com/job",
        objectId = None
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isLeft)
      result.left.foreach { error =>
        assert(error.message.contains("Failed to create job"))
      }
    }
  }
