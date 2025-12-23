package arcata.api.domain

import upickle.default.*

/**
 * A company that posts jobs.
 *
 * @param companyId
 *   Unique identifier (auto-generated)
 * @param companyName
 *   Name of the company
 * @param companyDomain
 *   Primary domain (e.g., "example.com")
 * @param companyJobsUrl
 *   URL to company's job board
 * @param companyLinkedinUrl
 *   LinkedIn company page URL
 * @param companyCity
 *   City where company is headquartered
 * @param companyState
 *   State/province
 * @param primaryIndustry
 *   Primary industry classification
 * @param employeeCountMin
 *   Minimum employee count estimate
 * @param employeeCountMax
 *   Maximum employee count estimate
 * @param industry
 *   AI-extracted industry classification
 * @param companySize
 *   Company size category (startup/small/medium/large/enterprise)
 * @param description
 *   Brief company description from AI
 * @param headquarters
 *   Headquarters location (e.g., "San Francisco, CA")
 */
final case class Company(
    companyId: Option[Long] = None,
    companyName: Option[String] = None,
    companyDomain: Option[String] = None,
    companyJobsUrl: Option[String] = None,
    companyLinkedinUrl: Option[String] = None,
    companyCity: Option[String] = None,
    companyState: Option[String] = None,
    primaryIndustry: Option[String] = None,
    employeeCountMin: Option[Int] = None,
    employeeCountMax: Option[Int] = None,
    // New enrichment fields
    industry: Option[String] = None,
    companySize: Option[String] = None,
    description: Option[String] = None,
    headquarters: Option[String] = None
) derives ReadWriter

object Company:
  /** Create a minimal company with just the domain. */
  def fromDomain(domain: String): Company =
    Company(companyDomain = Some(domain))
