package arcata.api.etl.steps

import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.*
import arcata.api.extraction.CompletionState

/** Input for the JobTransformer step. */
final case class JobTransformerInput(
  extracted: ExtractedJobData,
  sourceUrl: String,
  objectId: Option[String],
  completionState: CompletionState,
)

/** Output from the JobTransformer step. */
final case class JobTransformerOutput(
  transformed: Transformed[ExtractedJobData],
  sourceUrl: String,
  objectId: Option[String],
  completionState: CompletionState,
)

/**
 * Transforms and sanitizes extracted job data.
 *
 * This step performs best-effort data cleaning:
 *   - Trims whitespace from strings
 *   - Converts empty strings to None
 *   - Removes empty entries from lists
 *
 * The step never fails - it always returns Right with sanitized data. Validation of required
 * fields happens in the Loader step, which is the final gate before persistence.
 *
 * The output is wrapped in Transformed to provide compile-time safety that unsanitized data cannot
 * bypass this step and reach the loader directly.
 */
object JobTransformer extends BaseStep[JobTransformerInput, JobTransformerOutput]:

  override val name: String = "JobTransformer"

  override def execute(
    input: JobTransformerInput,
    ctx: PipelineContext,
  ): Either[StepError, JobTransformerOutput] = {
    logger.info(s"[${ctx.runId}] Transforming job data for: ${input.sourceUrl}")

    val transformed = ExtractedJobData(
      title = input.extracted.title.trim,
      companyName = input.extracted.companyName.flatMap(sanitizeString),
      description = input.extracted.description.flatMap(sanitizeString),
      location = input.extracted.location.flatMap(sanitizeString),
      jobType = input.extracted.jobType.flatMap(sanitizeString),
      experienceLevel = input.extracted.experienceLevel.flatMap(sanitizeString),
      educationLevel = input.extracted.educationLevel.flatMap(sanitizeString),
      salaryMin = input.extracted.salaryMin,
      salaryMax = input.extracted.salaryMax,
      salaryCurrency = input.extracted.salaryCurrency.flatMap(sanitizeString),
      qualifications = input.extracted.qualifications.map(sanitizeList),
      preferredQualifications = input.extracted.preferredQualifications.map(sanitizeList),
      responsibilities = input.extracted.responsibilities.map(sanitizeList),
      benefits = input.extracted.benefits.map(sanitizeList),
      category = input.extracted.category.flatMap(sanitizeString),
      applicationUrl = input.extracted.applicationUrl.flatMap(sanitizeString),
      isRemote = input.extracted.isRemote,
      postedDate = input.extracted.postedDate.flatMap(sanitizeString),
      closingDate = input.extracted.closingDate.flatMap(sanitizeString),
    )

    Right(
      JobTransformerOutput(
        transformed = Transformed(transformed),
        sourceUrl = input.sourceUrl,
        objectId = input.objectId,
        completionState = input.completionState,
      )
    )
  }

  /**
   * Sanitizes an optional string value.
   *
   * @return
   *   None if the string is null, empty, or whitespace-only; Some(trimmed) otherwise
   */
  private def sanitizeString(s: String): Option[String] =
    Option(s).map(_.trim).filter(_.nonEmpty)

  /**
   * Sanitizes a list of strings by removing empty entries.
   *
   * @return
   *   A list with all empty/whitespace entries removed and remaining entries trimmed
   */
  private def sanitizeList(list: List[String]): List[String] =
    list.flatMap(sanitizeString)
