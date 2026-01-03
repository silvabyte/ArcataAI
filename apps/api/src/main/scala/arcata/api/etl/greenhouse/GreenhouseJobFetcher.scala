package arcata.api.etl.greenhouse

import scala.util.{Failure, Success, Try}

import arcata.api.etl.framework.*

/** Input for the GreenhouseJobFetcher step. */
final case class GreenhouseJobFetcherInput(
    apiUrl: String,
    sourceUrl: String,
    companyId: Option[Long]
)

/** Output from the GreenhouseJobFetcher step. */
final case class GreenhouseJobFetcherOutput(
    json: String,
    apiUrl: String,
    sourceUrl: String,
    companyId: Option[Long]
)

/**
 * Fetches job details from the Greenhouse Job Board API.
 *
 * This step retrieves structured JSON data from the Greenhouse API, which can then be parsed
 * directly without needing AI extraction.
 *
 * API endpoint: `GET boards-api.greenhouse.io/v1/boards/{board_token}/jobs/{job_id}?pay_transparency=true`
 *
 * The `pay_transparency=true` parameter includes salary range information when available.
 */
final class GreenhouseJobFetcher(
    userAgent: String = "Mozilla/5.0 (compatible; ArcataBot/1.0)",
    timeoutMs: Int = 10000
) extends BaseStep[GreenhouseJobFetcherInput, GreenhouseJobFetcherOutput]:

  val name = "GreenhouseJobFetcher"

  override def execute(
      input: GreenhouseJobFetcherInput,
      ctx: PipelineContext
  ): Either[StepError, GreenhouseJobFetcherOutput] = {
    // Add pay_transparency=true to get salary data
    val urlWithParams = {
      if input.apiUrl.contains("?") then s"${input.apiUrl}&pay_transparency=true"
      else s"${input.apiUrl}?pay_transparency=true"
    }

    logger.info(s"[${ctx.runId}] Fetching Greenhouse job from: $urlWithParams")

    Try {
      requests.get(
        urlWithParams,
        headers = Map(
          "User-Agent" -> userAgent,
          "Accept" -> "application/json"
        ),
        readTimeout = timeoutMs,
        connectTimeout = timeoutMs,
        check = false
      )
    } match
      case Failure(e: java.net.SocketTimeoutException) =>
        Left(
          StepError.NetworkError(
            message = s"Timeout fetching Greenhouse job: ${input.apiUrl}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Failure(e: java.net.UnknownHostException) =>
        Left(
          StepError.NetworkError(
            message = s"Unknown host: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Failure(e) =>
        Left(
          StepError.ExtractionError(
            message = s"Failed to fetch Greenhouse job: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Success(response) if !response.is2xx =>
        Left(
          StepError.NetworkError(
            message = s"HTTP ${response.statusCode} when fetching Greenhouse job: ${input.apiUrl}",
            stepName = name
          )
        )

      case Success(response) =>
        val json = response.text()
        logger.debug(s"[${ctx.runId}] Received ${json.length} bytes from Greenhouse API")

        Right(
          GreenhouseJobFetcherOutput(
            json = json,
            apiUrl = input.apiUrl,
            sourceUrl = input.sourceUrl,
            companyId = input.companyId
          )
        )
  }

object GreenhouseJobFetcher:
  def apply(
      userAgent: String = "Mozilla/5.0 (compatible; ArcataBot/1.0)",
      timeoutMs: Int = 10000
  ): GreenhouseJobFetcher =
    new GreenhouseJobFetcher(userAgent, timeoutMs)
