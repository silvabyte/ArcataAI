package arcata.api.etl.framework

import scala.util.{Failure, Success, Try}

import scribe.Logging

/** Result of a pipeline execution. */
final case class PipelineResult[O](
  runId: String,
  output: Option[O],
  error: Option[StepError],
  durationMs: Long,
):
  def isSuccess: Boolean = output.isDefined && error.isEmpty
  def isFailure: Boolean = error.isDefined

object PipelineResult:
  def success[O](runId: String, output: O, durationMs: Long): PipelineResult[O] =
    PipelineResult(runId, Some(output), None, durationMs)

  def failure[O](runId: String, error: StepError, durationMs: Long): PipelineResult[O] =
    PipelineResult(runId, None, Some(error), durationMs)

/**
 * Base trait for ETL pipelines that orchestrate multiple steps.
 *
 * @tparam I
 *   Input type for the pipeline
 * @tparam O
 *   Output type for the pipeline
 */
trait BasePipeline[I, O] extends Logging:
  /** The name of this pipeline. */
  def name: String

  /**
   * Execute the pipeline logic.
   *
   * @param input
   *   The input data
   * @param ctx
   *   The pipeline context
   * @return
   *   Either a StepError or the output
   */
  def execute(input: I, ctx: PipelineContext): Either[StepError, O]

  /**
   * Run the pipeline with a new context created for the given profile.
   *
   * @param input
   *   The input data
   * @param profileId
   *   The user profile ID
   * @return
   *   The pipeline result
   */
  final def run(input: I, profileId: String): PipelineResult[O] = {
    val ctx = PipelineContext.create(profileId)
    runWithContext(input, ctx)
  }

  /**
   * Run the pipeline with a provided context.
   *
   * @param input
   *   The input data
   * @param ctx
   *   The pipeline context
   * @return
   *   The pipeline result
   */
  final def runWithContext(input: I, ctx: PipelineContext): PipelineResult[O] = {
    val startTime = System.currentTimeMillis()
    logger.info(s"[${ctx.runId}] Starting pipeline: $name")

    val result = Try(execute(input, ctx)) match
      case Success(either) => either
      case Failure(e: StepException) => Left(e.error)
      case Failure(e) =>
        Left(
          StepError.UnexpectedError(
            message = s"Unexpected error in pipeline $name: ${e.getMessage}",
            stepName = name,
            cause = Some(e),
          )
        )

    val duration = System.currentTimeMillis() - startTime

    result match
      case Right(output) =>
        logger.info(s"[${ctx.runId}] Completed pipeline: $name in ${duration}ms")
        PipelineResult.success(ctx.runId, output, duration)
      case Left(error) =>
        logger.error(
          s"[${ctx.runId}] Failed pipeline: $name in ${duration}ms - ${error.message}"
        )
        PipelineResult.failure(ctx.runId, error, duration)
  }

object BasePipeline:
  /** Create a pipeline from a single step. */
  def fromStep[I, O](pipelineName: String, step: BaseStep[I, O]): BasePipeline[I, O] = {
    new BasePipeline[I, O]:
      def name: String = pipelineName
      def execute(input: I, ctx: PipelineContext): Either[StepError, O] =
        step.run(input, ctx)
  }

  /** Create a pipeline from a composed step chain. */
  def fromSteps[I, O](pipelineName: String)(
    buildSteps: => BaseStep[I, O]
  ): BasePipeline[I, O] = {
    new BasePipeline[I, O]:
      def name: String = pipelineName
      def execute(input: I, ctx: PipelineContext): Either[StepError, O] =
        buildSteps.run(input, ctx)
  }
