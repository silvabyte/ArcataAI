package arcata.api.domain

import upickle.default.*

/**
 * A job posting.
 *
 * @param jobId
 *   Unique identifier (auto-generated)
 * @param companyId
 *   Foreign key to companies table
 * @param title
 *   Job title
 * @param description
 *   Full job description
 * @param location
 *   Job location (e.g., "Remote", "San Francisco, CA")
 * @param jobType
 *   Employment type (e.g., "Full-time", "Contract")
 * @param experienceLevel
 *   Required experience level (e.g., "Senior", "Entry-level")
 * @param educationLevel
 *   Required education (e.g., "Bachelor's", "Master's")
 * @param salaryMin
 *   Minimum salary amount
 * @param salaryMax
 *   Maximum salary amount
 * @param salaryCurrency
 *   Currency code (default USD)
 * @param qualifications
 *   Required qualifications
 * @param preferredQualifications
 *   Nice-to-have qualifications
 * @param responsibilities
 *   Job responsibilities
 * @param benefits
 *   Benefits offered
 * @param category
 *   Job category (e.g., "Engineering", "Marketing")
 * @param sourceUrl
 *   Original URL where job was found
 * @param applicationUrl
 *   Direct URL to apply
 * @param isRemote
 *   Whether job allows remote work
 * @param postedDate
 *   When the job was posted
 * @param closingDate
 *   Application deadline
 * @param completionState
 *   Quality of extraction: Complete, Sufficient, Partial, Minimal, Failed, Unknown
 */
final case class Job(
    jobId: Option[Long] = None,
    companyId: Long,
    title: String,
    description: Option[String] = None,
    location: Option[String] = None,
    jobType: Option[String] = None,
    experienceLevel: Option[String] = None,
    educationLevel: Option[String] = None,
    salaryMin: Option[Int] = None,
    salaryMax: Option[Int] = None,
    salaryCurrency: Option[String] = Some("USD"),
    qualifications: Option[Seq[String]] = None,
    preferredQualifications: Option[Seq[String]] = None,
    responsibilities: Option[Seq[String]] = None,
    benefits: Option[Seq[String]] = None,
    category: Option[String] = None,
    sourceUrl: Option[String] = None,
    applicationUrl: Option[String] = None,
    isRemote: Option[Boolean] = None,
    postedDate: Option[String] = None,
    closingDate: Option[String] = None,
    completionState: Option[String] = None
)

object Job:
  // Custom ReadWriter to handle snake_case from Supabase
  given ReadWriter[Job] = readwriter[ujson.Value].bimap[Job](
    job => {
      val obj = ujson.Obj(
        "company_id" -> ujson.Num(job.companyId.toDouble),
        "title" -> job.title
      )
      job.jobId.foreach(v => obj("job_id") = ujson.Num(v.toDouble))
      job.description.foreach(v => obj("description") = v)
      job.location.foreach(v => obj("location") = v)
      job.jobType.foreach(v => obj("job_type") = v)
      job.experienceLevel.foreach(v => obj("experience_level") = v)
      job.educationLevel.foreach(v => obj("education_level") = v)
      job.salaryMin.foreach(v => obj("salary_min") = v)
      job.salaryMax.foreach(v => obj("salary_max") = v)
      job.salaryCurrency.foreach(v => obj("salary_currency") = v)
      job.qualifications.foreach(v => obj("qualifications") = ujson.Arr.from(v))
      job.preferredQualifications.foreach(v => obj("preferred_qualifications") = ujson.Arr.from(v))
      job.responsibilities.foreach(v => obj("responsibilities") = ujson.Arr.from(v))
      job.benefits.foreach(v => obj("benefits") = ujson.Arr.from(v))
      job.category.foreach(v => obj("category") = v)
      job.sourceUrl.foreach(v => obj("source_url") = v)
      job.applicationUrl.foreach(v => obj("application_url") = v)
      job.isRemote.foreach(v => obj("is_remote") = v)
      job.postedDate.foreach(v => obj("posted_date") = v)
      job.closingDate.foreach(v => obj("closing_date") = v)
      job.completionState.foreach(v => obj("completion_state") = v)
      obj
    },
    json => {
      val obj = json.obj
      Job(
        jobId = obj.get("job_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        companyId = obj("company_id").num.toLong,
        title = obj("title").str,
        description = obj.get("description").flatMap(v => if v.isNull then None else Some(v.str)),
        location = obj.get("location").flatMap(v => if v.isNull then None else Some(v.str)),
        jobType = obj.get("job_type").flatMap(v => if v.isNull then None else Some(v.str)),
        experienceLevel = obj.get("experience_level").flatMap(v => if v.isNull then None else Some(v.str)),
        educationLevel = obj.get("education_level").flatMap(v => if v.isNull then None else Some(v.str)),
        salaryMin = obj.get("salary_min").flatMap(v => if v.isNull then None else Some(v.num.toInt)),
        salaryMax = obj.get("salary_max").flatMap(v => if v.isNull then None else Some(v.num.toInt)),
        salaryCurrency = obj.get("salary_currency").flatMap(v => if v.isNull then None else Some(v.str)),
        qualifications =
          obj.get("qualifications").flatMap(v => if v.isNull then None else Some(v.arr.map(_.str).toSeq)),
        preferredQualifications =
          obj.get("preferred_qualifications").flatMap(v => if v.isNull then None else Some(v.arr.map(_.str).toSeq)),
        responsibilities =
          obj.get("responsibilities").flatMap(v => if v.isNull then None else Some(v.arr.map(_.str).toSeq)),
        benefits = obj.get("benefits").flatMap(v => if v.isNull then None else Some(v.arr.map(_.str).toSeq)),
        category = obj.get("category").flatMap(v => if v.isNull then None else Some(v.str)),
        sourceUrl = obj.get("source_url").flatMap(v => if v.isNull then None else Some(v.str)),
        applicationUrl = obj.get("application_url").flatMap(v => if v.isNull then None else Some(v.str)),
        isRemote = obj.get("is_remote").flatMap(v => if v.isNull then None else Some(v.bool)),
        postedDate = obj.get("posted_date").flatMap(v => if v.isNull then None else Some(v.str)),
        closingDate = obj.get("closing_date").flatMap(v => if v.isNull then None else Some(v.str)),
        completionState = obj.get("completion_state").flatMap(v => if v.isNull then None else Some(v.str))
      )
    }
  )
