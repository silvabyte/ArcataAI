package arcata.api.etl.sources

import utest.*
import upickle.default.*

object JobSourceSuite extends TestSuite:
  val tests = Tests {
    test("JobSource enum") {
      test("all returns all sources") {
        val sources = JobSource.all
        assert(sources.nonEmpty)
        assert(sources.contains(JobSource.Greenhouse))
      }

      test("get returns source by id") {
        val source = JobSource.get("greenhouse")
        assert(source.isDefined)
        assert(source.get == JobSource.Greenhouse)
        assert(source.get.sourceId == "greenhouse")
        assert(source.get.name == "Greenhouse ATS")
      }

      test("get is case-insensitive") {
        assert(JobSource.get("GREENHOUSE").isDefined)
        assert(JobSource.get("Greenhouse").isDefined)
        assert(JobSource.get("greenhouse").isDefined)
      }

      test("get returns None for unknown id") {
        val source = JobSource.get("nonexistent")
        assert(source.isEmpty)
      }

      test("fromString is alias for get") {
        assert(JobSource.fromString("greenhouse") == JobSource.get("greenhouse"))
      }

      test("sourceIds returns all source IDs") {
        val ids = JobSource.sourceIds
        assert(ids.contains("greenhouse"))
      }

      test("toString returns sourceId") {
        assert(JobSource.Greenhouse.toString == "greenhouse")
      }

      test("all sources have required properties") {
        for source <- JobSource.all do
          // Every source must have a non-empty sourceId
          assert(source.sourceId.nonEmpty)

          // Every source must have a non-empty name
          assert(source.name.nonEmpty)

          // Config must have positive values
          assert(source.config.companyBatchSize > 0)
          assert(source.config.jobsPerCompany > 0)
          assert(source.config.delayBetweenRequestsMs >= 0)
      }
    }

    test("Greenhouse source") {
      test("has correct sourceId") {
        assert(JobSource.Greenhouse.sourceId == "greenhouse")
      }

      test("has correct name") {
        assert(JobSource.Greenhouse.name == "Greenhouse ATS")
      }

      test("has conservative defaults") {
        val config = JobSource.Greenhouse.config
        assert(config.companyBatchSize == 1)
        assert(config.jobsPerCompany == 1)
        assert(config.delayBetweenRequestsMs == 1000)
      }
    }

    test("JobSourceConfig") {
      test("has sensible defaults") {
        val config = JobSourceConfig()
        assert(config.companyBatchSize == 1)
        assert(config.jobsPerCompany == 1)
        assert(config.delayBetweenRequestsMs == 1000)
      }

      test("can be customized") {
        val config = JobSourceConfig(
          companyBatchSize = 10,
          jobsPerCompany = 50,
          delayBetweenRequestsMs = 500
        )
        assert(config.companyBatchSize == 10)
        assert(config.jobsPerCompany == 50)
        assert(config.delayBetweenRequestsMs == 500)
      }
    }

    test("DiscoveredJob") {
      test("can be created with minimal fields") {
        val job = DiscoveredJob(
          url = "https://example.com/job/123",
          source = JobSource.Greenhouse
        )
        assert(job.url == "https://example.com/job/123")
        assert(job.source == JobSource.Greenhouse)
        assert(job.companyId == None)
        assert(job.apiUrl == None)
        assert(job.metadata.isEmpty)
      }

      test("can be created with all fields") {
        val job = DiscoveredJob(
          url = "https://example.com/job/123",
          source = JobSource.Greenhouse,
          companyId = Some(456L),
          apiUrl = Some("https://boards-api.greenhouse.io/v1/boards/acme/jobs/789"),
          metadata = Map("greenhouse_job_id" -> "789", "title" -> "Engineer")
        )
        assert(job.url == "https://example.com/job/123")
        assert(job.source == JobSource.Greenhouse)
        assert(job.companyId == Some(456L))
        assert(job.apiUrl == Some("https://boards-api.greenhouse.io/v1/boards/acme/jobs/789"))
        assert(job.metadata("greenhouse_job_id") == "789")
        assert(job.metadata("title") == "Engineer")
      }

      test("source is required") {
        // DiscoveredJob now requires a source for routing to appropriate pipeline
        val job = DiscoveredJob(
          url = "https://example.com/job/123",
          source = JobSource.Greenhouse
        )
        assert(job.source.sourceId == "greenhouse")
      }
    }
  }
