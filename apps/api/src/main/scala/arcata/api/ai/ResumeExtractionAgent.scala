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
    timeouts = ProviderTimeouts(60_000 * 5, 60_000 * 5),
  )

  private val agent = Agent(
    name = "ResumeExtractor",
    instructions =
      """You are a resume parser. Given plain text extracted from a resume document,
extract structured resume information. Be thorough and preserve ALL information.

IMPORTANT - Field naming conventions (must match exactly):
- Contact: name, email, phone, location, linkedIn, github, portfolio
- Summary: headline (job title/tagline), summary (description text)
- Experience entries: id, company, title, location, startDate, endDate, current, highlights
- Education entries: id, institution, degree, field, location, startDate, endDate, current, gpa, honors, coursework
- Skills: categories array, each with id, name, and skills array
- Projects entries: id, name, description, url, startDate, endDate, current, technologies, highlights
- Certifications entries: id, name, issuer, issueDate, expirationDate, credentialId, credentialUrl, noExpiration
- Languages: entries array, each with id, language, and proficiency (native/fluent/advanced/intermediate/beginner)
- Volunteer entries: id, organization, role, location, startDate, endDate, current, highlights
- Awards entries: id, title, issuer, date, description

Guidelines:
- Extract contact information: name (full name), email, phone, location, linkedIn URL, github URL, portfolio URL
- Extract the professional summary with headline (job title if present) and summary text
- Parse work experience entries with company, title, dates, location, and bullet points (highlights array)
- Extract education with institution, degree, field (major), dates, GPA, and honors (as single string)
- Organize skills into categories (e.g., "Programming Languages", "Frameworks", "Tools")
- Extract projects with name, description, technologies array, URL, dates, and highlights array for key features/accomplishments
- Parse certifications with name, issuer, issue date, expiration date, credential ID, and verification URL
- Extract languages with proficiency levels (native, fluent, advanced, intermediate, beginner)
- Extract volunteer experience with organization, role, location, dates, and highlights
- Extract awards/honors with title, issuing organization, date, and description

CRITICAL - Projects Section:
- Look for sections titled "Projects", "Personal Projects", "Side Projects", "Open Source", etc.
- Each project should have a name, description, and technologies used
- Include highlights array for key accomplishments or features of each project

CRITICAL - Custom Sections:
- If the resume contains sections that don't fit standard categories (e.g., "Publications", 
  "Speaking Engagements", "Patents", "Interests"), extract them as customSections.
- Awards should go in the awards field, NOT customSections
- Volunteer work should go in the volunteer field, NOT customSections
- NEVER discard or ignore any content. Every piece of information must be captured.

Date formats:
- Output dates as they appear in the resume (normalization happens later)
- Use "Present" for current positions

If information is ambiguous or unclear, make a reasonable interpretation rather than omitting it.""",
    provider = provider,
    model = config.model,
    temperature = Some(0.1), // Low temperature for consistent extraction
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
        RequestMetadata(),
      )
      .map(_.data)
  }

object ResumeExtractionAgent:
  def apply(config: AIConfig): ResumeExtractionAgent =
    new ResumeExtractionAgent(config)
