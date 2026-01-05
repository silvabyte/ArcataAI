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

IMPORTANT - Field naming conventions (must match exactly):
- Contact: name, email, phone, location, linkedIn, github, portfolio
- Summary: headline (job title/tagline), summary (description text)
- Experience entries: id, company, title, location, startDate, endDate, current, highlights
- Education entries: id, institution, degree, field, location, startDate, endDate, current, gpa, honors, coursework
- Skills: categories array, each with id, name, and skills array

Guidelines:
- Extract contact information: name (full name), email, phone, location, linkedIn URL, github URL, portfolio URL
- Extract the professional summary with headline (job title if present) and summary text
- Parse work experience entries with company, title, dates, location, and bullet points (highlights array)
- Extract education with institution, degree, field (major), dates, GPA, and honors (as single string)
- Organize skills into categories (e.g., "Programming Languages", "Frameworks", "Tools")
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
- Output dates as they appear in the resume (normalization happens later)
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
