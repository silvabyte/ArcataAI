package arcata.api.domain

import utest.*
import upickle.default.*

object DomainModelsSuite extends TestSuite:
  val tests = Tests {
    test("Company") {
      test("serializes to JSON and back") {
        val company = Company(
          companyId = Some(123L),
          companyName = Some("Acme Corp"),
          companyDomain = Some("acme.com"),
          industry = Some("Technology"),
          companySize = Some("medium")
        )

        val json = write(company)
        val parsed = read[Company](json)

        assert(parsed == company)
      }

      test("fromDomain creates company with domain") {
        val company = Company.fromDomain("example.com")

        assert(company.companyDomain == Some("example.com"))
        assert(company.companyName == None)
        assert(company.companyId == None)
      }

      test("handles all optional fields as None") {
        val company = Company()
        val json = write(company)
        val parsed = read[Company](json)

        assert(parsed == company)
        assert(parsed.companyId == None)
      }
    }

    test("Job") {
      test("serializes to JSON and back") {
        val job = Job(
          jobId = Some(456L),
          companyId = Some(123L),
          title = "Software Engineer",
          description = Some("Build great things"),
          location = Some("Remote"),
          qualifications = Some(Seq("Scala", "TypeScript"))
        )

        val json = write(job)
        val parsed = read[Job](json)

        assert(parsed == job)
      }

      test("handles minimal required fields") {
        val job = Job(companyId = Some(1L), title = "Engineer")
        val json = write(job)
        val parsed = read[Job](json)

        assert(parsed.companyId == Some(1L))
        assert(parsed.title == "Engineer")
        assert(parsed.salaryCurrency == Some("USD")) // default value
      }

      test("handles orphaned job (no company)") {
        val job = Job(companyId = None, title = "Orphaned Job")
        val json = write(job)
        val parsed = read[Job](json)

        assert(parsed.companyId == None)
        assert(parsed.title == "Orphaned Job")
      }
    }

    test("ExtractedJobData") {
      test("serializes to JSON and back") {
        val data = ExtractedJobData(
          title = "Senior Developer",
          companyName = Some("Tech Corp"),
          qualifications = Some(List("Python", "Go")),
          responsibilities = Some(List("Lead team", "Write code"))
        )

        val json = write(data)
        val parsed = read[ExtractedJobData](json)

        assert(parsed == data)
      }

      test("minimal creates extraction with just title") {
        val data = ExtractedJobData.minimal("Engineer")

        assert(data.title == "Engineer")
        assert(data.companyName == None)
        assert(data.qualifications == None)
      }
    }

    test("JobStreamEntry") {
      test("serializes to JSON and back") {
        val entry = JobStreamEntry(
          streamId = Some(789L),
          jobId = 456L,
          profileId = "user-123",
          source = "manual",
          status = Some("new")
        )

        val json = write(entry)
        val parsed = read[JobStreamEntry](json)

        assert(parsed == entry)
      }
    }

    test("JobApplication") {
      test("serializes to JSON and back") {
        val app = JobApplication(
          applicationId = Some(101L),
          jobId = Some(456L),
          profileId = "user-123",
          statusOrder = 5,
          notes = Some("Great opportunity")
        )

        val json = write(app)
        val parsed = read[JobApplication](json)

        assert(parsed == app)
      }

      test("handles minimal fields") {
        val app = JobApplication(profileId = "user-1", statusOrder = 0)
        val json = write(app)
        val parsed = read[JobApplication](json)

        assert(parsed.profileId == "user-1")
        assert(parsed.jobId == None)
      }
    }
  }
