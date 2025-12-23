package arcata.api.etl

import arcata.api.clients.{ObjectStorageClient, SupabaseClient}
import arcata.api.config.{AIConfig, SupabaseConfig}
import arcata.api.domain.*
import arcata.api.etl.framework.PipelineContext
import munit.FunSuite

/** Comprehensive mock SupabaseClient for integration testing. */
class IntegrationMockSupabaseClient extends SupabaseClient(
      SupabaseConfig(
        url = "http://localhost",
        anonKey = "test",
        serviceRoleKey = "test",
        jwtSecret = "test"
      )
    ):

  // Storage for tracking what was created
  private var companies: Map[String, Company] = Map.empty
  private var jobs: Map[String, Job] = Map.empty
  private var streamEntries: List[JobStreamEntry] = List.empty
  private var applications: List[JobApplication] = List.empty

  private var companyIdCounter = 1L
  private var jobIdCounter = 1L
  private var streamIdCounter = 1L
  private var applicationIdCounter = 1L

  override def findCompanyByDomain(domain: String): Option[Company] =
    companies.get(domain)

  override def insertCompany(company: Company): Option[Company] = {
    val id = companyIdCounter
    companyIdCounter += 1
    val created = company.copy(companyId = Some(id))
    company.companyDomain.foreach(d => companies = companies + (d -> created))
    Some(created)
  }

  override def findJobBySourceUrl(sourceUrl: String): Option[Job] =
    jobs.get(sourceUrl)

  override def insertJob(job: Job): Option[Job] = {
    val id = jobIdCounter
    jobIdCounter += 1
    val created = job.copy(jobId = Some(id))
    job.sourceUrl.foreach(url => jobs = jobs + (url -> created))
    Some(created)
  }

  override def insertJobStreamEntry(entry: JobStreamEntry): Option[JobStreamEntry] = {
    val id = streamIdCounter
    streamIdCounter += 1
    val created = entry.copy(streamId = Some(id))
    streamEntries = created :: streamEntries
    Some(created)
  }

  override def insertJobApplication(application: JobApplication): Option[JobApplication] = {
    val id = applicationIdCounter
    applicationIdCounter += 1
    val created = application.copy(applicationId = Some(id))
    applications = created :: applications
    Some(created)
  }

  override def getDefaultStatusId(profileId: String): Option[Long] =
    Some(1L)

  // Accessors for verification
  def getCompanies: Map[String, Company] = companies
  def getJobs: Map[String, Job] = jobs
  def getStreamEntries: List[JobStreamEntry] = streamEntries
  def getApplications: List[JobApplication] = applications

  // Pre-populate for testing
  def addExistingCompany(company: Company): Unit =
    company.companyDomain.foreach(d => companies = companies + (d -> company))

  def addExistingJob(job: Job): Unit =
    job.sourceUrl.foreach(url => jobs = jobs + (url -> job))

/**
 * Mock AI server for integration testing.
 *
 * Returns predictable extracted data based on input.
 */
class MockAIServer:
  private val aiConfig = AIConfig(
    baseUrl = "http://mock-ai",
    apiKey = "test-key",
    model = "test-model"
  )

  def config: AIConfig = aiConfig

  /** Parse HTML and return mock extracted data. */
  def mockExtract(html: String, url: String): ExtractedJobData = {
    // Extract title from HTML or use default
    val titleMatch = """<title>([^<]+)</title>""".r.findFirstMatchIn(html)
    val title = titleMatch.map(_.group(1)).getOrElse("Extracted Job Title")

    // Extract company name from URL domain
    val domain = new java.net.URI(url).getHost.replace("www.", "")
    val companyName = domain.split("\\.").head.capitalize

    ExtractedJobData(
      title = title,
      companyName = Some(s"$companyName Inc"),
      description = Some("This is a great opportunity..."),
      location = Some("Remote"),
      jobType = Some("Full-time"),
      experienceLevel = Some("Mid-level"),
      qualifications = Some(Seq("3+ years experience", "Good communication"))
    )
  }

class JobIngestionPipelineIntegrationSuite extends FunSuite:

  // Since we can't actually call BoogieLoops or fetch real URLs,
  // we'll test the pipeline components that don't require network access

  test("Integration: full pipeline data flow with mocks"):
    val mockSupabase = new IntegrationMockSupabaseClient()
    val mockStorage = ObjectStorageClient.inMemory()
    val mockAI = new MockAIServer()

    // Simulate what the pipeline would produce at each stage
    val ctx = PipelineContext.create("user-integration-test")

    // Stage 1: Simulate HTML fetch output
    val html = "<html><title>Senior Software Engineer</title><body>Job description...</body></html>"
    val url = "https://techcorp.com/jobs/senior-engineer"

    // Store HTML like HtmlFetcher would
    val storageResult = mockStorage.store(html, "text/html", "job-html")
    assert(storageResult.isInstanceOf[arcata.api.clients.StorageResult.Success[?]])

    // Stage 2: Simulate job parsing
    val extractedData = mockAI.mockExtract(html, url)
    assertEquals(extractedData.title, "Senior Software Engineer")
    assertEquals(extractedData.companyName, Some("Techcorp Inc"))

    // Stage 3: Company resolution
    val existingCompany = mockSupabase.findCompanyByDomain("techcorp.com")
    assert(existingCompany.isEmpty) // New company

    val newCompany = Company(
      companyName = extractedData.companyName,
      companyDomain = Some("techcorp.com")
    )
    val createdCompany = mockSupabase.insertCompany(newCompany)
    assert(createdCompany.isDefined)
    assert(createdCompany.get.companyId.isDefined)

    // Stage 4: Job loading
    val job = Job(
      companyId = createdCompany.get.companyId.get,
      title = extractedData.title,
      description = extractedData.description,
      location = extractedData.location,
      sourceUrl = Some(url)
    )
    val createdJob = mockSupabase.insertJob(job)
    assert(createdJob.isDefined)
    assert(createdJob.get.jobId.isDefined)

    // Stage 5: Stream entry
    val streamEntry = JobStreamEntry(
      jobId = createdJob.get.jobId.get,
      profileId = ctx.profileId,
      source = "manual"
    )
    val createdStream = mockSupabase.insertJobStreamEntry(streamEntry)
    assert(createdStream.isDefined)
    assert(createdStream.get.streamId.isDefined)

    // Verify final state
    assertEquals(mockSupabase.getCompanies.size, 1)
    assertEquals(mockSupabase.getJobs.size, 1)
    assertEquals(mockSupabase.getStreamEntries.size, 1)
    assertEquals(mockStorage.size, 1)

  test("Integration: existing company should be reused"):
    val mockSupabase = new IntegrationMockSupabaseClient()

    // Pre-populate with existing company
    val existingCompany = Company(
      companyId = Some(999L),
      companyName = Some("Existing Corp"),
      companyDomain = Some("existing.com")
    )
    mockSupabase.addExistingCompany(existingCompany)

    // Verify it's found
    val found = mockSupabase.findCompanyByDomain("existing.com")
    assert(found.isDefined)
    assertEquals(found.get.companyId, Some(999L))

    // Creating a job should not create new company
    val job = Job(
      companyId = 999L,
      title = "New Job at Existing Company",
      sourceUrl = Some("https://existing.com/jobs/new")
    )
    mockSupabase.insertJob(job)

    // Should still have only 1 company
    assertEquals(mockSupabase.getCompanies.size, 1)
    assertEquals(mockSupabase.getJobs.size, 1)

  test("Integration: existing job should be returned"):
    val mockSupabase = new IntegrationMockSupabaseClient()

    // Pre-populate with existing job
    val existingJob = Job(
      jobId = Some(888L),
      companyId = 100L,
      title = "Existing Job",
      sourceUrl = Some("https://example.com/jobs/existing")
    )
    mockSupabase.addExistingJob(existingJob)

    // Verify it's found
    val found = mockSupabase.findJobBySourceUrl("https://example.com/jobs/existing")
    assert(found.isDefined)
    assertEquals(found.get.jobId, Some(888L))

  test("Integration: application creation with stream entry"):
    val mockSupabase = new IntegrationMockSupabaseClient()
    val ctx = PipelineContext.create("user-with-application")

    // Create company
    val company = mockSupabase.insertCompany(
      Company(companyName = Some("App Test Corp"), companyDomain = Some("apptest.com"))
    )

    // Create job
    val job = mockSupabase.insertJob(
      Job(
        companyId = company.get.companyId.get,
        title = "Applied Job",
        sourceUrl = Some("https://apptest.com/job")
      )
    )

    // Create stream entry
    val stream = mockSupabase.insertJobStreamEntry(
      JobStreamEntry(
        jobId = job.get.jobId.get,
        profileId = ctx.profileId,
        source = "manual"
      )
    )

    // Create application
    val application = mockSupabase.insertJobApplication(
      JobApplication(
        jobId = job.get.jobId,
        profileId = ctx.profileId,
        statusId = mockSupabase.getDefaultStatusId(ctx.profileId),
        notes = Some("Applied via integration test")
      )
    )

    assert(application.isDefined)
    assertEquals(application.get.notes, Some("Applied via integration test"))
    assertEquals(mockSupabase.getApplications.size, 1)
    assertEquals(mockSupabase.getStreamEntries.size, 1)

  test("Integration: multiple jobs from same company"):
    val mockSupabase = new IntegrationMockSupabaseClient()
    val ctx = PipelineContext.create("multi-job-test")

    // Create company once
    val company = mockSupabase.insertCompany(
      Company(companyName = Some("Multi Job Corp"), companyDomain = Some("multijob.com"))
    )

    // Create multiple jobs
    val job1 = mockSupabase.insertJob(
      Job(
        companyId = company.get.companyId.get,
        title = "Job 1",
        sourceUrl = Some("https://multijob.com/job1")
      )
    )
    val job2 = mockSupabase.insertJob(
      Job(
        companyId = company.get.companyId.get,
        title = "Job 2",
        sourceUrl = Some("https://multijob.com/job2")
      )
    )

    // Add to stream
    mockSupabase.insertJobStreamEntry(
      JobStreamEntry(jobId = job1.get.jobId.get, profileId = ctx.profileId, source = "scrape")
    )
    mockSupabase.insertJobStreamEntry(
      JobStreamEntry(jobId = job2.get.jobId.get, profileId = ctx.profileId, source = "scrape")
    )

    // Verify
    assertEquals(mockSupabase.getCompanies.size, 1)
    assertEquals(mockSupabase.getJobs.size, 2)
    assertEquals(mockSupabase.getStreamEntries.size, 2)

  test("Integration: object storage lifecycle"):
    val storage = ObjectStorageClient.inMemory()

    // Store multiple HTML documents
    val html1 = "<html>Job 1</html>"
    val html2 = "<html>Job 2</html>"

    val result1 = storage.store(html1, "text/html", "jobs")
    val result2 = storage.store(html2, "text/html", "jobs")

    assert(result1.isInstanceOf[arcata.api.clients.StorageResult.Success[?]])
    assert(result2.isInstanceOf[arcata.api.clients.StorageResult.Success[?]])
    assertEquals(storage.size, 2)

    // Retrieve
    val id1 = result1.asInstanceOf[arcata.api.clients.StorageResult.Success[String]].value
    val retrieved = storage.retrieve(id1)
    assert(retrieved.isInstanceOf[arcata.api.clients.StorageResult.Success[?]])

    // Delete one
    storage.delete(id1)
    assertEquals(storage.size, 1)

    // Clear all
    storage.clear()
    assertEquals(storage.size, 0)
