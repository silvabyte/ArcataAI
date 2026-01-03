package arcata.api.etl.greenhouse

import scala.util.Try

import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.*
import arcata.api.extraction.CompletionState
import upickle.default.*

// =============================================================================
// Greenhouse API Response Models
// =============================================================================

/** Location object from Greenhouse API. */
case class GreenhouseLocation(name: String) derives ReadWriter

/** Pay range information from Greenhouse API (when ?pay_transparency=true). */
case class GreenhousePayRange(
    min_cents: Option[Long] = None,
    max_cents: Option[Long] = None,
    currency_type: Option[String] = None,
    title: Option[String] = None
) derives ReadWriter

/**
 * Full job detail response from Greenhouse API.
 *
 * Endpoint: GET /v1/boards/{board_token}/jobs/{job_id}?pay_transparency=true
 *
 * Note: The `content` field contains HTML with escaped entities (e.g., `&lt;p&gt;`).
 *
 * All optional fields have default values to handle missing keys in the API response.
 */
case class GreenhouseJobDetail(
    id: Long,
    title: String,
    content: String,
    absolute_url: String,
    location: Option[GreenhouseLocation] = None,
    updated_at: Option[String] = None,
    internal_job_id: Option[Long] = None,
    requisition_id: Option[String] = None,
    pay_input_ranges: Option[Seq[GreenhousePayRange]] = None
) derives ReadWriter

// =============================================================================
// Step Input/Output
// =============================================================================

/** Input for the GreenhouseJobParser step. */
final case class GreenhouseJobParserInput(
    json: String,
    apiUrl: String,
    sourceUrl: String,
    companyId: Option[Long]
)

/** Output from the GreenhouseJobParser step. */
final case class GreenhouseJobParserOutput(
    extracted: ExtractedJobData,
    sourceUrl: String,
    companyId: Option[Long],
    completionState: CompletionState
)

/**
 * Parses Greenhouse API JSON into ExtractedJobData.
 *
 * This step converts structured data from the Greenhouse Job Board API directly into our domain
 * model, eliminating the need for AI extraction. This is faster, cheaper, and more reliable for
 * Greenhouse jobs.
 *
 * Mapping:
 *   - title -> title
 *   - location.name -> location
 *   - content -> description (HTML entities unescaped)
 *   - absolute_url -> applicationUrl
 *   - pay_input_ranges[0].min_cents / 100 -> salaryMin
 *   - pay_input_ranges[0].max_cents / 100 -> salaryMax
 *   - pay_input_ranges[0].currency_type -> salaryCurrency
 *   - Inferred from location -> isRemote
 *
 * Fields not available from Greenhouse API (left as None):
 *   - companyName (we already know the company from discovery)
 *   - jobType, experienceLevel, educationLevel
 *   - qualifications, preferredQualifications, responsibilities, benefits
 *   - category, postedDate, closingDate
 */
object GreenhouseJobParser extends BaseStep[GreenhouseJobParserInput, GreenhouseJobParserOutput]:

  val name = "GreenhouseJobParser"

  override def execute(
      input: GreenhouseJobParserInput,
      ctx: PipelineContext
  ): Either[StepError, GreenhouseJobParserOutput] = {
    logger.info(s"[${ctx.runId}] Parsing Greenhouse job JSON")

    Try(read[GreenhouseJobDetail](input.json)).toEither match
      case Left(e) =>
        Left(
          StepError.ExtractionError(
            message = s"Failed to parse Greenhouse JSON: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Right(job) =>
        val extracted = mapToExtractedJobData(job)
        val completionState = evaluateCompletionState(extracted)

        logger.info(
          s"[${ctx.runId}] Parsed Greenhouse job: ${job.title} (completion: $completionState)"
        )

        Right(
          GreenhouseJobParserOutput(
            extracted = extracted,
            sourceUrl = input.sourceUrl,
            companyId = input.companyId,
            completionState = completionState
          )
        )
  }

  /**
   * Map Greenhouse job detail to ExtractedJobData.
   */
  private def mapToExtractedJobData(job: GreenhouseJobDetail): ExtractedJobData = {
    val locationName = job.location.map(_.name)
    val payRange = job.pay_input_ranges.flatMap(_.headOption)

    ExtractedJobData(
      title = job.title,
      companyName = None, // We already know the company from discovery
      description = Some(unescapeHtml(job.content)),
      location = locationName,
      jobType = None,
      experienceLevel = None,
      educationLevel = None,
      salaryMin = payRange.flatMap(_.min_cents).map(centsToDoallars),
      salaryMax = payRange.flatMap(_.max_cents).map(centsToDoallars),
      salaryCurrency = payRange.flatMap(_.currency_type),
      qualifications = None,
      preferredQualifications = None,
      responsibilities = None,
      benefits = None,
      category = None,
      applicationUrl = Some(job.absolute_url),
      isRemote = locationName.map(inferIsRemote),
      postedDate = None,
      closingDate = None
    )
  }

  /**
   * Unescape HTML entities in content.
   *
   * Greenhouse API returns HTML with escaped entities like `&lt;p&gt;`. We need to unescape these
   * to get proper HTML for display.
   *
   * Handles the most common HTML entities. For full HTML entity support, consider adding
   * commons-text dependency.
   */
  private def unescapeHtml(content: String): String = {
    content
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&amp;", "&")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace("&apos;", "'")
      .replace("&nbsp;", " ")
  }

  /**
   * Convert cents to dollars.
   *
   * Greenhouse API returns salary in cents (e.g., 5000000 = $50,000). Our domain model stores
   * salary as integer dollars.
   */
  private def centsToDoallars(cents: Long): Int =
    (cents / 100).toInt

  /**
   * Infer if job is remote based on location string.
   *
   * Common patterns: "Remote", "USA - Remote", "Remote - NYC", etc.
   */
  private def inferIsRemote(location: String): Boolean =
    location.toLowerCase.contains("remote")

  /**
   * Evaluate the completion state based on extracted data.
   *
   * Greenhouse provides structured data but lacks some fields we'd get from AI extraction (like
   * qualifications, responsibilities, etc.). We mark these as "Sufficient" since we have all the
   * key fields.
   */
  private def evaluateCompletionState(data: ExtractedJobData): CompletionState = {
    val hasTitle = data.title.nonEmpty
    val hasDescription = data.description.exists(_.nonEmpty)
    val hasLocation = data.location.exists(_.nonEmpty)
    val hasApplicationUrl = data.applicationUrl.exists(_.nonEmpty)
    val hasSalary = data.salaryMin.isDefined || data.salaryMax.isDefined

    if !hasTitle then CompletionState.Failed
    else if hasDescription && hasLocation && hasApplicationUrl then
      if hasSalary then CompletionState.Complete
      else CompletionState.Sufficient
    else if hasDescription then CompletionState.Partial
    else CompletionState.Minimal
  }
