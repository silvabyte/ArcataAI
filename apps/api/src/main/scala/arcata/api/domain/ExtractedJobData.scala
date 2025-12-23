package arcata.api.domain

import boogieloops.schema.derivation.Schematic
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
 * @param salaryMin
 *   Minimum salary amount (extracted as integer)
 * @param salaryMax
 *   Maximum salary amount (extracted as integer)
 * @param salaryCurrency
 *   Currency code (default USD)
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
 * @param isRemote
 *   Whether job allows remote work
 * @param postedDate
 *   Posted date if found
 * @param closingDate
 *   Closing date if found
 */
final case class ExtractedJobData(
    title: String,
    companyName: Option[String] = None,
    description: Option[String] = None,
    location: Option[String] = None,
    jobType: Option[String] = None,
    experienceLevel: Option[String] = None,
    educationLevel: Option[String] = None,
    salaryMin: Option[Int] = None,
    salaryMax: Option[Int] = None,
    salaryCurrency: Option[String] = None,
    qualifications: Option[List[String]] = None,
    preferredQualifications: Option[List[String]] = None,
    responsibilities: Option[List[String]] = None,
    benefits: Option[List[String]] = None,
    category: Option[String] = None,
    applicationUrl: Option[String] = None,
    isRemote: Option[Boolean] = None,
    postedDate: Option[String] = None,
    closingDate: Option[String] = None
) derives ReadWriter, Schematic

object ExtractedJobData:
  /** Create a minimal extraction result with just the title. */
  def minimal(title: String): ExtractedJobData =
    ExtractedJobData(title = title)
