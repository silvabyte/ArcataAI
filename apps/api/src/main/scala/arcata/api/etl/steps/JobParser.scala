package arcata.api.etl.steps

import arcata.api.ai.JobExtractionAgent
import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.*

/** Input for the JobParser step. */
final case class JobParserInput(
    html: String,
    url: String,
    objectId: Option[String]
)

/** Output from the JobParser step. */
final case class JobParserOutput(
    extractedData: ExtractedJobData,
    url: String,
    objectId: Option[String]
)

/**
 * Parses job HTML using AI to extract structured job data.
 *
 * Uses JobExtractionAgent with Vercel AI Gateway for intelligent extraction.
 */
final class JobParser(aiConfig: AIConfig) extends BaseStep[JobParserInput, JobParserOutput]:

  val name = "JobParser"

  private val agent = JobExtractionAgent(aiConfig)

  override def execute(
      input: JobParserInput,
      ctx: PipelineContext
  ): Either[StepError, JobParserOutput] = {
    logger.info(s"[${ctx.runId}] Parsing job from: ${input.url}")

    agent.extract(input.html, input.url) match
      case Right(extractedData) =>
        logger.info(s"[${ctx.runId}] Extracted job: ${extractedData.title}")
        Right(
          JobParserOutput(
            extractedData = extractedData,
            url = input.url,
            objectId = input.objectId
          )
        )

      case Left(schemaError) =>
        Left(
          StepError.TransformationError(
            message = s"AI extraction failed: ${schemaError.message}",
            stepName = name,
            cause = Some(schemaError)
          )
        )
  }

object JobParser:
  def apply(aiConfig: AIConfig): JobParser =
    new JobParser(aiConfig)
