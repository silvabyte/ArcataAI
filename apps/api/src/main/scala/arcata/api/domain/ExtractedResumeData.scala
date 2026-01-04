package arcata.api.domain

import boogieloops.schema.derivation.Schematic
import upickle.default.*

/**
 * Contact information extracted from a resume.
 *
 * @param fullName
 *   Full name of the candidate
 * @param email
 *   Email address
 * @param phone
 *   Phone number
 * @param location
 *   City, state, or full address
 * @param linkedinUrl
 *   LinkedIn profile URL
 * @param githubUrl
 *   GitHub profile URL
 * @param portfolioUrl
 *   Personal website or portfolio URL
 */
final case class ContactInfo(
    fullName: Option[String] = None,
    email: Option[String] = None,
    phone: Option[String] = None,
    location: Option[String] = None,
    linkedinUrl: Option[String] = None,
    githubUrl: Option[String] = None,
    portfolioUrl: Option[String] = None
) derives ReadWriter, Schematic

/**
 * Work experience entry extracted from a resume.
 *
 * @param company
 *   Company or organization name
 * @param title
 *   Job title or role
 * @param location
 *   Location of the job
 * @param startDate
 *   Start date (e.g., "Jan 2020" or "2020-01")
 * @param endDate
 *   End date, or "Present" if current
 * @param description
 *   Description of responsibilities and achievements
 * @param highlights
 *   Key accomplishments as bullet points
 */
final case class WorkExperience(
    company: Option[String] = None,
    title: Option[String] = None,
    location: Option[String] = None,
    startDate: Option[String] = None,
    endDate: Option[String] = None,
    description: Option[String] = None,
    highlights: Option[List[String]] = None
) derives ReadWriter, Schematic

/**
 * Education entry extracted from a resume.
 *
 * @param institution
 *   School or university name
 * @param degree
 *   Degree type (e.g., "Bachelor of Science", "MBA")
 * @param fieldOfStudy
 *   Major or field of study
 * @param location
 *   Location of the institution
 * @param startDate
 *   Start date
 * @param endDate
 *   End date or graduation date
 * @param gpa
 *   GPA if listed
 * @param honors
 *   Honors, awards, or notable achievements
 */
final case class Education(
    institution: Option[String] = None,
    degree: Option[String] = None,
    fieldOfStudy: Option[String] = None,
    location: Option[String] = None,
    startDate: Option[String] = None,
    endDate: Option[String] = None,
    gpa: Option[String] = None,
    honors: Option[List[String]] = None
) derives ReadWriter, Schematic

/**
 * Project entry extracted from a resume.
 *
 * @param name
 *   Project name
 * @param description
 *   What the project does
 * @param technologies
 *   Technologies or tools used
 * @param url
 *   Link to project (GitHub, demo, etc.)
 * @param startDate
 *   When the project was started
 * @param endDate
 *   When the project ended (if applicable)
 */
final case class Project(
    name: Option[String] = None,
    description: Option[String] = None,
    technologies: Option[List[String]] = None,
    url: Option[String] = None,
    startDate: Option[String] = None,
    endDate: Option[String] = None
) derives ReadWriter, Schematic

/**
 * Certification or credential extracted from a resume.
 *
 * @param name
 *   Certification name
 * @param issuer
 *   Issuing organization
 * @param dateObtained
 *   When the certification was obtained
 * @param expirationDate
 *   Expiration date if applicable
 * @param credentialId
 *   Credential ID if provided
 * @param url
 *   Verification URL
 */
final case class Certification(
    name: Option[String] = None,
    issuer: Option[String] = None,
    dateObtained: Option[String] = None,
    expirationDate: Option[String] = None,
    credentialId: Option[String] = None,
    url: Option[String] = None
) derives ReadWriter, Schematic

/**
 * A custom section that doesn't fit standard resume categories.
 *
 * This captures content that the AI couldn't map to standard fields, ensuring no data is lost.
 *
 * @param title
 *   Section heading (e.g., "Publications", "Volunteer Work", "Awards")
 * @param content
 *   The content of the section as extracted text
 * @param items
 *   Structured items if the section contains a list
 */
final case class CustomSection(
    title: String,
    content: Option[String] = None,
    items: Option[List[String]] = None
) derives ReadWriter, Schematic

/**
 * Resume data extracted from a document by the AI parser.
 *
 * This is the output of the BoogieLoops AI extraction step for resumes. Follows a structure
 * compatible with the Lexical resume editor in the HQ app.
 *
 * @param contact
 *   Contact information section
 * @param summary
 *   Professional summary or objective statement
 * @param experience
 *   Work experience entries, in chronological order as they appear
 * @param education
 *   Education entries
 * @param skills
 *   Skills listed (technical and soft skills)
 * @param projects
 *   Projects section
 * @param certifications
 *   Certifications and credentials
 * @param languages
 *   Languages spoken
 * @param customSections
 *   Any sections that don't fit standard categories (awards, publications, volunteer work, etc.)
 */
final case class ExtractedResumeData(
    contact: Option[ContactInfo] = None,
    summary: Option[String] = None,
    experience: Option[List[WorkExperience]] = None,
    education: Option[List[Education]] = None,
    skills: Option[List[String]] = None,
    projects: Option[List[Project]] = None,
    certifications: Option[List[Certification]] = None,
    languages: Option[List[String]] = None,
    customSections: Option[List[CustomSection]] = None
) derives ReadWriter, Schematic

object ExtractedResumeData:
  /** Create an empty extraction result. */
  def empty: ExtractedResumeData = ExtractedResumeData()
