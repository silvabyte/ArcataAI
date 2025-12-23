package arcata.api.etl.framework

import utest.*

object PipelineContextSuite extends TestSuite:
  val tests = Tests {
    test("create generates unique run ID") {
      val ctx1 = PipelineContext.create("profile-1")
      val ctx2 = PipelineContext.create("profile-1")

      assert(ctx1.runId != ctx2.runId)
      assert(ctx1.profileId == "profile-1")
      assert(ctx2.profileId == "profile-1")
    }

    test("withRunId uses specified run ID") {
      val ctx = PipelineContext.withRunId("my-run-id", "profile-1")

      assert(ctx.runId == "my-run-id")
      assert(ctx.profileId == "profile-1")
    }

    test("withMetadata adds single entry") {
      val ctx = PipelineContext.create("profile-1")
      val updated = ctx.withMetadata("key1", "value1")

      assert(updated.getMetadata("key1") == Some("value1"))
      assert(ctx.getMetadata("key1") == None) // original unchanged
    }

    test("withMetadataEntries adds multiple entries") {
      val ctx = PipelineContext
        .create("profile-1")
        .withMetadataEntries(
          "key1" -> "value1",
          "key2" -> "value2"
        )

      assert(ctx.getMetadata("key1") == Some("value1"))
      assert(ctx.getMetadata("key2") == Some("value2"))
    }

    test("getMetadata returns None for missing key") {
      val ctx = PipelineContext.create("profile-1")

      assert(ctx.getMetadata("nonexistent") == None)
    }

    test("startedAt is set on creation") {
      val before = java.time.Instant.now()
      val ctx = PipelineContext.create("profile-1")
      val after = java.time.Instant.now()

      assert(!ctx.startedAt.isBefore(before))
      assert(!ctx.startedAt.isAfter(after))
    }
  }
