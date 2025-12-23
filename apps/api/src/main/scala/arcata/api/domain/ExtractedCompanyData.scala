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
 *   Company domain (e.g., "example.com")
 * @param industry
 *   Primary industry classification
 * @param size
 *   Company size category (startup, small, medium, large, enterprise)
 * @param description
 *   Brief company description
 * @param headquarters
 *   Headquarters location (e.g., "San Francisco, CA")
 * @param websiteUrl
 *   Company website URL
 */
final case class ExtractedCompanyData(
    name: String,
    domain: Option[String] = None,
    industry: Option[String] = None,
    size: Option[String] = None,
    description: Option[String] = None,
    headquarters: Option[String] = None,
    websiteUrl: Option[String] = None
) derives ReadWriter, Schematic

object ExtractedCompanyData:
  /** Create from just a name. */
  def fromName(name: String): ExtractedCompanyData =
    ExtractedCompanyData(name = name)
