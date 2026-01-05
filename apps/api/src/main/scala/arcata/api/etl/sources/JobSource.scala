package arcata.api.etl.sources

import arcata.api.clients.SupabaseClient
import arcata.api.etl.greenhouse.GreenhouseDiscovery
import scribe.Logging

/**
 * Configuration for rate limiting a job source.
 *
 * Each job source can have different rate limits based on the ATS platform's tolerance. Start
 * conservative and increase as you verify the source handles the load.
 *
 * @param companyBatchSize
 *   Maximum companies to process per workflow run
 * @param jobsPerCompany
 *   Maximum jobs to fetch per company
 * @param delayBetweenRequestsMs
 *   Delay between HTTP requests in milliseconds
 */
case class JobSourceConfig(
  companyBatchSize: Int = 1,
  jobsPerCompany: Int = 1,
  delayBetweenRequestsMs: Int = 1000,
)

/**
 * A discovered job URL ready for ingestion.
 *
 * @param url
 *   The job posting URL (should be normalized via UrlNormalizer)
 * @param source
 *   The JobSource that discovered this job (for routing to optimized pipelines)
 * @param companyId
 *   Optional company ID if already known from our database
 * @param apiUrl
 *   Optional direct API URL for fetching structured data (e.g., Greenhouse job detail API)
 * @param metadata
 *   Additional context from the source (e.g., greenhouse_job_id, lever_posting_id)
 */
case class DiscoveredJob(
  url: String,
  source: JobSource,
  companyId: Option[Long] = None,
  apiUrl: Option[String] = None,
  metadata: Map[String, String] = Map.empty,
)

/**
 * Enumeration of supported job sources (ATS platforms).
 *
 * Each source knows how to discover jobs from its platform. The actual discovery logic is
 * implemented in source-specific modules under `etl/{source}/`.
 *
 * To add a new source:
 *   1. Add a new enum case with sourceId, name, and config
 *   2. Create a discovery module (e.g., `etl/lever/LeverDiscovery.scala`)
 *   3. Add a case to the `discoverJobs` match expression
 *
 * @param sourceId
 *   Unique identifier used in database (company_jobs_source column)
 * @param name
 *   Human-readable name for logging
 * @param config
 *   Rate limiting configuration
 */
enum JobSource(
  val sourceId: String,
  val name: String,
  val config: JobSourceConfig,
) {

  case Greenhouse
    extends JobSource(
      sourceId = "greenhouse",
      name = "Greenhouse ATS",
      config = JobSourceConfig(
        companyBatchSize = 1,
        jobsPerCompany = 1,
        delayBetweenRequestsMs = 1000,
      ),
    )

  // Future sources:
  // case Lever extends JobSource("lever", "Lever ATS", JobSourceConfig(...))
  // case Ashby extends JobSource("ashby", "Ashby HQ", JobSourceConfig(...))
  // case Workday extends JobSource("workday", "Workday", JobSourceConfig(...))

  /**
   * Discover job URLs from this source.
   *
   * Delegates to source-specific discovery modules.
   *
   * @param supabase
   *   Client for querying companies table
   * @return
   *   Sequence of discovered jobs to ingest
   */
  def discoverJobs(supabase: SupabaseClient): Seq[DiscoveredJob] = this match
    case Greenhouse => GreenhouseDiscovery.discoverJobs(supabase, config)

  override def toString: String = sourceId
}

/**
 * Companion object with parsing and listing utilities.
 */
object JobSource extends Logging {

  /** All available job sources. */
  val all: Seq[JobSource] = JobSource.values.toSeq

  /** All source IDs. */
  val sourceIds: Seq[String] = all.map(_.sourceId)

  /**
   * Parse a JobSource from its sourceId string.
   *
   * @param sourceId
   *   The source identifier (e.g., "greenhouse")
   * @return
   *   The matching JobSource if found
   */
  def fromString(sourceId: String): Option[JobSource] =
    all.find(_.sourceId == sourceId.toLowerCase)

  /**
   * Get a JobSource by ID (alias for fromString).
   */
  def get(sourceId: String): Option[JobSource] = fromString(sourceId)
}
