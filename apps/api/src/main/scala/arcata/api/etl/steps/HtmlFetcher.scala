package arcata.api.etl.steps

import scala.util.{Failure, Success, Try}

import arcata.api.clients.{ObjectStorageClient, StorageError}
import arcata.api.etl.framework.*
import requests.*

import java.net.URI

/** Input for the HtmlFetcher step. */
final case class HtmlFetcherInput(
    url: String,
    profileId: String
)

/** Output from the HtmlFetcher step. */
final case class HtmlFetcherOutput(
    url: String,
    html: String,
    objectId: Option[String],
    contentType: String
)

/**
 * Fetches HTML content from a URL and optionally stores it in object storage.
 *
 * This is an extraction step that retrieves the raw HTML from a job posting URL.
 */
final class HtmlFetcher(
    storageClient: Option[ObjectStorageClient] = None,
    userAgent: String = "Mozilla/5.0 (compatible; ArcataBot/1.0)",
    timeoutMs: Int = 30000
) extends BaseStep[HtmlFetcherInput, HtmlFetcherOutput]:

  val name = "HtmlFetcher"

  override def execute(
      input: HtmlFetcherInput,
      ctx: PipelineContext
  ): Either[StepError, HtmlFetcherOutput] = {
    logger.info(s"[${ctx.runId}] Fetching HTML from: ${input.url}")

    Try {
      requests.get(
        input.url,
        headers = Map(
          "User-Agent" -> userAgent,
          "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
          "Accept-Language" -> "en-US,en;q=0.5"
        ),
        readTimeout = timeoutMs,
        connectTimeout = timeoutMs,
        check = false // Don't throw on non-2xx
      )
    } match
      case Failure(e: java.net.SocketTimeoutException) =>
        Left(
          StepError.NetworkError(
            message = s"Timeout fetching ${input.url}",
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
            message = s"Failed to fetch HTML: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )

      case Success(response) if !response.is2xx =>
        Left(
          StepError.NetworkError(
            message = s"HTTP ${response.statusCode} when fetching ${input.url}",
            stepName = name
          )
        )

      case Success(response) =>
        val html = response.text()
        val contentType = response.headers
          .getOrElse("content-type", Seq("text/html"))
          .headOption
          .getOrElse("text/html")

        // Optionally store in object storage
        val objectId = storageClient.flatMap { client =>
          val fileName = generateFileName(input.url)
          client.upload(html.getBytes("UTF-8"), fileName, Some("text/html"), input.profileId) match
            case Right(stored) =>
              logger.info(s"[${ctx.runId}] Stored HTML with ID: ${stored.objectId}")
              Some(stored.objectId)
            case Left(error) =>
              logger.warn(s"[${ctx.runId}] Failed to store HTML: $error")
              None
        }

        Right(
          HtmlFetcherOutput(
            url = input.url,
            html = html,
            objectId = objectId,
            contentType = contentType
          )
        )
  }

  private def generateFileName(url: String): String = {
    val host = Try(new URI(url).getHost).getOrElse("unknown")
    val timestamp = System.currentTimeMillis()
    s"$host-$timestamp.html"
  }

object HtmlFetcher:
  def apply(
      storageClient: Option[ObjectStorageClient] = None,
      userAgent: String = "Mozilla/5.0 (compatible; ArcataBot/1.0)",
      timeoutMs: Int = 30000
  ): HtmlFetcher =
    new HtmlFetcher(storageClient, userAgent, timeoutMs)
