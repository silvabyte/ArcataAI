package arcata.api.etl.greenhouse

import arcata.api.clients.SupabaseClient
import arcata.api.etl.sources.{DiscoveredJob, JobSource, JobSourceConfig}
import arcata.api.util.UrlNormalizer
import scribe.Logging
import upickle.default.*

import scala.util.Try

/**
 * Greenhouse-specific job discovery logic.
 *
 * Discovers job URLs from the Greenhouse public API:
 *   - List endpoint: `boards-api.greenhouse.io/v1/boards/{company}/jobs`
 *   - Detail endpoint: `boards-api.greenhouse.io/v1/boards/{company}/jobs/{id}`
 *
 * The discovery phase only uses the list endpoint to find job URLs. The detail endpoint is used
 * later by `GreenhouseIngestionPipeline` to fetch structured job data without needing AI extraction.
 */
object GreenhouseDiscovery extends Logging {

  /** Greenhouse list API response - minimal fields for discovery. */
  private case class GreenhouseJob(
    id: Long,
    title: String,
    absolute_url: String,
  ) derives ReadWriter

  private case class GreenhouseJobsResponse(
    jobs: Seq[GreenhouseJob]
  ) derives ReadWriter

  /**
   * Discover jobs from Greenhouse ATS for all companies with Greenhouse as their job source.
   *
   * @param supabase
   *   Client for querying companies table
   * @param config
   *   Rate limiting configuration
   * @return
   *   Sequence of discovered jobs with API URLs for optimized ingestion
   */
  def discoverJobs(supabase: SupabaseClient, config: JobSourceConfig): Seq[DiscoveredJob] = {
    val companies = supabase.findCompaniesByJobSource("greenhouse", config.companyBatchSize)
    logger.info(s"[Greenhouse] Found ${companies.size} companies with Greenhouse source")

    companies.flatMap {
      company =>
        company.companyJobsUrl.flatMap(UrlNormalizer.extractGreenhouseCompanyId) match
          case Some(ghCompanyId) =>
            logger.debug(s"[Greenhouse] Fetching jobs for company: $ghCompanyId")
            fetchJobs(ghCompanyId, company.companyId, config.jobsPerCompany)
          case None =>
            logger.warn(s"[Greenhouse] Could not extract company ID from ${company.companyJobsUrl}")
            Seq.empty
    }
  }

  /**
   * Fetch job listings from Greenhouse API for a specific company.
   *
   * @param ghCompanyId
   *   The Greenhouse board token (company identifier)
   * @param companyId
   *   Our internal company ID (for linking)
   * @param limit
   *   Maximum number of jobs to return
   * @return
   *   Discovered jobs with API URLs for structured data fetching
   */
  private def fetchJobs(
    ghCompanyId: String,
    companyId: Option[Long],
    limit: Int,
  ): Seq[DiscoveredJob] = {
    val listApiUrl = s"https://boards-api.greenhouse.io/v1/boards/$ghCompanyId/jobs"

    Try {
      val response = requests.get(listApiUrl, readTimeout = 10000, connectTimeout = 5000)

      if response.is2xx then
        val parsed = read[GreenhouseJobsResponse](response.text())
        val jobs = parsed.jobs.take(limit).map {
          job =>
            // Build the job detail API URL for structured data fetching
            val jobDetailApiUrl = s"https://boards-api.greenhouse.io/v1/boards/$ghCompanyId/jobs/${job.id}"
            DiscoveredJob(
              url = UrlNormalizer.normalize(job.absolute_url),
              source = JobSource.Greenhouse,
              companyId = companyId,
              apiUrl = Some(jobDetailApiUrl),
              metadata = Map(
                "greenhouse_job_id" -> job.id.toString,
                "greenhouse_company_id" -> ghCompanyId,
                "title" -> job.title,
              ),
            )
        }
        logger.info(s"[Greenhouse] Discovered ${jobs.size} jobs from $ghCompanyId")
        jobs
      else {
        logger.error(s"[Greenhouse] API error for $ghCompanyId: ${response.statusCode}")
        Seq.empty
      }
    }.recover {
      case e: Exception =>
        logger.error(s"[Greenhouse] Failed to fetch ($ghCompanyId): ${e.getMessage}")
        Seq.empty
    }.get
  }
}
