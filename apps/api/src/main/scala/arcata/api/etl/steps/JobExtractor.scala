package arcata.api.etl.steps

import arcata.api.ai.JobExtractionAgent
import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.*
import arcata.api.extraction.{CompletionScorer, CompletionState}

/** Input for the JobExtractor step. */
final case class JobExtractorInput(
  content: String,
  url: String,
  objectId: Option[String],
)

/** Output from the JobExtractor step. */
final case class JobExtractorOutput(
  extractedData: ExtractedJobData,
  url: String,
  objectId: Option[String],
  completionState: CompletionState,
)

/**
 * Extracts job data from cleaned content using AI extraction.
 *
 * Uses the JobExtractionAgent to intelligently parse job posting content
 * and extract structured data. The AI reads cleaned markdown (with preserved
 * JSON-LD structured data) and outputs structured ExtractedJobData.
 *
 * This approach works better than config-driven extraction because:
 * - AI understands content semantically, not just structurally
 * - No need to generate/maintain CSS selectors or JSONPath rules
 * - Handles varied page structures automatically
 * - JSON-LD schema.org data provides high-quality structured hints
 */
final class JobExtractor(aiConfig: AIConfig) extends BaseStep[JobExtractorInput, JobExtractorOutput]:

  val name = "JobExtractor"

  private val extractionAgent = JobExtractionAgent(aiConfig)

  override def execute(
    input: JobExtractorInput,
    ctx: PipelineContext,
  ): Either[StepError, JobExtractorOutput] = {
    logger.info(s"[${ctx.runId}] Extracting job data from: ${input.url}")
    logger.debug(s"[${ctx.runId}] Content length: ${input.content.length} chars")
    logger.debug(input.content)

    extractionAgent.extract(input.content, input.url) match
      case Right(extractedData) =>
        val scoringResult = CompletionScorer.scoreExtractedData(extractedData)
        logger.info(s"[${ctx.runId}] Extraction successful: ${scoringResult.summary}")

        Right(
          JobExtractorOutput(
            extractedData = extractedData,
            url = input.url,
            objectId = input.objectId,
            completionState = scoringResult.state,
          )
        )

      case Left(schemaError) =>
        val errorMessage = schemaError.toString
        logger.error(s"[${ctx.runId}] AI extraction failed: $errorMessage")
        Left(
          StepError.TransformationError(
            message = s"AI extraction failed: $errorMessage",
            stepName = name,
            cause = Some(new Exception(errorMessage)),
          )
        )
  }

object JobExtractor:
  def apply(aiConfig: AIConfig): JobExtractor =
    new JobExtractor(aiConfig)
