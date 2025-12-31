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
 * Uses the new Supabase secret key format (sb_secret_...).
 */
class SupabaseClient(config: SupabaseConfig) extends Logging:
  private val baseUrl = s"${config.url}/rest/v1"

  // New secret keys: put the secret key in apikey header
  // The Supabase API Gateway handles minting a short-lived JWT internally
  private def headers: Map[String, String] = Map(
    "apikey" -> config.serviceRoleKey,
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

  /** Find a company by jobs URL. */
  def findCompanyByJobsUrl(jobsUrl: String): Option[Company] = {
    val response = requests.get(
      s"$baseUrl/companies",
      headers = headers,
      params = Map("company_jobs_url" -> s"eq.$jobsUrl", "limit" -> "1")
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

  /**
   * Find open jobs that need their status checked.
   *
   * @param batchSize
   *   Maximum number of jobs to return
   * @param olderThanDays
   *   Only include jobs not checked in this many days (or never checked)
   * @return
   *   Sequence of jobs to check
   */
  def findJobsToCheck(batchSize: Int, olderThanDays: Int): Seq[Job] = {
    val threshold = java.time.Instant.now().minus(java.time.Duration.ofDays(olderThanDays)).toString
    // Query: status = 'open' AND source_url IS NOT NULL AND (last_status_check IS NULL OR last_status_check < threshold)
    val response = requests.get(
      s"$baseUrl/jobs",
      headers = headers,
      params = Map(
        "status" -> "eq.open",
        "source_url" -> "not.is.null",
        "or" -> s"(last_status_check.is.null,last_status_check.lt.$threshold)",
        "limit" -> batchSize.toString,
        "order" -> "last_status_check.asc.nullsfirst"
      )
    )
    parseResponse[Seq[Job]](response).getOrElse(Seq.empty)
  }

  /**
   * Update a job's status to closed.
   *
   * @param jobId
   *   The job ID to update
   * @param closedReason
   *   Why the job was marked closed
   * @return
   *   true if update succeeded
   */
  def updateJobStatusClosed(jobId: Long, closedReason: Option[String]): Boolean = {
    val now = java.time.Instant.now().toString
    val obj = ujson.Obj(
      "status" -> "closed",
      "closed_at" -> now,
      "last_status_check" -> now
    )
    closedReason.foreach(v => obj("closed_reason") = v)

    val response = requests.patch(
      s"$baseUrl/jobs",
      headers = headers,
      params = Map("job_id" -> s"eq.$jobId"),
      data = ujson.write(obj)
    )
    response.is2xx
  }

  /**
   * Update a job's last status check timestamp (job is still open).
   *
   * @param jobId
   *   The job ID to update
   * @return
   *   true if update succeeded
   */
  def updateJobLastCheck(jobId: Long): Boolean = {
    val now = java.time.Instant.now().toString
    val obj = ujson.Obj("last_status_check" -> now)

    val response = requests.patch(
      s"$baseUrl/jobs",
      headers = headers,
      params = Map("job_id" -> s"eq.$jobId"),
      data = ujson.write(obj)
    )
    response.is2xx
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
    val obj = ujson.Obj("title" -> job.title)
    job.companyId.foreach(v => obj("company_id") = v)
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
    job.postedDate.foreach(v => obj("posted_date") = v)
    job.closingDate.foreach(v => obj("closing_date") = v)
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

object SupabaseClient:
  def apply(config: SupabaseConfig): SupabaseClient =
    new SupabaseClient(config)
