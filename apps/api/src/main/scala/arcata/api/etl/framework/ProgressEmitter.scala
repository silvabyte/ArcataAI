package arcata.api.etl.framework

/**
 * Trait for emitting progress updates during pipeline execution.
 */
trait ProgressEmitter:
  /**
   * Emit a progress update.
   *
   * @param step Current step number (0-indexed)
   * @param totalSteps Total number of steps in this pipeline run
   * @param status Status identifier (e.g., "checking", "fetching", "complete", "error")
   * @param message Human-readable message
   */
  def emit(step: Int, totalSteps: Int, status: String, message: String): Unit

object ProgressEmitter:
  /** A no-op emitter that discards all progress updates. */
  val noop: ProgressEmitter = new ProgressEmitter:
    def emit(step: Int, totalSteps: Int, status: String, message: String): Unit = ()
