package arcata.api.etl.steps

import scala.util.{Failure, Success, Try}

import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedJobData
import arcata.api.etl.framework.*
import requests.*
import upickle.default.*

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

/** Response from the BoogieLoops AI extraction API. */
private final case class BoogieLoopsResponse(
    success: Boolean,
    data: Option[ExtractedJobData],
    error: Option[String]
) derives ReadWriter

/**
 * Parses job HTML using BoogieLoops AI to extract structured job data.
 *
 * This is a transformation step that uses AI to extract job details from raw HTML.
 */
final class JobParser(config: AIConfig) extends BaseStep[JobParserInput, JobParserOutput]:

  val name = "JobParser"

  override def execute(
      input: JobParserInput,
      ctx: PipelineContext
  ): Either[StepError, JobParserOutput] = {
    logger.info(s"[${ctx.runId}] Parsing job from: ${input.url}")

    val requestBody = ujson.Obj(
      "html" -> input.html,
      "url" -> input.url,
      "extraction_type" -> "job_posting"
    )

    val apiResult = Try {
      requests.post(
        s"${config.baseUrl}/extract",
        headers = Map(
          "Authorization" -> s"Bearer ${config.apiKey}",
          "Content-Type" -> "application/json"
        ),
        data = ujson.write(requestBody),
        readTimeout = 60000, // AI extraction can take time
        connectTimeout = 10000
      )
    }

    apiResult match
      case Failure(e: java.net.SocketTimeoutException) =>
        Left(
          StepError.NetworkError(
            message = "BoogieLoops API timeout",
            stepName = name,
            cause = Some(e)
          )
        )

      case Failure(e) =>
        Left(
          StepError.TransformationError(
            message = s"Job parsing failed: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Success(response) if !response.is2xx =>
        Left(
          StepError.TransformationError(
            message = s"BoogieLoops API error: HTTP ${response.statusCode}",
            stepName = name
          )
        )

      case Success(response) =>
        parseResponse(response.text(), input, ctx)
  }

  private def parseResponse(
      responseText: String,
      input: JobParserInput,
      ctx: PipelineContext
  ): Either[StepError, JobParserOutput] = {
    Try(read[BoogieLoopsResponse](responseText)) match
      case Failure(e: upickle.core.AbortException) =>
        Left(
          StepError.TransformationError(
            message = s"Failed to parse BoogieLoops response: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Failure(e) =>
        Left(
          StepError.TransformationError(
            message = s"Unexpected error parsing response: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Success(BoogieLoopsResponse(true, Some(data), _)) =>
        logger.info(s"[${ctx.runId}] Extracted job: ${data.title}")
        Right(
          JobParserOutput(
            extractedData = data,
            url = input.url,
            objectId = input.objectId
          )
        )

      case Success(BoogieLoopsResponse(false, _, Some(error))) =>
        Left(
          StepError.TransformationError(
            message = s"BoogieLoops extraction failed: $error",
            stepName = name
          )
        )

      case Success(_) =>
        Left(
          StepError.TransformationError(
            message = "BoogieLoops returned unexpected response",
            stepName = name
          )
        )
  }

object JobParser:
  def apply(config: AIConfig): JobParser =
    new JobParser(config)

  /** Create a parser with API URL and key directly. */
  def apply(baseUrl: String, apiKey: String, model: String = "anthropic/claude-sonnet-4-20250514"): JobParser =
    new JobParser(AIConfig(baseUrl, apiKey, model))
