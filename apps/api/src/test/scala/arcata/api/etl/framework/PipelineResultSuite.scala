package arcata.api.etl.framework

import utest.*

object PipelineResultSuite extends TestSuite:
  val tests = Tests {
    test("success creates successful result") {
      val result = PipelineResult.success("run-1", "output", 100L)

      assert(result.isSuccess)
      assert(!result.isFailure)
      assert(result.output == Some("output"))
      assert(result.error == None)
      assert(result.runId == "run-1")
      assert(result.durationMs == 100L)
    }

    test("failure creates failed result") {
      val error = StepError.ValidationError("err", "step")
      val result = PipelineResult.failure[String]("run-2", error, 50L)

      assert(!result.isSuccess)
      assert(result.isFailure)
      assert(result.output == None)
      assert(result.error == Some(error))
      assert(result.runId == "run-2")
      assert(result.durationMs == 50L)
    }
  }
