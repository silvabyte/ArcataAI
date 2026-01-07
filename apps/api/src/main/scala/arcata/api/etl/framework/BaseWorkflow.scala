package arcata.api.etl.framework

import castor.SimpleActor
import scribe.Logging

/**
 * Message to trigger a workflow run.
 *
 * @tparam I
 *   Input type for the workflow
 * @param input
 *   The input data for the workflow
 * @param profileId
 *   The user profile ID (use "system" for cron/automated runs)
 */
case class WorkflowRun[I](input: I, profileId: String)

/**
 * Base trait for async workflows that wrap pipelines in actors.
 *
 * Workflows receive messages, execute the underlying pipeline, and handle results asynchronously
 * (fire-and-forget). They combine Castor's SimpleActor for async message processing with
 * BasePipeline for step orchestration.
 *
 * Usage:
 * {{{
 * class MyWorkflow(deps: Deps)(using ac: castor.Context)
 *     extends BaseWorkflow[MyInput, MyOutput]:
 *
 *   val name = "MyWorkflow"
 *
 *   override def execute(input: MyInput, ctx: PipelineContext): Either[StepError, MyOutput] =
 *     // orchestrate steps here
 *     for
 *       step1Result <- step1.run(input, ctx)
 *       step2Result <- step2.run(step1Result, ctx)
 *     yield step2Result
 * }}}
 *
 * Trigger via:
 * {{{
 * workflow.send(WorkflowRun(input, "system"))
 * }}}
 *
 * @tparam I
 *   Input type for the workflow/pipeline
 * @tparam O
 *   Output type for the workflow/pipeline
 */
abstract class BaseWorkflow[I, O](
  using
  ac: castor.Context
) extends SimpleActor[WorkflowRun[I]]
  with BasePipeline[I, O]
  with Logging:

  /**
   * Called when pipeline completes successfully.
   *
   * Override to customize success handling (e.g., send notifications, update metrics).
   *
   * @param result
   *   The successful pipeline result containing output and timing info
   */
  def onSuccess(result: PipelineResult[O]): Unit =
    logger.info(s"[$name] Workflow completed: runId=${result.runId}, duration=${result.durationMs}ms")

  /**
   * Called when pipeline fails.
   *
   * Override to customize failure handling (e.g., send alerts, retry logic).
   *
   * @param result
   *   The failed pipeline result containing error details and timing info
   */
  def onFailure(result: PipelineResult[O]): Unit =
    logger.error(s"[$name] Workflow failed: runId=${result.runId}, error=${result.error.map(_.message)}")

  /**
   * Actor's run method - executes pipeline asynchronously.
   *
   * This is called by Castor when a message is received. It runs the pipeline synchronously within
   * the actor (ensuring sequential processing), then calls onSuccess or onFailure.
   */
  override def run(msg: WorkflowRun[I]): Unit = {
    val result = this.run(msg.input, msg.profileId) // calls BasePipeline.run
    if result.isSuccess then onSuccess(result)
    else onFailure(result)
  }
