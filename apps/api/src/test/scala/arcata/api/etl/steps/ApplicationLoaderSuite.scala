package arcata.api.etl.steps

import utest.*
import arcata.api.domain.*
import arcata.api.etl.framework.*
import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig

object ApplicationLoaderSuite extends TestSuite:
  // Test helper: mock SupabaseClient
  class MockSupabaseClient(
      defaultStatusId: Option[Long] = None,
      insertApplicationResult: Option[JobApplication] = None
  ) extends SupabaseClient(
        SupabaseConfig(
          url = "http://test",
          anonKey = "test",
          serviceRoleKey = "test",
          jwtSecret = "test"
        )
      ):
    override def getDefaultStatusId(profileId: String): Option[Long] = defaultStatusId
    override def insertJobApplication(application: JobApplication): Option[JobApplication] =
      insertApplicationResult

  val tests = Tests {
    test("creates application for job") {
      val createdApp = JobApplication(
        applicationId = Some(101L),
        jobId = Some(123L),
        profileId = "user-1",
        statusOrder = 0,
        notes = Some("Great opportunity")
      )
      val client = MockSupabaseClient(
        defaultStatusId = Some(1L),
        insertApplicationResult = Some(createdApp)
      )
      val loader = ApplicationLoader(client)

      val input = ApplicationLoaderInput(
        job = Job(jobId = Some(123L), companyId = 1L, title = "Engineer"),
        profileId = "user-1",
        notes = Some("Great opportunity")
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.application.applicationId == Some(101L))
        assert(output.application.jobId == Some(123L))
        assert(output.job.jobId == Some(123L))
      }
    }

    test("creates application without default status") {
      val createdApp = JobApplication(
        applicationId = Some(102L),
        jobId = Some(123L),
        profileId = "user-1",
        statusOrder = 0
      )
      val client = MockSupabaseClient(
        defaultStatusId = None, // No default status
        insertApplicationResult = Some(createdApp)
      )
      val loader = ApplicationLoader(client)

      val input = ApplicationLoaderInput(
        job = Job(jobId = Some(123L), companyId = 1L, title = "Engineer"),
        profileId = "user-1"
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
    }

    test("fails when job has no ID") {
      val client = MockSupabaseClient()
      val loader = ApplicationLoader(client)

      val input = ApplicationLoaderInput(
        job = Job(companyId = 1L, title = "Engineer"), // No jobId
        profileId = "user-1"
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isLeft)
      result.left.foreach { error =>
        assert(error.message.contains("Job must have an ID"))
      }
    }

    test("fails when application insert fails") {
      val client = MockSupabaseClient(
        defaultStatusId = Some(1L),
        insertApplicationResult = None // Insert fails
      )
      val loader = ApplicationLoader(client)

      val input = ApplicationLoaderInput(
        job = Job(jobId = Some(123L), companyId = 1L, title = "Engineer"),
        profileId = "user-1"
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isLeft)
      result.left.foreach { error =>
        assert(error.message.contains("Failed to create application"))
      }
    }
  }
