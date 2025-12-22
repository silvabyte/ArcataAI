package arcata.api.domain

import munit.FunSuite
import upickle.default.*

class DomainModelsSuite extends FunSuite:

  test("Company should serialize to JSON and back"):
    val company = Company(
      companyId = Some(123L),
      companyName = Some("Acme Corp"),
      companyDomain = Some("acme.com")
    )

    val json = write(company)
    val parsed = read[Company](json)

    assertEquals(parsed, company)

  test("Company.fromDomain should create company with domain"):
    val company = Company.fromDomain("example.com")

    assertEquals(company.companyDomain, Some("example.com"))
    assertEquals(company.companyName, None)

  test("Job should serialize to JSON and back"):
    val job = Job(
      jobId = Some(456L),
      companyId = 123L,
      title = "Software Engineer",
      description = Some("Build great things"),
      location = Some("Remote"),
      qualifications = Some(Seq("Scala", "TypeScript"))
    )

    val json = write(job)
    val parsed = read[Job](json)

    assertEquals(parsed, job)

  test("ExtractedJobData should serialize to JSON and back"):
    val data = ExtractedJobData(
      title = "Senior Developer",
      companyName = Some("Tech Corp"),
      qualifications = Some(Seq("Python", "Go"))
    )

    val json = write(data)
    val parsed = read[ExtractedJobData](json)

    assertEquals(parsed, data)

  test("ExtractedJobData.minimal should create minimal extraction"):
    val data = ExtractedJobData.minimal("Engineer")

    assertEquals(data.title, "Engineer")
    assertEquals(data.companyName, None)

  test("JobStreamEntry should serialize to JSON and back"):
    val entry = JobStreamEntry(
      streamId = Some(789L),
      jobId = 456L,
      profileId = "user-123",
      source = "manual",
      status = Some("new")
    )

    val json = write(entry)
    val parsed = read[JobStreamEntry](json)

    assertEquals(parsed, entry)

  test("JobApplication should serialize to JSON and back"):
    val app = JobApplication(
      applicationId = Some(101L),
      jobId = Some(456L),
      profileId = "user-123",
      statusOrder = 5,
      notes = Some("Great opportunity")
    )

    val json = write(app)
    val parsed = read[JobApplication](json)

    assertEquals(parsed, app)
