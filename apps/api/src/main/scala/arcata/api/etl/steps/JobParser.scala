package arcata.api.etl.steps

import arcata.api.ai.JobExtractionAgent
import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.*
import boogieloops.ai.SchemaError

/** Input for the JobParser step. */
final case class JobParserInput(
    content: String,
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
 * Parses job content using AI to extract structured job data.
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

    agent.extract(input.content, input.url) match
      case Right(extractedData) =>
        logger.info(s"[${ctx.runId}] Extracted job: ${extractedData.title}")
        Right(
          JobParserOutput(
            extractedData = extractedData,
            url = input.url,
            objectId = input.objectId
          )
        )

      case Left(schemaError: SchemaError) =>
        val errorMessage: String = schemaError match
          case SchemaError.NetworkError(msg, _) => msg
          case SchemaError.ParseError(msg, _) => msg
          case SchemaError.ModelNotSupported(model, prov, _) => s"Model $model not supported by $prov"
          case SchemaError.ApiError(msg, _, _) => msg
          case SchemaError.SchemaConversionError(msg, _) => msg
          case SchemaError.ConfigurationError(msg) => msg
        Left(
          StepError.TransformationError(
            message = s"AI extraction failed: $errorMessage",
            stepName = name,
            cause = Some(new Exception(errorMessage))
          )
        )
  }

object JobParser:
  def apply(aiConfig: AIConfig): JobParser =
    new JobParser(aiConfig)
