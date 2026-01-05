package arcata.api.domain

import boogieloops.schema.derivation.Schematic
import upickle.default.*

/**
 * Company data extracted/enriched by AI.
 *
 * This is the output of the CompanyEnrichmentAgent.
 *
 * @param name
 *   Company name
 * @param domain
 *   Company domain derived from websiteUrl (e.g., "hopper.com")
 * @param industry
 *   Primary industry classification
 * @param size
 *   Company size category (startup, small, medium, large, enterprise)
 * @param description
 *   Brief company description
 * @param headquarters
 *   Headquarters location (e.g., "San Francisco, CA")
 * @param websiteUrl
 *   Company's primary website URL (e.g., "https://www.hopper.com/")
 * @param jobsUrl
 *   Base URL for company's job listings (e.g., "https://jobs.ashbyhq.com/hopper")
 */
final case class ExtractedCompanyData(
  name: String,
  domain: Option[String] = None,
  industry: Option[String] = None,
  size: Option[String] = None,
  description: Option[String] = None,
  headquarters: Option[String] = None,
  websiteUrl: Option[String] = None,
  jobsUrl: Option[String] = None,
) derives ReadWriter, Schematic

object ExtractedCompanyData:
  /** Create from just a name. */
  def fromName(name: String): ExtractedCompanyData =
    ExtractedCompanyData(name = name)
