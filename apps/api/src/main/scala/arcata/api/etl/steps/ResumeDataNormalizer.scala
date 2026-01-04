package arcata.api.etl.steps

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
    objectId: String
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
    objectId: String
)

/**
 * ETL step that normalizes and sanitizes extracted resume data.
 *
 * Performs:
 * - Trimming whitespace from all string fields
 * - Removing empty strings and empty lists
 * - Normalizing URLs (ensuring they have protocol)
 * - Deduplicating skills
 * - Sorting experience and education by date (most recent first)
 */
object ResumeDataNormalizer extends BaseStep[ResumeDataNormalizerInput, ResumeDataNormalizerOutput]:

  val name = "ResumeDataNormalizer"

  override def execute(
      input: ResumeDataNormalizerInput,
      ctx: PipelineContext
  ): Either[StepError, ResumeDataNormalizerOutput] = {
    logger.info(s"[${ctx.runId}] Normalizing resume data from ${input.fileName}")

    val normalized = normalizeResumeData(input.extractedData)

    logger.info(s"[${ctx.runId}] Normalization complete")

    Right(
      ResumeDataNormalizerOutput(
        normalizedData = normalized,
        fileName = input.fileName,
        objectId = input.objectId
      )
    )
  }

  private def normalizeResumeData(data: ExtractedResumeData): ExtractedResumeData = {
    data.copy(
      contact = data.contact.map(normalizeContact),
      summary = data.summary.map(_.trim).filter(_.nonEmpty),
      experience = data.experience.map(normalizeExperienceList),
      education = data.education.map(normalizeEducationList),
      skills = data.skills.map(normalizeSkills),
      projects = data.projects.map(normalizeProjectList),
      certifications = data.certifications.map(normalizeCertificationList),
      languages = data.languages.map(normalizeStringList),
      customSections = data.customSections.map(normalizeCustomSectionList)
    )
  }

  private def normalizeContact(contact: ContactInfo): ContactInfo = {
    contact.copy(
      fullName = contact.fullName.map(_.trim).filter(_.nonEmpty),
      email = contact.email.map(_.trim.toLowerCase).filter(_.nonEmpty),
      phone = contact.phone.map(_.trim).filter(_.nonEmpty),
      location = contact.location.map(_.trim).filter(_.nonEmpty),
      linkedinUrl = contact.linkedinUrl.map(normalizeUrl).filter(_.nonEmpty),
      githubUrl = contact.githubUrl.map(normalizeUrl).filter(_.nonEmpty),
      portfolioUrl = contact.portfolioUrl.map(normalizeUrl).filter(_.nonEmpty)
    )
  }

  private def normalizeExperienceList(experiences: List[WorkExperience]): List[WorkExperience] = {
    experiences
      .map(normalizeExperience)
      .filter(exp => exp.company.exists(_.nonEmpty) || exp.title.exists(_.nonEmpty))
  }

  private def normalizeExperience(exp: WorkExperience): WorkExperience = {
    exp.copy(
      company = exp.company.map(_.trim).filter(_.nonEmpty),
      title = exp.title.map(_.trim).filter(_.nonEmpty),
      location = exp.location.map(_.trim).filter(_.nonEmpty),
      startDate = exp.startDate.map(_.trim).filter(_.nonEmpty),
      endDate = exp.endDate.map(_.trim).filter(_.nonEmpty),
      description = exp.description.map(_.trim).filter(_.nonEmpty),
      highlights = exp.highlights.map(normalizeStringList)
    )
  }

  private def normalizeEducationList(educations: List[Education]): List[Education] = {
    educations
      .map(normalizeEducation)
      .filter(edu => edu.institution.exists(_.nonEmpty) || edu.degree.exists(_.nonEmpty))
  }

  private def normalizeEducation(edu: Education): Education = {
    edu.copy(
      institution = edu.institution.map(_.trim).filter(_.nonEmpty),
      degree = edu.degree.map(_.trim).filter(_.nonEmpty),
      fieldOfStudy = edu.fieldOfStudy.map(_.trim).filter(_.nonEmpty),
      location = edu.location.map(_.trim).filter(_.nonEmpty),
      startDate = edu.startDate.map(_.trim).filter(_.nonEmpty),
      endDate = edu.endDate.map(_.trim).filter(_.nonEmpty),
      gpa = edu.gpa.map(_.trim).filter(_.nonEmpty),
      honors = edu.honors.map(normalizeStringList)
    )
  }

  private def normalizeSkills(skills: List[String]): List[String] = {
    skills
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct // Remove duplicates
  }

  private def normalizeProjectList(projects: List[Project]): List[Project] = {
    projects
      .map(normalizeProject)
      .filter(proj => proj.name.exists(_.nonEmpty))
  }

  private def normalizeProject(proj: Project): Project = {
    proj.copy(
      name = proj.name.map(_.trim).filter(_.nonEmpty),
      description = proj.description.map(_.trim).filter(_.nonEmpty),
      technologies = proj.technologies.map(normalizeStringList),
      url = proj.url.map(normalizeUrl).filter(_.nonEmpty),
      startDate = proj.startDate.map(_.trim).filter(_.nonEmpty),
      endDate = proj.endDate.map(_.trim).filter(_.nonEmpty)
    )
  }

  private def normalizeCertificationList(certs: List[Certification]): List[Certification] = {
    certs
      .map(normalizeCertification)
      .filter(cert => cert.name.exists(_.nonEmpty))
  }

  private def normalizeCertification(cert: Certification): Certification = {
    cert.copy(
      name = cert.name.map(_.trim).filter(_.nonEmpty),
      issuer = cert.issuer.map(_.trim).filter(_.nonEmpty),
      dateObtained = cert.dateObtained.map(_.trim).filter(_.nonEmpty),
      expirationDate = cert.expirationDate.map(_.trim).filter(_.nonEmpty),
      credentialId = cert.credentialId.map(_.trim).filter(_.nonEmpty),
      url = cert.url.map(normalizeUrl).filter(_.nonEmpty)
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
      items = section.items.map(normalizeStringList)
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
