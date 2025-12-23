package arcata.api.ai

import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedCompanyData
import boogieloops.ai.{Agent, RequestMetadata}
import boogieloops.ai.providers.OpenAICompatibleProvider
import boogieloops.schema.SchemaError

/**
 * AI agent that enriches company data from job posting context.
 *
 * Uses the Vercel AI Gateway to extract additional company information
 * that can be inferred from the job posting and URL.
 */
final class CompanyEnrichmentAgent(config: AIConfig):

  private val provider = OpenAICompatibleProvider(
    baseUrl = config.baseUrl,
    apiKey = config.apiKey,
    modelId = config.model,
    strictModelValidation = false
  )

  private val agent = Agent(
    name = "CompanyEnricher",
    instructions = """You extract and enrich company information from job posting context.
Given the company name, job posting HTML, and URL, extract additional company details.

Guidelines:
- Infer industry from the job posting content and company name
- Estimate company size based on available clues (job volume, office locations, etc.)
- Extract any company description or "about us" content
- Identify headquarters location if mentioned
- Extract the company domain from the URL if possible
- Only include information that can be reasonably inferred - don't guess
- For size, use: startup, small, medium, large, or enterprise""",
    provider = provider,
    model = config.model,
    temperature = Some(0.1)
  )

  /**
   * Enrich company data from job posting context.
   *
   * @param companyName
   *   The name of the company
   * @param html
   *   The raw HTML of the job posting page
   * @param url
   *   The source URL of the job posting
   * @return
   *   Either an error or the enriched company data
   */
  def enrich(
      companyName: String,
      html: String,
      url: String
  ): Either[SchemaError, ExtractedCompanyData] =
    agent
      .generateObjectWithoutHistory[ExtractedCompanyData](
        s"""Extract and enrich company information for '$companyName' from this job posting.

Source URL: $url

HTML Content:
$html""",
        RequestMetadata()
      )
      .map(_.data)

object CompanyEnrichmentAgent:
  def apply(config: AIConfig): CompanyEnrichmentAgent =
    new CompanyEnrichmentAgent(config)
