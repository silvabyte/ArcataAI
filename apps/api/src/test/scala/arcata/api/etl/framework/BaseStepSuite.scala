package arcata.api.etl.framework

import munit.FunSuite

class BaseStepSuite extends FunSuite:

  test("BaseStep.identity should pass input through unchanged"):
    val step = BaseStep.identity[String]("identity")
    val ctx = PipelineContext.create("test-profile")

    val result = step.run("hello", ctx)

    assertEquals(result, Right("hello"))

  test("BaseStep.fail should return error"):
    val error = StepError.ValidationError("test error", "fail")
    val step = BaseStep.fail[String, Int]("fail", error)
    val ctx = PipelineContext.create("test-profile")

    val result = step.run("input", ctx)

    assertEquals(result, Left(error))

  test("BaseStep.apply should create step from function"):
    val step = BaseStep("double") { (input: Int, _: PipelineContext) =>
      Right(input * 2)
    }
    val ctx = PipelineContext.create("test-profile")

    val result = step.run(5, ctx)

    assertEquals(result, Right(10))

  test("BaseStep.andThen should compose steps"):
    val double = BaseStep("double") { (input: Int, _: PipelineContext) =>
      Right(input * 2)
    }
    val addOne = BaseStep("addOne") { (input: Int, _: PipelineContext) =>
      Right(input + 1)
    }

    val composed = double.andThen(addOne)
    val ctx = PipelineContext.create("test-profile")

    val result = composed.run(5, ctx)

    assertEquals(result, Right(11)) // (5 * 2) + 1 = 11

  test("BaseStep.andThen should short-circuit on error"):
    val fail = BaseStep("fail") { (_: Int, _: PipelineContext) =>
      Left(StepError.ValidationError("failed", "fail"))
    }
    val addOne = BaseStep("addOne") { (input: Int, _: PipelineContext) =>
      Right(input + 1)
    }

    val composed = fail.andThen(addOne)
    val ctx = PipelineContext.create("test-profile")

    val result = composed.run(5, ctx)

    assert(result.isLeft)

  test("BaseStep should catch exceptions and return error"):
    val throwing = BaseStep("throwing") { (_: Int, _: PipelineContext) =>
      throw new RuntimeException("boom!")
    }
    val ctx = PipelineContext.create("test-profile")

    val result = throwing.run(5, ctx)

    assert(result.isLeft)
    result match
      case Left(error) =>
        assert(error.message.contains("Unexpected error"))
        assert(error.cause.isDefined)
      case Right(_) => fail("Expected error")
