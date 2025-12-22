package arcata.api.domain

import upickle.default.*

/**
 * A job posting.
 *
 * @param jobId
 *   Unique identifier (auto-generated)
 * @param companyId
 *   Foreign key to companies table
 * @param title
 *   Job title
 * @param description
 *   Full job description
 * @param location
 *   Job location (e.g., "Remote", "San Francisco, CA")
 * @param jobType
 *   Employment type (e.g., "Full-time", "Contract")
 * @param experienceLevel
 *   Required experience level (e.g., "Senior", "Entry-level")
 * @param educationLevel
 *   Required education (e.g., "Bachelor's", "Master's")
 * @param salaryRange
 *   Salary range as text (e.g., "$120k-$180k")
 * @param qualifications
 *   Required qualifications
 * @param preferredQualifications
 *   Nice-to-have qualifications
 * @param responsibilities
 *   Job responsibilities
 * @param benefits
 *   Benefits offered
 * @param category
 *   Job category (e.g., "Engineering", "Marketing")
 * @param sourceUrl
 *   Original URL where job was found
 * @param applicationUrl
 *   Direct URL to apply
 * @param applicationEmail
 *   Email to send applications to
 * @param rawHtmlObjectId
 *   Object storage ID for raw HTML
 * @param status
 *   Job status (e.g., "active", "closed")
 * @param postedDate
 *   When the job was posted
 * @param closingDate
 *   Application deadline
 */
final case class Job(
    jobId: Option[Long] = None,
    companyId: Long,
    title: String,
    description: Option[String] = None,
    location: Option[String] = None,
    jobType: Option[String] = None,
    experienceLevel: Option[String] = None,
    educationLevel: Option[String] = None,
    salaryRange: Option[String] = None,
    qualifications: Option[Seq[String]] = None,
    preferredQualifications: Option[Seq[String]] = None,
    responsibilities: Option[Seq[String]] = None,
    benefits: Option[Seq[String]] = None,
    category: Option[String] = None,
    sourceUrl: Option[String] = None,
    applicationUrl: Option[String] = None,
    applicationEmail: Option[String] = None,
    rawHtmlObjectId: Option[String] = None,
    status: Option[String] = Some("active"),
    postedDate: Option[String] = None,
    closingDate: Option[String] = None
) derives ReadWriter
