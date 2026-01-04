package arcata.api.ai

import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedResumeData
import boogieloops.ai.{Agent, RequestMetadata, SchemaError}
import boogieloops.ai.providers.OpenAICompatibleProvider
import boogieloops.ai.providers.ProviderTimeouts

/**
 * AI agent that extracts structured resume data from text.
 *
 * Uses the Vercel AI Gateway to call Claude for intelligent extraction. The agent is designed to
 * preserve all information from the resume, mapping known sections to structured fields and
 * capturing any unmapped content in customSections to prevent data loss.
 */
final class ResumeExtractionAgent(config: AIConfig):

  private val provider = OpenAICompatibleProvider(
    baseUrl = config.baseUrl,
    apiKey = config.apiKey,
    modelId = config.model,
    strictModelValidation = false,
    timeouts = ProviderTimeouts(60_000 * 5, 60_000 * 5)
  )

  private val agent = Agent(
    name = "ResumeExtractor",
    instructions = """You are a resume parser. Given plain text extracted from a resume document,
extract structured resume information. Be thorough and preserve ALL information.

Guidelines:
- Extract contact information: name, email, phone, location, LinkedIn, GitHub, portfolio URLs
- Extract the professional summary or objective if present
- Parse work experience entries with company, title, dates, location, and bullet points
- Extract education with institution, degree, field of study, dates, GPA, and honors
- Collect all skills mentioned (technical and soft skills)
- Extract projects with name, description, technologies, and URLs
- Parse certifications with name, issuer, dates, and credential IDs
- List languages spoken if mentioned

CRITICAL - Custom Sections:
- If the resume contains sections that don't fit standard categories (e.g., "Publications", 
  "Awards", "Volunteer Work", "Speaking Engagements", "Patents", "Interests"), 
  extract them as customSections with the section title and content.
- NEVER discard or ignore any content. Every piece of information must be captured either
  in a standard field or in customSections.

Date formats:
- Preserve dates as they appear in the resume (e.g., "Jan 2020", "2020", "January 2020 - Present")
- Use "Present" for current positions

If information is ambiguous or unclear, make a reasonable interpretation rather than omitting it.""",
    provider = provider,
    model = config.model,
    temperature = Some(0.1) // Low temperature for consistent extraction
  )

  /**
   * Extract resume data from text content.
   *
   * @param text
   *   Plain text extracted from the resume document (PDF, DOCX, or TXT)
   * @param fileName
   *   Original file name (for context)
   * @return
   *   Either an error or the extracted resume data
   */
  def extract(text: String, fileName: String): Either[SchemaError, ExtractedResumeData] = {
    agent
      .generateObjectWithoutHistory[ExtractedResumeData](
        s"""Extract all resume information from this document.

File name: $fileName

Resume content:
$text""",
        RequestMetadata()
      )
      .map(_.data)
  }

object ResumeExtractionAgent:
  def apply(config: AIConfig): ResumeExtractionAgent =
    new ResumeExtractionAgent(config)
