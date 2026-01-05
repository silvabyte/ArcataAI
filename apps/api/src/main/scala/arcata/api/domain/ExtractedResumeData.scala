package arcata.api.domain

import boogieloops.schema.derivation.Schematic
import upickle.default.*

/**
 * Contact information extracted from a resume.
 *
 * Field names match the frontend Lexical ContactNode schema.
 *
 * @param name
 *   Full name of the candidate
 * @param email
 *   Email address
 * @param phone
 *   Phone number
 * @param location
 *   City, state, or full address
 * @param linkedIn
 *   LinkedIn profile URL
 * @param github
 *   GitHub profile URL
 * @param portfolio
 *   Personal website or portfolio URL
 */
final case class ContactInfo(
  name: Option[String] = None,
  email: Option[String] = None,
  phone: Option[String] = None,
  location: Option[String] = None,
  linkedIn: Option[String] = None,
  github: Option[String] = None,
  portfolio: Option[String] = None,
) derives ReadWriter, Schematic

/**
 * Professional summary extracted from a resume.
 *
 * @param headline
 *   Job title or professional tagline (e.g., "Senior Software Engineer")
 * @param summary
 *   Professional summary or objective statement
 */
final case class SummaryInfo(
  headline: Option[String] = None,
  summary: Option[String] = None,
) derives ReadWriter, Schematic

/**
 * Work experience entry extracted from a resume.
 *
 * Field names and date format match the frontend Lexical ExperienceNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param company
 *   Company or organization name
 * @param title
 *   Job title or role
 * @param location
 *   Location of the job
 * @param startDate
 *   Start date in YYYY-MM format (e.g., "2024-02")
 * @param endDate
 *   End date in YYYY-MM format, or empty string if current
 * @param current
 *   Whether this is the current position
 * @param highlights
 *   Key accomplishments as bullet points
 */
final case class WorkExperience(
  id: Option[String] = None,
  company: Option[String] = None,
  title: Option[String] = None,
  location: Option[String] = None,
  startDate: Option[String] = None,
  endDate: Option[String] = None,
  current: Option[Boolean] = None,
  highlights: Option[List[String]] = None,
) derives ReadWriter, Schematic

/**
 * Education entry extracted from a resume.
 *
 * Field names match the frontend Lexical EducationNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param institution
 *   School or university name
 * @param degree
 *   Degree type (e.g., "Bachelor of Science", "MBA")
 * @param field
 *   Major or field of study
 * @param location
 *   Location of the institution
 * @param startDate
 *   Start date in YYYY-MM format
 * @param endDate
 *   End date in YYYY-MM format, or empty string if current
 * @param current
 *   Whether currently studying here
 * @param gpa
 *   GPA if listed
 * @param honors
 *   Honors, awards, or notable achievements
 * @param coursework
 *   Relevant coursework
 */
final case class Education(
  id: Option[String] = None,
  institution: Option[String] = None,
  degree: Option[String] = None,
  field: Option[String] = None,
  location: Option[String] = None,
  startDate: Option[String] = None,
  endDate: Option[String] = None,
  current: Option[Boolean] = None,
  gpa: Option[String] = None,
  honors: Option[String] = None,
  coursework: Option[List[String]] = None,
) derives ReadWriter, Schematic

/**
 * A skill category containing related skills.
 *
 * @param id
 *   Unique identifier for the category
 * @param name
 *   Category name (e.g., "Programming Languages", "Frameworks")
 * @param skills
 *   List of skills in this category
 */
final case class SkillCategory(
  id: Option[String] = None,
  name: Option[String] = None,
  skills: Option[List[String]] = None,
) derives ReadWriter, Schematic

/**
 * Skills data with categorized skills.
 *
 * @param categories
 *   List of skill categories
 */
final case class SkillsData(
  categories: Option[List[SkillCategory]] = None
) derives ReadWriter, Schematic

/**
 * Project entry extracted from a resume.
 *
 * Field names match the frontend Lexical ProjectsNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param name
 *   Project name
 * @param description
 *   What the project does
 * @param url
 *   Link to project (GitHub, demo, etc.)
 * @param startDate
 *   Start date in YYYY-MM format
 * @param endDate
 *   End date in YYYY-MM format, or empty string if current
 * @param current
 *   Whether this is an ongoing project
 * @param technologies
 *   Technologies or tools used
 * @param highlights
 *   Key accomplishments or features as bullet points
 */
final case class Project(
  id: Option[String] = None,
  name: Option[String] = None,
  description: Option[String] = None,
  url: Option[String] = None,
  startDate: Option[String] = None,
  endDate: Option[String] = None,
  current: Option[Boolean] = None,
  technologies: Option[List[String]] = None,
  highlights: Option[List[String]] = None,
) derives ReadWriter, Schematic

/**
 * Certification or credential extracted from a resume.
 *
 * Field names match the frontend Lexical CertificationsNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param name
 *   Certification name
 * @param issuer
 *   Issuing organization
 * @param issueDate
 *   When the certification was obtained in YYYY-MM format
 * @param expirationDate
 *   Expiration date in YYYY-MM format if applicable
 * @param credentialId
 *   Credential ID if provided
 * @param credentialUrl
 *   Verification URL
 * @param noExpiration
 *   Whether this credential does not expire
 */
final case class Certification(
  id: Option[String] = None,
  name: Option[String] = None,
  issuer: Option[String] = None,
  issueDate: Option[String] = None,
  expirationDate: Option[String] = None,
  credentialId: Option[String] = None,
  credentialUrl: Option[String] = None,
  noExpiration: Option[Boolean] = None,
) derives ReadWriter, Schematic

/**
 * Language entry with proficiency level.
 *
 * Field names match the frontend Lexical LanguagesNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param language
 *   Name of the language
 * @param proficiency
 *   Proficiency level: native, fluent, advanced, intermediate, or beginner
 */
final case class LanguageEntry(
  id: Option[String] = None,
  language: Option[String] = None,
  proficiency: Option[String] = None,
) derives ReadWriter, Schematic

/**
 * Languages data with structured entries.
 *
 * @param entries
 *   List of language entries with proficiency levels
 */
final case class LanguagesData(
  entries: Option[List[LanguageEntry]] = None
) derives ReadWriter, Schematic

/**
 * Volunteer experience entry extracted from a resume.
 *
 * Field names match the frontend Lexical VolunteerNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param organization
 *   Organization or charity name
 * @param role
 *   Volunteer role or title
 * @param location
 *   Location of the organization
 * @param startDate
 *   Start date in YYYY-MM format
 * @param endDate
 *   End date in YYYY-MM format, or empty string if current
 * @param current
 *   Whether currently volunteering here
 * @param highlights
 *   Key contributions as bullet points
 */
final case class VolunteerExperience(
  id: Option[String] = None,
  organization: Option[String] = None,
  role: Option[String] = None,
  location: Option[String] = None,
  startDate: Option[String] = None,
  endDate: Option[String] = None,
  current: Option[Boolean] = None,
  highlights: Option[List[String]] = None,
) derives ReadWriter, Schematic

/**
 * Award or honor entry extracted from a resume.
 *
 * Field names match the frontend Lexical AwardsNode schema.
 *
 * @param id
 *   Unique identifier for the entry
 * @param title
 *   Award title or name
 * @param issuer
 *   Issuing organization
 * @param date
 *   Date received in YYYY-MM format
 * @param description
 *   Description of the award
 */
final case class Award(
  id: Option[String] = None,
  title: Option[String] = None,
  issuer: Option[String] = None,
  date: Option[String] = None,
  description: Option[String] = None,
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
  items: Option[List[String]] = None,
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
 *   Professional summary with headline and text
 * @param experience
 *   Work experience entries, in chronological order as they appear
 * @param education
 *   Education entries
 * @param skills
 *   Skills organized by categories
 * @param projects
 *   Projects section
 * @param certifications
 *   Certifications and credentials
 * @param languages
 *   Languages spoken with proficiency levels
 * @param volunteer
 *   Volunteer experience entries
 * @param awards
 *   Awards and honors
 * @param customSections
 *   Any sections that don't fit standard categories (publications, patents, etc.)
 */
final case class ExtractedResumeData(
  contact: Option[ContactInfo] = None,
  summary: Option[SummaryInfo] = None,
  experience: Option[List[WorkExperience]] = None,
  education: Option[List[Education]] = None,
  skills: Option[SkillsData] = None,
  projects: Option[List[Project]] = None,
  certifications: Option[List[Certification]] = None,
  languages: Option[LanguagesData] = None,
  volunteer: Option[List[VolunteerExperience]] = None,
  awards: Option[List[Award]] = None,
  customSections: Option[List[CustomSection]] = None,
) derives ReadWriter, Schematic

object ExtractedResumeData:
  /** Create an empty extraction result. */
  def empty: ExtractedResumeData = ExtractedResumeData()
