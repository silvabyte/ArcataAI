package arcata.api.ai

import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedCompanyData
import arcata.api.logging.Log
import boogieloops.ai.{Agent, RequestMetadata, SchemaError}
import boogieloops.ai.providers.{OpenAICompatibleProvider, ProviderTimeouts}
import upickle.default.*

/**
 * AI agent that enriches company data from job posting context.
 *
 * Uses the Vercel AI Gateway to extract additional company information that can be inferred from
 * the job posting and URL.
 */
final class CompanyEnrichmentAgent(config: AIConfig):

  private val provider = OpenAICompatibleProvider(
    baseUrl = config.baseUrl,
    apiKey = config.apiKey,
    modelId = config.model,
    strictModelValidation = false,
    timeouts = ProviderTimeouts(60_000 * 5, 60_000 * 5)
  )

  private val agent = Agent(
    name = "CompanyEnricher",
    instructions = """You extract and enrich company information from job posting context.
Given the company name, job posting content, and source URL, extract company details.

CRITICAL EXTRACTIONS:
1. websiteUrl - The company's PRIMARY website URL (NOT the job board URL)
   - Look for JSON-LD "hiringOrganization.sameAs" or "hiringOrganization.url"
   - Look for "About Us", "Company", or footer links to the main company site
   - Example: "https://www.hopper.com/" NOT "https://jobs.ashbyhq.com/hopper"

2. domain - The company's primary domain, derived from websiteUrl
   - Extract just the domain without "www." prefix
   - Example: If websiteUrl is "https://www.hopper.com/", domain is "hopper.com"
   - NEVER use ATS domains (ashbyhq.com, greenhouse.io, lever.co, workday.com, etc.)

3. jobsUrl - The BASE URL for the company's job listings (the source URL without job-specific path)
   - Example: Source "https://jobs.ashbyhq.com/hopper/abc-123" → jobsUrl "https://jobs.ashbyhq.com/hopper"
   - Example: Source "https://boards.greenhouse.io/stripe/jobs/456" → jobsUrl "https://boards.greenhouse.io/stripe"

OTHER EXTRACTIONS:
- industry: Infer from job posting content and company name
- size: Company size category (startup, small, medium, large, enterprise)
- description: Brief company description or "about us" content
- headquarters: Headquarters location if mentioned

RULES:
- Only include information that can be reasonably inferred - don't guess
- websiteUrl and domain are the MOST IMPORTANT fields - without them we cannot identify the company
- If you cannot find the company's actual website, leave websiteUrl and domain as null""",
    provider = provider,
    model = config.model,
    temperature = Some(0.1)
  )

  /**
   * Enrich company data from job posting context.
   *
   * @param companyName
   *   The name of the company
   * @param content
   *   The cleaned content (Markdown) of the job posting
   * @param url
   *   The source URL of the job posting
   * @return
   *   Either an error or the enriched company data
   */
  def enrich(
      companyName: String,
      content: String,
      url: String
  ): Either[SchemaError, ExtractedCompanyData] = {
    Log.info(
      s"CompanyEnrichmentAgent: Starting enrichment for '$companyName'",
      Map("url" -> url, "contentLength" -> content.length)
    )

    val result = agent
      .generateObjectWithoutHistory[ExtractedCompanyData](
        s"""Extract and enrich company information for '$companyName' from this job posting.

Source URL: $url

Content:
$content""",
        RequestMetadata()
      )

    result match
      case Right(response) =>
        val dataJson = write(response.data)
        Log.info(
          s"CompanyEnrichmentAgent: Raw AI response for '$companyName'",
          Map(
            "rawData" -> dataJson,
            "name" -> response.data.name,
            "domain" -> response.data.domain.getOrElse("null"),
            "websiteUrl" -> response.data.websiteUrl.getOrElse("null"),
            "jobsUrl" -> response.data.jobsUrl.getOrElse("null"),
            "industry" -> response.data.industry.getOrElse("null"),
            "size" -> response.data.size.getOrElse("null"),
            "description" -> response.data.description.map(_.take(100)).getOrElse("null"),
            "headquarters" -> response.data.headquarters.getOrElse("null")
          )
        )
      case Left(error) =>
        Log.error(
          s"CompanyEnrichmentAgent: Failed to enrich '$companyName'",
          Map("error" -> error.toString, "url" -> url)
        )

    result.map(_.data)
  }

object CompanyEnrichmentAgent:
  def apply(config: AIConfig): CompanyEnrichmentAgent =
    new CompanyEnrichmentAgent(config)
