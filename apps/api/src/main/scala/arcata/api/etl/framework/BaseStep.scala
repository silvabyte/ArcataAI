package arcata.api.etl.framework

import scala.util.{Failure, Success, Try}

import scribe.Logging

/**
 * Base trait for ETL pipeline steps.
 *
 * Each step takes an input of type I and produces an output of type O, or fails with a StepError.
 *
 * @tparam I
 *   Input type
 * @tparam O
 *   Output type
 */
trait BaseStep[I, O] extends Logging:
  /** The name of this step (used in error messages and logging). */
  def name: String

  /**
   * Execute the step logic.
   *
   * @param input
   *   The input data
   * @param ctx
   *   The pipeline context
   * @return
   *   Either a StepError or the output data
   */
  def execute(input: I, ctx: PipelineContext): Either[StepError, O]

  /**
   * Run the step with logging and timing.
   *
   * @param input
   *   The input data
   * @param ctx
   *   The pipeline context
   * @return
   *   Either a StepError or the output data
   */
  final def run(input: I, ctx: PipelineContext): Either[StepError, O] = {
    val startTime = System.currentTimeMillis()
    logger.info(s"[${ctx.runId}] Starting step: $name")

    val result = Try(execute(input, ctx)) match
      case Success(either) => either
      case Failure(e: StepException) => Left(e.error)
      case Failure(e) =>
        Left(
          StepError.UnexpectedError(
            message = s"Unexpected error in step $name: ${e.getMessage}",
            stepName = name,
            cause = Some(e),
          )
        )

    val duration = System.currentTimeMillis() - startTime
    result match
      case Right(_) =>
        logger.info(s"[${ctx.runId}] Completed step: $name in ${duration}ms")
      case Left(error) =>
        logger.error(s"[${ctx.runId}] Failed step: $name in ${duration}ms - ${error.message}")

    result
  }

  /** Compose this step with another step to form a pipeline segment. */
  def andThen[O2](next: BaseStep[O, O2]): BaseStep[I, O2] = {
    val self = this
    new BaseStep[I, O2]:
      def name: String = s"${self.name} -> ${next.name}"

      def execute(input: I, ctx: PipelineContext): Either[StepError, O2] =
        self.run(input, ctx).flatMap(next.run(_, ctx))
  }

/** Factory methods for creating BaseStep instances. */
object BaseStep:
  /**
   * Create a simple step from a function.
   *
   * @tparam I
   *   Input type for the step
   * @tparam O
   *   Output type for the step
   * @param stepName
   *   Name for logging and error reporting
   * @param f
   *   The step logic as a function from (input, context) to Either[StepError, output]
   * @return
   *   A configured BaseStep instance
   */
  def apply[I, O](
    stepName: String
  )(f: (I, PipelineContext) => Either[StepError, O]): BaseStep[I, O] = {
    new BaseStep[I, O]:
      def name: String = stepName
      def execute(input: I, ctx: PipelineContext): Either[StepError, O] = f(input, ctx)
  }

  /**
   * Create a step that always succeeds with the input unchanged (identity step).
   *
   * @tparam A
   *   The input/output type
   * @param stepName
   *   Name for logging and error reporting
   * @return
   *   A pass-through step that returns its input unchanged
   */
  def identity[A](stepName: String): BaseStep[A, A] =
    apply(stepName)((input, _) => Right(input))

  /**
   * Create a step that always fails with the given error.
   *
   * @tparam I
   *   Input type for the step
   * @tparam O
   *   Output type for the step
   * @param stepName
   *   Name for logging and error reporting
   * @param error
   *   The error to return
   * @return
   *   A step that always fails with the specified error
   */
  def fail[I, O](stepName: String, error: StepError): BaseStep[I, O] =
    apply(stepName)((_, _) => Left(error))
