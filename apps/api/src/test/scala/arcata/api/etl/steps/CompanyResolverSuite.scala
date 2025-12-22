package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.config.SupabaseConfig
import arcata.api.domain.{Company, ExtractedJobData}
import arcata.api.etl.framework.{PipelineContext, StepError}
import munit.FunSuite

/** Mock SupabaseClient for testing CompanyResolver. */
class MockSupabaseClient(
    existingCompanies: Map[String, Company] = Map.empty,
    shouldFailInsert: Boolean = false
) extends SupabaseClient(
      SupabaseConfig(
        url = "http://localhost",
        anonKey = "test",
        serviceRoleKey = "test",
        jwtSecret = "test"
      )
    ):

  private var insertedCompanies: List[Company] = List.empty

  override def findCompanyByDomain(domain: String): Option[Company] =
    existingCompanies.get(domain)

  override def insertCompany(company: Company): Option[Company] = {
    if shouldFailInsert then None
    else {
      val created = company.copy(companyId = Some(System.currentTimeMillis()))
      insertedCompanies = created :: insertedCompanies
      Some(created)
    }
  }

  def getInsertedCompanies: List[Company] = insertedCompanies

class CompanyResolverSuite extends FunSuite:

  val ctx: PipelineContext = PipelineContext.create("test-profile")

  test("CompanyResolver should find existing company by domain"):
    val existingCompany = Company(
      companyId = Some(123L),
      companyName = Some("Acme Corp"),
      companyDomain = Some("acme.com")
    )
    val mockClient = new MockSupabaseClient(
      existingCompanies = Map("acme.com" -> existingCompany)
    )
    val resolver = CompanyResolver(mockClient)

    val input = CompanyResolverInput(
      extractedData = ExtractedJobData.minimal("Software Engineer"),
      url = "https://www.acme.com/jobs/123",
      objectId = None
    )

    val result = resolver.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.company.companyId, Some(123L))
      assertEquals(output.company.companyName, Some("Acme Corp"))
    }

  test("CompanyResolver should create new company when not found"):
    val mockClient = new MockSupabaseClient()
    val resolver = CompanyResolver(mockClient)

    val input = CompanyResolverInput(
      extractedData = ExtractedJobData(
        title = "Software Engineer",
        companyName = Some("New Company")
      ),
      url = "https://newcompany.io/careers/job",
      objectId = Some("obj-123")
    )

    val result = resolver.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assert(output.company.companyId.isDefined)
      assertEquals(output.company.companyName, Some("New Company"))
      assertEquals(output.company.companyDomain, Some("newcompany.io"))
    }
    assertEquals(mockClient.getInsertedCompanies.length, 1)

  test("CompanyResolver should strip www prefix from domain"):
    val mockClient = new MockSupabaseClient()
    val resolver = CompanyResolver(mockClient)

    val input = CompanyResolverInput(
      extractedData = ExtractedJobData.minimal("Engineer"),
      url = "https://www.example.com/jobs",
      objectId = None
    )

    val result = resolver.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.company.companyDomain, Some("example.com"))
    }

  test("CompanyResolver should fail on invalid URL"):
    val mockClient = new MockSupabaseClient()
    val resolver = CompanyResolver(mockClient)

    val input = CompanyResolverInput(
      extractedData = ExtractedJobData.minimal("Engineer"),
      url = "not-a-valid-url",
      objectId = None
    )

    val result = resolver.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.ValidationError])
      assert(error.message.contains("Could not extract domain"))
    }

  test("CompanyResolver should fail when insert fails"):
    val mockClient = new MockSupabaseClient(shouldFailInsert = true)
    val resolver = CompanyResolver(mockClient)

    val input = CompanyResolverInput(
      extractedData = ExtractedJobData.minimal("Engineer"),
      url = "https://example.com/jobs",
      objectId = None
    )

    val result = resolver.run(input, ctx)

    assert(result.isLeft)
    result.left.foreach { error =>
      assert(error.isInstanceOf[StepError.LoadError])
    }

  test("CompanyResolver should preserve extracted data and objectId"):
    val mockClient = new MockSupabaseClient()
    val resolver = CompanyResolver(mockClient)
    val extractedData = ExtractedJobData(
      title = "Senior Engineer",
      description = Some("Great role"),
      location = Some("Remote")
    )

    val input = CompanyResolverInput(
      extractedData = extractedData,
      url = "https://tech.co/job",
      objectId = Some("storage-id-456")
    )

    val result = resolver.run(input, ctx)

    assert(result.isRight)
    result.foreach { output =>
      assertEquals(output.extractedData, extractedData)
      assertEquals(output.objectId, Some("storage-id-456"))
      assertEquals(output.url, "https://tech.co/job")
    }
