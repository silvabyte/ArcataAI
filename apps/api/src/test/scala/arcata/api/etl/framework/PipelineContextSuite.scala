package arcata.api.etl.framework

import munit.FunSuite

class PipelineContextSuite extends FunSuite:

  test("PipelineContext.create should generate unique run IDs"):
    val ctx1 = PipelineContext.create("profile-1")
    val ctx2 = PipelineContext.create("profile-1")

    assertNotEquals(ctx1.runId, ctx2.runId)

  test("PipelineContext should store and retrieve metadata"):
    val ctx = PipelineContext.create("profile-1")
    val updated = ctx.withMetadata("key1", "value1")

    assertEquals(updated.getMetadata("key1"), Some("value1"))
    assertEquals(updated.getMetadata("nonexistent"), None)

  test("PipelineContext.withMetadataEntries should add multiple entries"):
    val ctx = PipelineContext.create("profile-1")
    val updated = ctx.withMetadataEntries(
      "key1" -> "value1",
      "key2" -> "value2"
    )

    assertEquals(updated.getMetadata("key1"), Some("value1"))
    assertEquals(updated.getMetadata("key2"), Some("value2"))

  test("PipelineContext should preserve profileId"):
    val ctx = PipelineContext.create("my-profile-123")

    assertEquals(ctx.profileId, "my-profile-123")

  test("PipelineContext.withRunId should use provided run ID"):
    val ctx = PipelineContext.withRunId("custom-run-id", "profile-1")

    assertEquals(ctx.runId, "custom-run-id")
