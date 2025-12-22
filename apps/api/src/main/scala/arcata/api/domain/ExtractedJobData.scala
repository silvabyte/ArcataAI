package arcata.api.domain

import upickle.default.*

/**
 * Job data extracted from HTML by the AI parser.
 *
 * This is the output of the BoogieLoops AI extraction step.
 *
 * @param title
 *   Job title
 * @param companyName
 *   Company name as it appears on the page
 * @param description
 *   Full job description
 * @param location
 *   Job location
 * @param jobType
 *   Employment type
 * @param experienceLevel
 *   Required experience level
 * @param educationLevel
 *   Required education
 * @param salaryRange
 *   Salary range
 * @param qualifications
 *   Required qualifications
 * @param preferredQualifications
 *   Nice-to-have qualifications
 * @param responsibilities
 *   Job responsibilities
 * @param benefits
 *   Benefits offered
 * @param category
 *   Inferred job category
 * @param applicationUrl
 *   Direct application URL if found
 * @param applicationEmail
 *   Application email if found
 * @param postedDate
 *   Posted date if found
 * @param closingDate
 *   Closing date if found
 * @param rawText
 *   Raw extracted text (for debugging)
 */
final case class ExtractedJobData(
    title: String,
    companyName: Option[String] = None,
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
    applicationUrl: Option[String] = None,
    applicationEmail: Option[String] = None,
    postedDate: Option[String] = None,
    closingDate: Option[String] = None,
    rawText: Option[String] = None
) derives ReadWriter

object ExtractedJobData:
  /** Create a minimal extraction result with just the title. */
  def minimal(title: String): ExtractedJobData =
    ExtractedJobData(title = title)
