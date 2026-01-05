package arcata.api.etl.steps

import java.util.UUID

import arcata.api.domain.*
import arcata.api.etl.framework.*

/**
 * Input for the ResumeDataNormalizer step.
 *
 * @param extractedData
 *   Raw extracted resume data from AI
 * @param fileName
 *   Original file name (passed through)
 * @param objectId
 *   ObjectStorage ID (passed through)
 */
final case class ResumeDataNormalizerInput(
  extractedData: ExtractedResumeData,
  fileName: String,
  objectId: String,
)

/**
 * Output from the ResumeDataNormalizer step.
 *
 * @param normalizedData
 *   Sanitized and normalized resume data
 * @param fileName
 *   Original file name (passed through)
 * @param objectId
 *   ObjectStorage ID (passed through)
 */
final case class ResumeDataNormalizerOutput(
  normalizedData: ExtractedResumeData,
  fileName: String,
  objectId: String,
)

/**
 * ETL step that normalizes and sanitizes extracted resume data.
 *
 * Performs:
 * - Trimming whitespace from all string fields
 * - Removing empty strings and empty lists
 * - Normalizing URLs (ensuring they have protocol)
 * - Deduplicating skills
 * - Generating UUIDs for entries that need IDs
 * - Converting date formats to YYYY-MM
 * - Setting `current` flag based on endDate
 */
object ResumeDataNormalizer extends BaseStep[ResumeDataNormalizerInput, ResumeDataNormalizerOutput]:

  val name = "ResumeDataNormalizer"

  // Month name to number mapping for date conversion
  private val MonthMap = Map(
    "january" -> "01",
    "february" -> "02",
    "march" -> "03",
    "april" -> "04",
    "may" -> "05",
    "june" -> "06",
    "july" -> "07",
    "august" -> "08",
    "september" -> "09",
    "october" -> "10",
    "november" -> "11",
    "december" -> "12",
    "jan" -> "01",
    "feb" -> "02",
    "mar" -> "03",
    "apr" -> "04",
    "jun" -> "06",
    "jul" -> "07",
    "aug" -> "08",
    "sep" -> "09",
    "oct" -> "10",
    "nov" -> "11",
    "dec" -> "12",
  )

  override def execute(
    input: ResumeDataNormalizerInput,
    ctx: PipelineContext,
  ): Either[StepError, ResumeDataNormalizerOutput] = {
    logger.info(s"[${ctx.runId}] Normalizing resume data from ${input.fileName}")

    val normalized = normalizeResumeData(input.extractedData)

    logger.info(s"[${ctx.runId}] Normalization complete")

    Right(
      ResumeDataNormalizerOutput(
        normalizedData = normalized,
        fileName = input.fileName,
        objectId = input.objectId,
      )
    )
  }

  private def normalizeResumeData(data: ExtractedResumeData): ExtractedResumeData = {
    data.copy(
      contact = data.contact.map(normalizeContact),
      summary = data.summary.map(normalizeSummary),
      experience = data.experience.map(normalizeExperienceList),
      education = data.education.map(normalizeEducationList),
      skills = data.skills.map(normalizeSkillsData),
      projects = data.projects.map(normalizeProjectList),
      certifications = data.certifications.map(normalizeCertificationList),
      languages = data.languages.map(normalizeLanguagesData),
      volunteer = data.volunteer.map(normalizeVolunteerList),
      awards = data.awards.map(normalizeAwardList),
      customSections = data.customSections.map(normalizeCustomSectionList),
    )
  }

  private def normalizeContact(contact: ContactInfo): ContactInfo = {
    contact.copy(
      name = contact.name.map(_.trim).filter(_.nonEmpty),
      email = contact.email.map(_.trim.toLowerCase).filter(_.nonEmpty),
      phone = contact.phone.map(_.trim).filter(_.nonEmpty),
      location = contact.location.map(_.trim).filter(_.nonEmpty),
      linkedIn = contact.linkedIn.map(normalizeUrl).filter(_.nonEmpty),
      github = contact.github.map(normalizeUrl).filter(_.nonEmpty),
      portfolio = contact.portfolio.map(normalizeUrl).filter(_.nonEmpty),
    )
  }

  private def normalizeSummary(summary: SummaryInfo): SummaryInfo = {
    summary.copy(
      headline = summary.headline.map(_.trim).filter(_.nonEmpty),
      summary = summary.summary.map(_.trim).filter(_.nonEmpty),
    )
  }

  private def normalizeExperienceList(experiences: List[WorkExperience]): List[WorkExperience] = {
    experiences
      .map(normalizeExperience)
      .filter(exp => exp.company.exists(_.nonEmpty) || exp.title.exists(_.nonEmpty))
  }

  private def normalizeExperience(exp: WorkExperience): WorkExperience = {
    val normalizedEndDate = exp.endDate.map(_.trim).filter(_.nonEmpty)
    val isCurrent = normalizedEndDate.exists(d => d.equalsIgnoreCase("present") || d.equalsIgnoreCase("current"))

    exp.copy(
      id = exp.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      company = exp.company.map(_.trim).filter(_.nonEmpty),
      title = exp.title.map(_.trim).filter(_.nonEmpty),
      location = exp.location.map(_.trim).filter(_.nonEmpty),
      startDate = exp.startDate.flatMap(normalizeDate),
      endDate = if isCurrent then Some("") else normalizedEndDate.flatMap(normalizeDate),
      current = Some(isCurrent),
      highlights = exp.highlights.map(normalizeStringList),
    )
  }

  private def normalizeEducationList(educations: List[Education]): List[Education] = {
    educations
      .map(normalizeEducation)
      .filter(edu => edu.institution.exists(_.nonEmpty) || edu.degree.exists(_.nonEmpty))
  }

  private def normalizeEducation(edu: Education): Education = {
    val normalizedEndDate = edu.endDate.map(_.trim).filter(_.nonEmpty)
    val isCurrent = normalizedEndDate.exists(d => d.equalsIgnoreCase("present") || d.equalsIgnoreCase("current"))

    edu.copy(
      id = edu.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      institution = edu.institution.map(_.trim).filter(_.nonEmpty),
      degree = edu.degree.map(_.trim).filter(_.nonEmpty),
      field = edu.field.map(_.trim).filter(_.nonEmpty),
      location = edu.location.map(_.trim).filter(_.nonEmpty),
      startDate = edu.startDate.flatMap(normalizeDate),
      endDate = if isCurrent then Some("") else normalizedEndDate.flatMap(normalizeDate),
      current = Some(isCurrent),
      gpa = edu.gpa.map(_.trim).filter(_.nonEmpty),
      honors = edu.honors.map(_.trim).filter(_.nonEmpty),
      coursework = edu.coursework.map(normalizeStringList),
    )
  }

  private def normalizeSkillsData(skills: SkillsData): SkillsData = {
    skills.copy(
      categories = skills.categories.map(
        cats =>
          cats
            .map(normalizeSkillCategory)
            .filter(cat => cat.name.exists(_.nonEmpty) || cat.skills.exists(_.nonEmpty))
      )
    )
  }

  private def normalizeSkillCategory(cat: SkillCategory): SkillCategory = {
    cat.copy(
      id = cat.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      name = cat.name.map(_.trim).filter(_.nonEmpty),
      skills = cat.skills.map(skills => skills.map(_.trim).filter(_.nonEmpty).distinct),
    )
  }

  private def normalizeProjectList(projects: List[Project]): List[Project] = {
    projects
      .map(normalizeProject)
      .filter(proj => proj.name.exists(_.nonEmpty))
  }

  private def normalizeProject(proj: Project): Project = {
    val normalizedEndDate = proj.endDate.map(_.trim).filter(_.nonEmpty)
    val isCurrent = normalizedEndDate.exists(d => d.equalsIgnoreCase("present") || d.equalsIgnoreCase("current"))

    proj.copy(
      id = proj.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      name = proj.name.map(_.trim).filter(_.nonEmpty),
      description = proj.description.map(_.trim).filter(_.nonEmpty),
      url = proj.url.map(normalizeUrl).filter(_.nonEmpty),
      startDate = proj.startDate.flatMap(normalizeDate),
      endDate = if isCurrent then Some("") else normalizedEndDate.flatMap(normalizeDate),
      current = Some(isCurrent),
      technologies = proj.technologies.map(normalizeStringList),
      highlights = proj.highlights.map(normalizeStringList),
    )
  }

  private def normalizeCertificationList(certs: List[Certification]): List[Certification] = {
    certs
      .map(normalizeCertification)
      .filter(cert => cert.name.exists(_.nonEmpty))
  }

  private def normalizeCertification(cert: Certification): Certification = {
    val normalizedExpDate = cert.expirationDate.map(_.trim).filter(_.nonEmpty)
    val hasNoExpiration = normalizedExpDate.exists(
      d =>
        d.equalsIgnoreCase("none") || d.equalsIgnoreCase("no expiration") || d.equalsIgnoreCase("n/a")
    ) || cert.noExpiration.getOrElse(false)

    cert.copy(
      id = cert.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      name = cert.name.map(_.trim).filter(_.nonEmpty),
      issuer = cert.issuer.map(_.trim).filter(_.nonEmpty),
      issueDate = cert.issueDate.flatMap(normalizeDate),
      expirationDate = if hasNoExpiration then Some("") else normalizedExpDate.flatMap(normalizeDate),
      credentialId = cert.credentialId.map(_.trim).filter(_.nonEmpty),
      credentialUrl = cert.credentialUrl.map(normalizeUrl).filter(_.nonEmpty),
      noExpiration = Some(hasNoExpiration),
    )
  }

  private def normalizeLanguagesData(languages: LanguagesData): LanguagesData = {
    languages.copy(
      entries = languages.entries.map(
        entries =>
          entries
            .map(normalizeLanguageEntry)
            .filter(entry => entry.language.exists(_.nonEmpty))
      )
    )
  }

  private def normalizeLanguageEntry(entry: LanguageEntry): LanguageEntry = {
    entry.copy(
      id = entry.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      language = entry.language.map(_.trim).filter(_.nonEmpty),
      proficiency = entry.proficiency.map(normalizeProficiency),
    )
  }

  /**
   * Normalize proficiency level to match frontend enum values.
   */
  private def normalizeProficiency(proficiency: String): String = {
    proficiency.trim.toLowerCase match
      case "native" | "native speaker" | "mother tongue" => "native"
      case "fluent" | "professional" | "full professional" => "fluent"
      case "advanced" | "professional working" => "advanced"
      case "intermediate" | "limited working" => "intermediate"
      case "beginner" | "elementary" | "basic" => "beginner"
      case other => other // Pass through unknown values
  }

  private def normalizeVolunteerList(volunteer: List[VolunteerExperience]): List[VolunteerExperience] = {
    volunteer
      .map(normalizeVolunteer)
      .filter(v => v.organization.exists(_.nonEmpty) || v.role.exists(_.nonEmpty))
  }

  private def normalizeVolunteer(vol: VolunteerExperience): VolunteerExperience = {
    val normalizedEndDate = vol.endDate.map(_.trim).filter(_.nonEmpty)
    val isCurrent = normalizedEndDate.exists(d => d.equalsIgnoreCase("present") || d.equalsIgnoreCase("current"))

    vol.copy(
      id = vol.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      organization = vol.organization.map(_.trim).filter(_.nonEmpty),
      role = vol.role.map(_.trim).filter(_.nonEmpty),
      location = vol.location.map(_.trim).filter(_.nonEmpty),
      startDate = vol.startDate.flatMap(normalizeDate),
      endDate = if isCurrent then Some("") else normalizedEndDate.flatMap(normalizeDate),
      current = Some(isCurrent),
      highlights = vol.highlights.map(normalizeStringList),
    )
  }

  private def normalizeAwardList(awards: List[Award]): List[Award] = {
    awards
      .map(normalizeAward)
      .filter(award => award.title.exists(_.nonEmpty))
  }

  private def normalizeAward(award: Award): Award = {
    award.copy(
      id = award.id.filter(_.nonEmpty).orElse(Some(UUID.randomUUID().toString)),
      title = award.title.map(_.trim).filter(_.nonEmpty),
      issuer = award.issuer.map(_.trim).filter(_.nonEmpty),
      date = award.date.flatMap(normalizeDate),
      description = award.description.map(_.trim).filter(_.nonEmpty),
    )
  }

  private def normalizeCustomSectionList(sections: List[CustomSection]): List[CustomSection] = {
    sections
      .map(normalizeCustomSection)
      .filter(sec => sec.title.nonEmpty && (sec.content.exists(_.nonEmpty) || sec.items.exists(_.nonEmpty)))
  }

  private def normalizeCustomSection(section: CustomSection): CustomSection = {
    section.copy(
      title = section.title.trim,
      content = section.content.map(_.trim).filter(_.nonEmpty),
      items = section.items.map(normalizeStringList),
    )
  }

  private def normalizeStringList(items: List[String]): List[String] = {
    items.map(_.trim).filter(_.nonEmpty)
  }

  /**
   * Normalize a URL by ensuring it has a protocol.
   */
  private def normalizeUrl(url: String): String = {
    val trimmed = url.trim
    if trimmed.isEmpty then trimmed
    else if trimmed.startsWith("http://") || trimmed.startsWith("https://") then trimmed
    else s"https://$trimmed"
  }

  /**
   * Normalize a date string to YYYY-MM format.
   *
   * Handles various input formats:
   * - "July 2025" -> "2025-07"
   * - "Jul 2025" -> "2025-07"
   * - "2025-07" -> "2025-07" (already normalized)
   * - "2025" -> "2025-01" (year only, assume January)
   * - "Present" -> None (handled separately)
   */
  private def normalizeDate(date: String): Option[String] = {
    val trimmed = date.trim
    if trimmed.isEmpty || trimmed.equalsIgnoreCase("present") || trimmed.equalsIgnoreCase("current") then
      None
    else if trimmed.matches("\\d{4}-\\d{2}") then
      // Already in YYYY-MM format
      Some(trimmed)
    else if trimmed.matches("\\d{4}") then
      // Year only - assume January
      Some(s"$trimmed-01")
    else
      // Try to parse "Month Year" format
      val parts = trimmed.split("\\s+")
      if parts.length >= 2 then
        val monthStr = parts(0).toLowerCase
        val yearStr = parts.find(_.matches("\\d{4}"))
        (MonthMap.get(monthStr), yearStr) match
          case (Some(month), Some(year)) => Some(s"$year-$month")
          case _ => None
      else
        None
  }
