package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig
import arcata.api.domain.{Company, ExtractedJobData, Job}
import arcata.api.etl.framework.{PipelineContext, StepError}
import munit.FunSuite

/** Mock SupabaseClient for testing JobLoader. */
class MockJobSupabaseClient(
    existingJobs: Map[String, Job] = Map.empty,
    shouldFailInsert: Boolean = false
) extends SupabaseClient(
      SupabaseConfig(
        url = "http://localhost",
        anonKey = "test",
        serviceRoleKey = "test",
        jwtSecret = "test"
      )
    ):

  private var insertedJobs: List[Job] = List.empty

  override def findJobBySourceUrl(sourceUrl: String): Option[Job] =
    existingJobs.get(sourceUrl)

  override def insertJob(job: Job): Option[Job] = {
    if shouldFailInsert then None
    else {
      val created = job.copy(jobId = Some(System.currentTimeMillis()))
      insertedJobs = created :: insertedJobs
      Some(created)
    }
  }

  def getInsertedJobs: List[Job] = insertedJobs

class JobLoaderSuite extends FunSuite:

  val ctx: PipelineContext = PipelineContext.create("test-profile")

  val testCompany: Company = Company(
    companyId = Some(100L),
    companyName = Some("Test Corp"),
    companyDomain = Some("test.com")
  )

  val testExtractedData: ExtractedJobData = ExtractedJobData(
    title = "Software Engineer",
    description = Some("Build great software"),
    location = Some("Remote"),
    qualifications = Some(Seq("Scala", "TypeScript"))
  )

  test("JobLoader should create new job"):
    val mockClient = new MockJobSupabaseClient()
    val loader = JobLoader(mockClient)

    val input = JobLoaderInput(
      extractedData = testExtractedData,
      company = testCompany,
      url = "https://test.com/jobs/123",
      objectId = Some("html-obj-1")
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assert(output.job.jobId.isDefined)
      assertEquals(output.job.title, "Software Engineer")
      assertEquals(output.job.companyId, 100L)
      assertEquals(output.job.sourceUrl, Some("https://test.com/jobs/123"))
      assertEquals(output.job.rawHtmlObjectId, Some("html-obj-1"))
      assertEquals(output.company, testCompany)
    }
    assertEquals(mockClient.getInsertedJobs.length, 1)

  test("JobLoader should return existing job if already exists"):
    val existingJob = Job(
      jobId = Some(999L),
      companyId = 100L,
      title = "Existing Job",
      sourceUrl = Some("https://test.com/jobs/existing")
    )
    val mockClient = new MockJobSupabaseClient(
      existingJobs = Map("https://test.com/jobs/existing" -> existingJob)
    )
    val loader = JobLoader(mockClient)

    val input = JobLoaderInput(
      extractedData = testExtractedData,
      company = testCompany,
      url = "https://test.com/jobs/existing",
      objectId = None
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.job.jobId, Some(999L))
      assertEquals(output.job.title, "Existing Job")
    }
    // Should not insert since job already exists
    assertEquals(mockClient.getInsertedJobs.length, 0)

  test("JobLoader should fail if company has no ID"):
    val mockClient = new MockJobSupabaseClient()
    val loader = JobLoader(mockClient)
    val companyWithoutId = Company(companyName = Some("No ID Corp"))

    val input = JobLoaderInput(
      extractedData = testExtractedData,
      company = companyWithoutId,
      url = "https://test.com/jobs/123",
      objectId = None
    )

    val result = loader.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.ValidationError])
      assert(error.message.contains("Company must have an ID"))
    }

  test("JobLoader should fail when insert fails"):
    val mockClient = new MockJobSupabaseClient(shouldFailInsert = true)
    val loader = JobLoader(mockClient)

    val input = JobLoaderInput(
      extractedData = testExtractedData,
      company = testCompany,
      url = "https://test.com/jobs/new",
      objectId = None
    )

    val result = loader.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.LoadError])
    }

  test("JobLoader should map all extracted data fields"):
    val fullExtractedData = ExtractedJobData(
      title = "Senior Developer",
      description = Some("Lead development"),
      location = Some("New York"),
      jobType = Some("Full-time"),
      experienceLevel = Some("Senior"),
      educationLevel = Some("Bachelor's"),
      salaryRange = Some("$150k-$200k"),
      qualifications = Some(Seq("Python", "AWS")),
      preferredQualifications = Some(Seq("Kubernetes")),
      responsibilities = Some(Seq("Lead team", "Code review")),
      benefits = Some(Seq("Health", "401k")),
      category = Some("Engineering"),
      applicationUrl = Some("https://apply.test.com"),
      applicationEmail = Some("jobs@test.com"),
      postedDate = Some("2024-01-15"),
      closingDate = Some("2024-02-15")
    )
    val mockClient = new MockJobSupabaseClient()
    val loader = JobLoader(mockClient)

    val input = JobLoaderInput(
      extractedData = fullExtractedData,
      company = testCompany,
      url = "https://test.com/jobs/full",
      objectId = Some("obj-full")
    )

    val result = loader.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.job.title, "Senior Developer")
      assertEquals(output.job.description, Some("Lead development"))
      assertEquals(output.job.location, Some("New York"))
      assertEquals(output.job.jobType, Some("Full-time"))
      assertEquals(output.job.experienceLevel, Some("Senior"))
      assertEquals(output.job.salaryRange, Some("$150k-$200k"))
      assertEquals(output.job.qualifications, Some(Seq("Python", "AWS")))
      assertEquals(output.job.applicationUrl, Some("https://apply.test.com"))
    }
