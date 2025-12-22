package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig
import arcata.api.domain.{Job, JobStreamEntry}
import arcata.api.etl.framework.{PipelineContext, StepError}
import munit.FunSuite

/** Mock SupabaseClient for testing StreamLoader. */
class MockStreamSupabaseClient(
    shouldFailInsert: Boolean = false
) extends SupabaseClient(
      SupabaseConfig(
        url = "http://localhost",
        anonKey = "test",
        serviceRoleKey = "test",
        jwtSecret = "test"
      )
    ):

  private var insertedEntries: List[JobStreamEntry] = List.empty

  override def insertJobStreamEntry(entry: JobStreamEntry): Option[JobStreamEntry] = {
    if shouldFailInsert then None
    else {
      val created = entry.copy(streamId = Some(System.currentTimeMillis()))
      insertedEntries = created :: insertedEntries
      Some(created)
    }
  }

  def getInsertedEntries: List[JobStreamEntry] = insertedEntries

class StreamLoaderSuite extends FunSuite:

  val ctx: PipelineContext = PipelineContext.create("test-profile")

  val testJob: Job = Job(
    jobId = Some(456L),
    companyId = 100L,
    title = "Test Job"
  )

  test("StreamLoader should create stream entry"):
    val mockClient = new MockStreamSupabaseClient()
    val loader = StreamLoader(mockClient)

    val input = StreamLoaderInput(
      job = testJob,
      profileId = "user-123",
      source = "manual"
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assert(output.streamEntry.streamId.isDefined)
      assertEquals(output.streamEntry.jobId, 456L)
      assertEquals(output.streamEntry.profileId, "user-123")
      assertEquals(output.streamEntry.source, "manual")
      assertEquals(output.streamEntry.status, Some("new"))
      assertEquals(output.job, testJob)
    }
    assertEquals(mockClient.getInsertedEntries.length, 1)

  test("StreamLoader should fail if job has no ID"):
    val mockClient = new MockStreamSupabaseClient()
    val loader = StreamLoader(mockClient)
    val jobWithoutId = Job(companyId = 100L, title = "No ID Job")

    val input = StreamLoaderInput(
      job = jobWithoutId,
      profileId = "user-123",
      source = "scrape"
    )

    val result = loader.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.ValidationError])
      assert(error.message.contains("Job must have an ID"))
    }

  test("StreamLoader should fail when insert fails"):
    val mockClient = new MockStreamSupabaseClient(shouldFailInsert = true)
    val loader = StreamLoader(mockClient)

    val input = StreamLoaderInput(
      job = testJob,
      profileId = "user-123",
      source = "manual"
    )

    val result = loader.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.LoadError])
    }

  test("StreamLoader should preserve source type"):
    val mockClient = new MockStreamSupabaseClient()
    val loader = StreamLoader(mockClient)

    val sources = Seq("manual", "scrape", "recommendation", "import")

    sources.foreach { source =>
      val input = StreamLoaderInput(
        job = testJob,
        profileId = "user-456",
        source = source
      )

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assertEquals(output.streamEntry.source, source)
      }
    }
