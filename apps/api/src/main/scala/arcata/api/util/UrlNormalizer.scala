package arcata.api.util

import java.net.URI
import scala.util.Try

/**
 * Utility for normalizing URLs for deduplication purposes.
 *
 * Job URLs from different sources may have query params, tracking codes, or slight variations that
 * should resolve to the same job. This normalizer strips those variations to enable reliable
 * deduplication.
 */
object UrlNormalizer:

  /**
   * Normalize a URL for deduplication purposes.
   *
   * Transformations applied:
   *   - Removes query parameters
   *   - Removes fragment (#anchor)
   *   - Lowercases scheme and host
   *   - Removes trailing slashes from path
   *
   * @param url
   *   The raw URL to normalize
   * @return
   *   Normalized URL string, or original if parsing fails
   */
  def normalize(url: String): String = {
    Try {
      val uri = URI.create(url.trim)
      val scheme = Option(uri.getScheme).map(_.toLowerCase).getOrElse("https")
      val host = Option(uri.getHost).map(_.toLowerCase).getOrElse("")
      val port = uri.getPort match
        case -1 => ""
        case 80 if scheme == "http" => ""
        case 443 if scheme == "https" => ""
        case p => s":$p"
      val path = Option(uri.getPath)
        .map(_.replaceAll("/+$", "")) // Remove trailing slashes
        .filter(_.nonEmpty)
        .getOrElse("")

      s"$scheme://$host$port$path"
    }.getOrElse(url)
  }

  /**
   * Extract the company identifier from a Greenhouse URL.
   *
   * Supports various Greenhouse URL patterns:
   *   - https://boards.greenhouse.io/{company}
   *   - https://job-boards.greenhouse.io/{company}
   *   - https://boards-api.greenhouse.io/v1/boards/{company}/jobs
   *
   * @param url
   *   A Greenhouse URL
   * @return
   *   The company identifier if found
   */
  def extractGreenhouseCompanyId(url: String): Option[String] = {
    Try {
      val uri = URI.create(url.trim)
      val host = Option(uri.getHost).map(_.toLowerCase).getOrElse("")
      val path = Option(uri.getPath).getOrElse("")

      if isGreenhouseHost(host) then extractCompanyFromPath(path, host)
      else None
    }.getOrElse(None)
  }

  /**
   * Check if a URL is a Greenhouse job board URL.
   *
   * @param url
   *   URL to check
   * @return
   *   true if this is a Greenhouse URL
   */
  def isGreenhouseUrl(url: String): Boolean = {
    Try {
      val uri = URI.create(url.trim)
      val host = Option(uri.getHost).map(_.toLowerCase).getOrElse("")
      isGreenhouseHost(host)
    }.getOrElse(false)
  }

  private def isGreenhouseHost(host: String): Boolean = {
    host == "boards.greenhouse.io" ||
    host == "job-boards.greenhouse.io" ||
    host == "boards-api.greenhouse.io"
  }

  private def extractCompanyFromPath(path: String, host: String): Option[String] = {
    val segments = path.split("/").filter(_.nonEmpty).toList

    host match
      case "boards-api.greenhouse.io" =>
        // Pattern: /v1/boards/{company}/jobs
        segments match
          case "v1" :: "boards" :: company :: _ => Some(company)
          case _ => None

      case _ =>
        // Pattern: /{company} or /{company}/jobs/...
        segments.headOption
  }
