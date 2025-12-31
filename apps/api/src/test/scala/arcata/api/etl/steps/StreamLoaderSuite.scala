package arcata.api.etl.steps

import utest.*
import arcata.api.domain.*
import arcata.api.etl.framework.*
import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig

object StreamLoaderSuite extends TestSuite:
  // Test helper: mock SupabaseClient
  class MockSupabaseClient(
      insertStreamEntryResult: Option[JobStreamEntry] = None
  ) extends SupabaseClient(
        SupabaseConfig(
          url = "http://test",
          serviceRoleKey = "test"
        )
      ):
    override def insertJobStreamEntry(entry: JobStreamEntry): Option[JobStreamEntry] =
      insertStreamEntryResult

  val tests = Tests {
    test("creates stream entry for job") {
      val createdEntry = JobStreamEntry(
        streamId = Some(789L),
        jobId = 123L,
        profileId = "user-1",
        source = "manual",
        status = Some("new")
      )
      val client = MockSupabaseClient(insertStreamEntryResult = Some(createdEntry))
      val loader = StreamLoader(client)

      val input = StreamLoaderInput(
        job = Job(jobId = Some(123L), companyId = Some(1L), title = "Engineer"),
        profileId = "user-1",
        source = "manual"
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isRight)
      result.foreach { output =>
        assert(output.streamEntry.streamId == Some(789L))
        assert(output.streamEntry.jobId == 123L)
        assert(output.streamEntry.profileId == "user-1")
      }
    }

    test("fails when job has no ID") {
      val client = MockSupabaseClient()
      val loader = StreamLoader(client)

      val input = StreamLoaderInput(
        job = Job(companyId = Some(1L), title = "Engineer"), // No jobId
        profileId = "user-1",
        source = "manual"
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isLeft)
      result.left.foreach { error =>
        assert(error.message.contains("Job must have an ID"))
      }
    }

    test("fails when stream entry insert fails") {
      val client = MockSupabaseClient(insertStreamEntryResult = None)
      val loader = StreamLoader(client)

      val input = StreamLoaderInput(
        job = Job(jobId = Some(123L), companyId = Some(1L), title = "Engineer"),
        profileId = "user-1",
        source = "manual"
      )
      val ctx = PipelineContext.create("test-profile")

      val result = loader.run(input, ctx)

      assert(result.isLeft)
      result.left.foreach { error =>
        assert(error.message.contains("Failed to create stream entry"))
      }
    }
  }
