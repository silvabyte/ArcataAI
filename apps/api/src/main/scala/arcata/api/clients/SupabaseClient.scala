package arcata.api.clients

import scala.util.{Failure, Success, Try}

import arcata.api.config.SupabaseConfig
import arcata.api.domain.*
import requests.*
import scribe.Logging
import upickle.default.*

/**
 * Client for interacting with Supabase REST API.
 *
 * Uses the PostgREST API exposed by Supabase to perform CRUD operations.
 */
class SupabaseClient(config: SupabaseConfig) extends Logging:
  private val baseUrl = s"${config.url}/rest/v1"

  private def headers: Map[String, String] = Map(
    "apikey" -> config.anonKey,
    "Authorization" -> s"Bearer ${config.serviceRoleKey}",
    "Content-Type" -> "application/json",
    "Prefer" -> "return=representation"
  )

  // Company operations

  /** Find a company by domain. */
  def findCompanyByDomain(domain: String): Option[Company] = {
    val response = requests.get(
      s"$baseUrl/companies",
      headers = headers,
      params = Map("company_domain" -> s"eq.$domain", "limit" -> "1")
    )
    parseResponse[Seq[Company]](response).flatMap(_.headOption)
  }

  /** Insert a new company and return the created record. */
  def insertCompany(company: Company): Option[Company] = {
    val json = writeCompanyForInsert(company)
    val response = requests.post(
      s"$baseUrl/companies",
      headers = headers,
      data = json
    )
    parseResponse[Seq[Company]](response).flatMap(_.headOption)
  }

  // Job operations

  /** Insert a new job and return the created record. */
  def insertJob(job: Job): Option[Job] = {
    val json = writeJobForInsert(job)
    val response = requests.post(
      s"$baseUrl/jobs",
      headers = headers,
      data = json
    )
    parseResponse[Seq[Job]](response).flatMap(_.headOption)
  }

  /** Find a job by source URL. */
  def findJobBySourceUrl(sourceUrl: String): Option[Job] = {
    val response = requests.get(
      s"$baseUrl/jobs",
      headers = headers,
      params = Map("source_url" -> s"eq.$sourceUrl", "limit" -> "1")
    )
    parseResponse[Seq[Job]](response).flatMap(_.headOption)
  }

  // Job Stream operations

  /** Insert a job stream entry. */
  def insertJobStreamEntry(entry: JobStreamEntry): Option[JobStreamEntry] = {
    val json = writeJobStreamEntryForInsert(entry)
    val response = requests.post(
      s"$baseUrl/job_stream",
      headers = headers,
      data = json
    )
    parseResponse[Seq[JobStreamEntry]](response).flatMap(_.headOption)
  }

  // Job Application operations

  /** Insert a job application. */
  def insertJobApplication(application: JobApplication): Option[JobApplication] = {
    val json = writeJobApplicationForInsert(application)
    val response = requests.post(
      s"$baseUrl/job_applications",
      headers = headers,
      data = json
    )
    parseResponse[Seq[JobApplication]](response).flatMap(_.headOption)
  }

  /** Get the default status ID for a user. */
  def getDefaultStatusId(profileId: String): Option[Long] = {
    val response = requests.get(
      s"$baseUrl/application_statuses",
      headers = headers,
      params = Map(
        "profile_id" -> s"eq.$profileId",
        "is_default" -> "eq.true",
        "select" -> "status_id",
        "limit" -> "1"
      )
    )
    parseResponse[Seq[ujson.Value]](response)
      .flatMap(_.headOption)
      .flatMap(v => Try(v("status_id").num.toLong).toOption)
  }

  // Helper methods

  private def parseResponse[T: Reader](response: Response): Option[T] = {
    if response.is2xx then
      Try(read[T](response.text())) match
        case Success(value) => Some(value)
        case Failure(e) =>
          logger.error(s"Failed to parse response: ${e.getMessage}")
          None
    else {
      logger.error(s"Supabase API error: ${response.statusCode} - ${response.text()}")
      None
    }
  }

  // JSON serialization helpers (convert camelCase to snake_case)

  private def writeCompanyForInsert(company: Company): String = {
    val obj = ujson.Obj()
    company.companyName.foreach(v => obj("company_name") = v)
    company.companyDomain.foreach(v => obj("company_domain") = v)
    company.companyJobsUrl.foreach(v => obj("company_jobs_url") = v)
    company.companyLinkedinUrl.foreach(v => obj("company_linkedin_url") = v)
    company.companyCity.foreach(v => obj("company_city") = v)
    company.companyState.foreach(v => obj("company_state") = v)
    company.primaryIndustry.foreach(v => obj("primary_industry") = v)
    company.employeeCountMin.foreach(v => obj("employee_count_min") = v)
    company.employeeCountMax.foreach(v => obj("employee_count_max") = v)
    ujson.write(obj)
  }

  private def writeJobForInsert(job: Job): String = {
    val obj = ujson.Obj("company_id" -> job.companyId, "title" -> job.title)
    job.description.foreach(v => obj("description") = v)
    job.location.foreach(v => obj("location") = v)
    job.jobType.foreach(v => obj("job_type") = v)
    job.experienceLevel.foreach(v => obj("experience_level") = v)
    job.educationLevel.foreach(v => obj("education_level") = v)
    job.salaryMin.foreach(v => obj("salary_min") = v)
    job.salaryMax.foreach(v => obj("salary_max") = v)
    job.salaryCurrency.foreach(v => obj("salary_currency") = v)
    job.qualifications.foreach(v => obj("qualifications") = ujson.Arr.from(v))
    job.preferredQualifications.foreach(v => obj("preferred_qualifications") = ujson.Arr.from(v))
    job.responsibilities.foreach(v => obj("responsibilities") = ujson.Arr.from(v))
    job.benefits.foreach(v => obj("benefits") = ujson.Arr.from(v))
    job.category.foreach(v => obj("category") = v)
    job.sourceUrl.foreach(v => obj("source_url") = v)
    job.applicationUrl.foreach(v => obj("application_url") = v)
    job.isRemote.foreach(v => obj("is_remote") = v)
    // Filter out empty strings for timestamp fields - PostgreSQL expects valid timestamps or null
    job.postedDate.filter(_.nonEmpty).foreach(v => obj("posted_date") = v)
    job.closingDate.filter(_.nonEmpty).foreach(v => obj("closing_date") = v)
    job.completionState.foreach(v => obj("completion_state") = v)
    ujson.write(obj)
  }

  private def writeJobStreamEntryForInsert(entry: JobStreamEntry): String = {
    val obj = ujson.Obj(
      "job_id" -> entry.jobId,
      "profile_id" -> entry.profileId,
      "source" -> entry.source
    )
    entry.status.foreach(v => obj("status") = v)
    entry.bestMatchScore.foreach(v => obj("best_match_score") = v)
    entry.bestMatchJobProfileId.foreach(v => obj("best_match_job_profile_id") = v)
    entry.profileMatches.foreach(v => obj("profile_matches") = v)
    ujson.write(obj)
  }

  private def writeJobApplicationForInsert(app: JobApplication): String = {
    val obj = ujson.Obj(
      "profile_id" -> app.profileId,
      "status_order" -> app.statusOrder
    )
    app.jobId.foreach(v => obj("job_id") = v)
    app.jobProfileId.foreach(v => obj("job_profile_id") = v)
    app.statusId.foreach(v => obj("status_id") = v)
    app.applicationDate.foreach(v => obj("application_date") = v)
    app.notes.foreach(v => obj("notes") = v)
    ujson.write(obj)
  }

  // Extraction Config operations

  /** Find an extraction config by match hash. */
  def findExtractionConfigByHash(matchHash: String): Option[ExtractionConfig] = {
    val response = requests.get(
      s"$baseUrl/extraction_configs",
      headers = headers,
      params = Map("match_hash" -> s"eq.$matchHash", "limit" -> "1")
    )
    parseResponse[Seq[ExtractionConfig]](response).flatMap(_.headOption)
  }

  /** Get all extraction configs for matching against a page. */
  def getAllExtractionConfigs(): Seq[ExtractionConfig] = {
    val response = requests.get(
      s"$baseUrl/extraction_configs",
      headers = headers,
      params = Map("order" -> "created_at.desc", "limit" -> "100")
    )
    parseResponse[Seq[ExtractionConfig]](response).getOrElse(Seq.empty)
  }

  /** Insert a new extraction config and return the created record. */
  def insertExtractionConfig(config: ExtractionConfig): Option[ExtractionConfig] = {
    val json = writeExtractionConfigForInsert(config)
    val response = requests.post(
      s"$baseUrl/extraction_configs",
      headers = headers,
      data = json
    )
    parseResponse[Seq[ExtractionConfig]](response).flatMap(_.headOption)
  }

  /** Upsert an extraction config (insert or update based on match_hash + version). */
  def upsertExtractionConfig(config: ExtractionConfig): Option[ExtractionConfig] = {
    val json = writeExtractionConfigForInsert(config)
    val upsertHeaders = headers + ("Prefer" -> "resolution=merge-duplicates,return=representation")
    val response = requests.post(
      s"$baseUrl/extraction_configs",
      headers = upsertHeaders,
      data = json
    )
    parseResponse[Seq[ExtractionConfig]](response).flatMap(_.headOption)
  }

  private def writeExtractionConfigForInsert(config: ExtractionConfig): String = {
    // Use writeJs to get ujson.Value objects, not JSON strings
    // This ensures JSONB columns receive proper JSON objects
    val obj = ujson.Obj(
      "name" -> config.name,
      "version" -> config.version,
      "match_patterns" -> writeJs(config.matchPatterns),
      "match_hash" -> config.matchHash,
      "extract_rules" -> writeJs(config.extractRules)
    )
    config.id.foreach(v => obj("id") = v)
    ujson.write(obj)
  }

object SupabaseClient:
  def apply(config: SupabaseConfig): SupabaseClient =
    new SupabaseClient(config)
