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
          serviceRoleKey = "test"
        )
      ):
    override def findJobBySourceUrl(sourceUrl: String): Option[Job] = findJobResult
    override def insertJob(job: Job): Option[Job] = insertJobResult

  val tests = Tests {
    test("returns existing job when found by source URL") {
      val existingJob = Job(
        jobId = Some(123L),
        companyId = Some(1L),
        title = "Existing Job",
        sourceUrl = Some("https://example.com/job")
      )
      val client = MockSupabaseClient(findJobResult = Some(existingJob))
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(ExtractedJobData(title = "New Job")),
        company = Some(Company(companyId = Some(1L))),
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

    test("creates new job with company") {
      val createdJob = Job(
        jobId = Some(456L),
        companyId = Some(1L),
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
        company = Some(Company(companyId = Some(1L))),
        url = "https://example.com/new-job",
        objectId = Some("obj-123")
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.job.jobId == Some(456L))
        assert(output.job.companyId == Some(1L))
      }
    }

    test("creates orphaned job when company is None") {
      val orphanedJob = Job(
        jobId = Some(789L),
        companyId = None, // Orphaned - no company
        title = "Orphaned Job"
      )
      val client = MockSupabaseClient(
        findJobResult = None,
        insertJobResult = Some(orphanedJob)
      )
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(ExtractedJobData(title = "Orphaned Job")),
        company = None, // No company resolved
        url = "https://example.com/orphaned-job",
        objectId = None
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.job.jobId == Some(789L))
        assert(output.job.companyId == None)
        assert(output.company == None)
      }
    }

    test("creates orphaned job when company has no ID") {
      val orphanedJob = Job(
        jobId = Some(101L),
        companyId = None,
        title = "Job Without Company ID"
      )
      val client = MockSupabaseClient(
        findJobResult = None,
        insertJobResult = Some(orphanedJob)
      )
      val loader = JobLoader(client)

      val input = JobLoaderInput(
        extractedData = Transformed(ExtractedJobData(title = "Job Without Company ID")),
        company = Some(Company()), // Company present but no companyId
        url = "https://example.com/job",
        objectId = None
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.job.companyId == None)
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
        company = Some(Company(companyId = Some(1L))),
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
