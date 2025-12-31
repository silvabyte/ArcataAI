package arcata.api.ai

import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedJobData
import boogieloops.ai.{Agent, RequestMetadata, SchemaError}
import boogieloops.ai.providers.OpenAICompatibleProvider
import boogieloops.ai.providers.ProviderTimeouts

/**
 * AI agent that extracts structured job data from HTML.
 *
 * Uses the Vercel AI Gateway to call Claude for intelligent extraction.
 */
final class JobExtractionAgent(config: AIConfig):

  private val provider = OpenAICompatibleProvider(
    baseUrl = config.baseUrl,
    apiKey = config.apiKey,
    modelId = config.model,
    strictModelValidation = false,
    timeouts = ProviderTimeouts(60_000 * 5, 60_000 * 5)
  )

  private val agent = Agent(
    name = "JobExtractor",
    instructions = """You are a job posting parser. Given HTML content from a job posting page,
extract structured job information. Be thorough but concise.

Guidelines:
- Extract the exact job title as shown
- Get the company name from the page content
- Parse location, job type, experience level from the posting
- Extract qualifications and responsibilities as lists
- Include salary information if visible
- Note application URLs or emails if present
- If information is not clearly present, leave the field empty rather than guessing""",
    provider = provider,
    model = config.model,
    temperature = Some(0.1) // Low temperature for consistent extraction
  )

  /**
   * Extract job data from content.
   *
   * @param content
   *   The cleaned content (Markdown) of the job posting, with preserved JSON-LD structured data
   * @param url
   *   The source URL of the job posting
   * @return
   *   Either an error or the extracted job data
   */
  def extract(content: String, url: String): Either[SchemaError, ExtractedJobData] = {
    agent
      .generateObjectWithoutHistory[ExtractedJobData](
        s"""Extract job details from this job posting content.

Source URL: $url

Content:
$content""",
        RequestMetadata()
      )
      .map(_.data)
  }

object JobExtractionAgent:
  def apply(config: AIConfig): JobExtractionAgent =
    new JobExtractionAgent(config)
