package arcata.api.domain

import upickle.default.*

/**
 * A company that posts jobs.
 *
 * @param companyId
 *   Unique identifier (auto-generated)
 * @param companyName
 *   Name of the company
 * @param companyDomain
 *   Primary domain (e.g., "example.com")
 * @param companyJobsUrl
 *   URL to company's job board
 * @param companyLinkedinUrl
 *   LinkedIn company page URL
 * @param companyCity
 *   City where company is headquartered
 * @param companyState
 *   State/province
 * @param primaryIndustry
 *   Primary industry classification
 * @param employeeCountMin
 *   Minimum employee count estimate
 * @param employeeCountMax
 *   Maximum employee count estimate
 * @param industry
 *   AI-extracted industry classification
 * @param companySize
 *   Company size category (startup/small/medium/large/enterprise)
 * @param description
 *   Brief company description from AI
 * @param headquarters
 *   Headquarters location (e.g., "San Francisco, CA")
 * @param companyJobsSource
 *   ATS platform type (greenhouse, lever, ashby, workday, icims, workable, custom)
 */
final case class Company(
    companyId: Option[Long] = None,
    companyName: Option[String] = None,
    companyDomain: Option[String] = None,
    companyJobsUrl: Option[String] = None,
    companyLinkedinUrl: Option[String] = None,
    companyCity: Option[String] = None,
    companyState: Option[String] = None,
    primaryIndustry: Option[String] = None,
    employeeCountMin: Option[Int] = None,
    employeeCountMax: Option[Int] = None,
    // Enrichment fields
    industry: Option[String] = None,
    companySize: Option[String] = None,
    description: Option[String] = None,
    headquarters: Option[String] = None,
    // Job source tracking
    companyJobsSource: Option[String] = None
)

object Company:
  /** Create a minimal company with just the domain. */
  def fromDomain(domain: String): Company =
    Company(companyDomain = Some(domain))

  // Custom ReadWriter to handle snake_case from Supabase
  given ReadWriter[Company] = readwriter[ujson.Value].bimap[Company](
    company => {
      val obj = ujson.Obj()
      company.companyId.foreach(v => obj("company_id") = ujson.Num(v.toDouble))
      company.companyName.foreach(v => obj("company_name") = v)
      company.companyDomain.foreach(v => obj("company_domain") = v)
      company.companyJobsUrl.foreach(v => obj("company_jobs_url") = v)
      company.companyLinkedinUrl.foreach(v => obj("company_linkedin_url") = v)
      company.companyCity.foreach(v => obj("company_city") = v)
      company.companyState.foreach(v => obj("company_state") = v)
      company.primaryIndustry.foreach(v => obj("primary_industry") = v)
      company.employeeCountMin.foreach(v => obj("employee_count_min") = v)
      company.employeeCountMax.foreach(v => obj("employee_count_max") = v)
      company.industry.foreach(v => obj("industry") = v)
      company.companySize.foreach(v => obj("company_size") = v)
      company.description.foreach(v => obj("description") = v)
      company.headquarters.foreach(v => obj("headquarters") = v)
      company.companyJobsSource.foreach(v => obj("company_jobs_source") = v)
      obj
    },
    json => {
      val obj = json.obj
      Company(
        companyId = obj.get("company_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        companyName = obj.get("company_name").flatMap(v => if v.isNull then None else Some(v.str)),
        companyDomain = obj.get("company_domain").flatMap(v => if v.isNull then None else Some(v.str)),
        companyJobsUrl = obj.get("company_jobs_url").flatMap(v => if v.isNull then None else Some(v.str)),
        companyLinkedinUrl = obj.get("company_linkedin_url").flatMap(v => if v.isNull then None else Some(v.str)),
        companyCity = obj.get("company_city").flatMap(v => if v.isNull then None else Some(v.str)),
        companyState = obj.get("company_state").flatMap(v => if v.isNull then None else Some(v.str)),
        primaryIndustry = obj.get("primary_industry").flatMap(v => if v.isNull then None else Some(v.str)),
        employeeCountMin = obj.get("employee_count_min").flatMap(v => if v.isNull then None else Some(v.num.toInt)),
        employeeCountMax = obj.get("employee_count_max").flatMap(v => if v.isNull then None else Some(v.num.toInt)),
        industry = obj.get("industry").flatMap(v => if v.isNull then None else Some(v.str)),
        companySize = obj.get("company_size").flatMap(v => if v.isNull then None else Some(v.str)),
        description = obj.get("description").flatMap(v => if v.isNull then None else Some(v.str)),
        headquarters = obj.get("headquarters").flatMap(v => if v.isNull then None else Some(v.str)),
        companyJobsSource = obj.get("company_jobs_source").flatMap(v => if v.isNull then None else Some(v.str))
      )
    }
  )
