package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig
import arcata.api.domain.{Job, JobApplication}
import arcata.api.etl.framework.{PipelineContext, StepError}
import munit.FunSuite

/** Mock SupabaseClient for testing ApplicationLoader. */
class MockApplicationSupabaseClient(
    defaultStatusId: Option[Long] = Some(1L),
    shouldFailInsert: Boolean = false
) extends SupabaseClient(
      SupabaseConfig(
        url = "http://localhost",
        anonKey = "test",
        serviceRoleKey = "test",
        jwtSecret = "test"
      )
    ):

  private var insertedApplications: List[JobApplication] = List.empty

  override def getDefaultStatusId(profileId: String): Option[Long] =
    defaultStatusId

  override def insertJobApplication(application: JobApplication): Option[JobApplication] = {
    if shouldFailInsert then None
    else {
      val created = application.copy(applicationId = Some(System.currentTimeMillis()))
      insertedApplications = created :: insertedApplications
      Some(created)
    }
  }

  def getInsertedApplications: List[JobApplication] = insertedApplications

class ApplicationLoaderSuite extends FunSuite:

  val ctx: PipelineContext = PipelineContext.create("test-profile")

  val testJob: Job = Job(
    jobId = Some(789L),
    companyId = 100L,
    title = "Test Job for Application"
  )

  test("ApplicationLoader should create application"):
    val mockClient = new MockApplicationSupabaseClient()
    val loader = ApplicationLoader(mockClient)

    val input = ApplicationLoaderInput(
      job = testJob,
      profileId = "user-app-123",
      notes = Some("Great opportunity!")
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assert(output.application.applicationId.isDefined)
      assertEquals(output.application.jobId, Some(789L))
      assertEquals(output.application.profileId, "user-app-123")
      assertEquals(output.application.notes, Some("Great opportunity!"))
      assertEquals(output.application.statusId, Some(1L))
      assertEquals(output.application.statusOrder, 0)
      assert(output.application.applicationDate.isDefined)
      assertEquals(output.job, testJob)
    }
    assertEquals(mockClient.getInsertedApplications.length, 1)

  test("ApplicationLoader should work without notes"):
    val mockClient = new MockApplicationSupabaseClient()
    val loader = ApplicationLoader(mockClient)

    val input = ApplicationLoaderInput(
      job = testJob,
      profileId = "user-app-456",
      notes = None
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.application.notes, None)
    }

  test("ApplicationLoader should fail if job has no ID"):
    val mockClient = new MockApplicationSupabaseClient()
    val loader = ApplicationLoader(mockClient)
    val jobWithoutId = Job(companyId = 100L, title = "No ID Job")

    val input = ApplicationLoaderInput(
      job = jobWithoutId,
      profileId = "user-app-789",
      notes = None
    )

    val result = loader.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.ValidationError])
      assert(error.message.contains("Job must have an ID"))
    }

  test("ApplicationLoader should fail when insert fails"):
    val mockClient = new MockApplicationSupabaseClient(shouldFailInsert = true)
    val loader = ApplicationLoader(mockClient)

    val input = ApplicationLoaderInput(
      job = testJob,
      profileId = "user-app-fail",
      notes = None
    )

    val result = loader.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.LoadError])
    }

  test("ApplicationLoader should handle missing default status"):
    val mockClient = new MockApplicationSupabaseClient(defaultStatusId = None)
    val loader = ApplicationLoader(mockClient)

    val input = ApplicationLoaderInput(
      job = testJob,
      profileId = "user-no-status",
      notes = None
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.application.statusId, None)
    }

  test("ApplicationLoader should set application date"):
    val mockClient = new MockApplicationSupabaseClient()
    val loader = ApplicationLoader(mockClient)

    val input = ApplicationLoaderInput(
      job = testJob,
      profileId = "user-date-test",
      notes = None
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assert(output.application.applicationDate.isDefined)
      // Date should be in ISO format YYYY-MM-DD
      val datePattern = """\d{4}-\d{2}-\d{2}""".r
      assert(datePattern.matches(output.application.applicationDate.get))
    }
