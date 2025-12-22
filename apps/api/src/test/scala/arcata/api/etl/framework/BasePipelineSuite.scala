package arcata.api.etl.framework

import munit.FunSuite

class BasePipelineSuite extends FunSuite:

  test("BasePipeline.fromStep should create pipeline from step"):
    val step = BaseStep("addTen") { (input: Int, _: PipelineContext) =>
      Right(input + 10)
    }
    val pipeline = BasePipeline.fromStep("AddTenPipeline", step)

    val result = pipeline.run(5, "test-profile")

    assert(result.isSuccess)
    assertEquals(result.output, Some(15))

  test("BasePipeline.fromSteps should create pipeline from composed steps"):
    val pipeline = BasePipeline.fromSteps[Int, Int]("ComposedPipeline") {
      val double = BaseStep("double")((n: Int, _) => Right(n * 2))
      val addOne = BaseStep("addOne")((n: Int, _) => Right(n + 1))
      double.andThen(addOne)
    }

    val result = pipeline.run(5, "test-profile")

    assert(result.isSuccess)
    assertEquals(result.output, Some(11)) // (5 * 2) + 1

  test("BasePipeline should track duration"):
    val step = BaseStep("sleep") { (_: Unit, _: PipelineContext) =>
      Thread.sleep(10)
      Right("done")
    }
    val pipeline = BasePipeline.fromStep("SleepPipeline", step)

    val result = pipeline.run((), "test-profile")

    assert(result.isSuccess)
    assert(result.durationMs >= 10)

  test("BasePipeline should capture error on failure"):
    val error = StepError.ValidationError("oops", "failStep")
    val step = BaseStep.fail[String, Int]("failStep", error)
    val pipeline = BasePipeline.fromStep("FailPipeline", step)

    val result = pipeline.run("input", "test-profile")

    assert(result.isFailure)
    assertEquals(result.error, Some(error))
    assertEquals(result.output, None)

  test("BasePipeline should generate unique run IDs"):
    val step = BaseStep.identity[Int]("identity")
    val pipeline = BasePipeline.fromStep("IdPipeline", step)

    val result1 = pipeline.run(1, "profile-1")
    val result2 = pipeline.run(2, "profile-1")

    assertNotEquals(result1.runId, result2.runId)

  test("BasePipeline.runWithContext should use provided context"):
    val step = BaseStep("checkContext") { (_: Unit, ctx: PipelineContext) =>
      Right(ctx.runId)
    }
    val pipeline = BasePipeline.fromStep("ContextPipeline", step)
    val ctx = PipelineContext.withRunId("custom-run-123", "profile")

    val result = pipeline.runWithContext((), ctx)

    assert(result.isSuccess)
    assertEquals(result.runId, "custom-run-123")
    assertEquals(result.output, Some("custom-run-123"))

  test("PipelineResult.isSuccess should be correct"):
    val success = PipelineResult.success("run-1", "output", 100L)
    val failure = PipelineResult.failure[String]("run-2", StepError.ValidationError("err", "s"), 50L)

    assert(success.isSuccess)
    assert(!success.isFailure)
    assert(!failure.isSuccess)
    assert(failure.isFailure)
