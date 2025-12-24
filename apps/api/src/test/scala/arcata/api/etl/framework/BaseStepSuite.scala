package arcata.api.etl.framework

import utest.*

object BaseStepSuite extends TestSuite:
  val tests = Tests {
    test("identity step passes input through unchanged") {
      val step = BaseStep.identity[String]("identity")
      val ctx = PipelineContext.create("test-profile")

      val result = step.run("hello", ctx)

      assert(result == Right("hello"))
    }

    test("fail step returns error") {
      val error = StepError.ValidationError("test error", "fail")
      val step = BaseStep.fail[String, Int]("fail", error)
      val ctx = PipelineContext.create("test-profile")

      val result = step.run("input", ctx)

      assert(result == Left(error))
    }

    test("apply creates step from function") {
      val step = BaseStep("double") { (input: Int, _: PipelineContext) =>
        Right(input * 2)
      }
      val ctx = PipelineContext.create("test-profile")

      val result = step.run(5, ctx)

      assert(result == Right(10))
    }

    test("andThen composes steps") {
      val double = BaseStep("double") { (input: Int, _: PipelineContext) =>
        Right(input * 2)
      }
      val addOne = BaseStep("addOne") { (input: Int, _: PipelineContext) =>
        Right(input + 1)
      }

      val composed = double.andThen(addOne)
      val ctx = PipelineContext.create("test-profile")

      val result = composed.run(5, ctx)

      // (5 * 2) + 1 = 11
      assert(result == Right(11))
    }

    test("andThen short-circuits on error") {
      val failStep: BaseStep[Int, Int] = BaseStep("fail") { (_: Int, _: PipelineContext) =>
        Left(StepError.ValidationError("failed", "fail"))
      }
      val addOne: BaseStep[Int, Int] = BaseStep("addOne") { (input: Int, _: PipelineContext) =>
        Right(input + 1)
      }

      val composed = failStep.andThen(addOne)
      val ctx = PipelineContext.create("test-profile")

      val result = composed.run(5, ctx)

      assert(result.isLeft)
    }

    test("step catches exceptions and returns error") {
      val throwing = BaseStep("throwing") { (_: Int, _: PipelineContext) =>
        throw new RuntimeException("boom!") // scalafix:ok DisableSyntax.throw
      }
      val ctx = PipelineContext.create("test-profile")

      val result = throwing.run(5, ctx)

      assert(result.isLeft)
      result match
        case Left(error) =>
          assert(error.message.contains("Unexpected error"))
          assert(error.cause.isDefined)
        case Right(_) =>
          throw new java.lang.AssertionError("Expected error") // scalafix:ok DisableSyntax.throw
    }

    test("composed step name includes both step names") {
      val step1 = BaseStep.identity[Int]("first")
      val step2 = BaseStep.identity[Int]("second")
      val composed = step1.andThen(step2)

      assert(composed.name == "first -> second")
    }
  }
