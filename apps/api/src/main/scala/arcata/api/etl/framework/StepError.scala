package arcata.api.etl.framework

/** Represents an error that occurred during ETL step execution. */
sealed trait StepError:
  def message: String
  def cause: Option[Throwable]
  def stepName: String

object StepError:
  /** Error during data extraction phase. */
  final case class ExtractionError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  /** Error during data transformation phase. */
  final case class TransformationError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  /** Error during data loading phase. */
  final case class LoadError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  /** Error due to invalid input data. */
  final case class ValidationError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  /** Error when a required resource is not found. */
  final case class NotFoundError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  /** Error due to network or connectivity issues. */
  final case class NetworkError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  /** Generic unexpected error. */
  final case class UnexpectedError(
    message: String,
    stepName: String,
    cause: Option[Throwable] = None,
  ) extends StepError

  extension (error: StepError)
    def toException: StepException = StepException(error)

/** Exception wrapper for StepError to enable throwing in imperative code. */
final case class StepException(error: StepError)
  extends Exception(error.message, error.cause.orNull)
