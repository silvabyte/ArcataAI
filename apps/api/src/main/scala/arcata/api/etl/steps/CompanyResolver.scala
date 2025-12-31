package arcata.api.etl.steps

import arcata.api.ai.CompanyEnrichmentAgent
import arcata.api.clients.SupabaseClient
import arcata.api.config.AIConfig
import arcata.api.domain.{Company, ExtractedCompanyData, ExtractedJobData}
import arcata.api.etl.framework.*
import boogieloops.ai.SchemaError

/** Input for the CompanyResolver step. */
final case class CompanyResolverInput(
    extractedData: Transformed[ExtractedJobData],
    url: String,
    objectId: Option[String],
    content: String
)

/** Output from the CompanyResolver step. */
final case class CompanyResolverOutput(
    extractedData: Transformed[ExtractedJobData],
    company: Option[Company],
    url: String,
    objectId: Option[String]
)

/**
 * Resolves or creates a company based on AI-extracted company data.
 *
 * This step uses AI to extract the real company website/domain from the job posting
 * content (especially JSON-LD data), rather than using the ATS domain from the URL.
 *
 * Lookup priority:
 * 1. Find by domain (company's actual domain, e.g., "hopper.com")
 * 2. Find by jobsUrl (ATS URL pattern, e.g., "https://jobs.ashbyhq.com/hopper")
 * 3. Create new company if domain is available
 * 4. Return None if AI couldn't extract company website (job will be orphaned)
 */
final class CompanyResolver(supabaseClient: SupabaseClient, aiConfig: AIConfig)
    extends BaseStep[CompanyResolverInput, CompanyResolverOutput]:

  val name = "CompanyResolver"

  private val enrichmentAgent = CompanyEnrichmentAgent(aiConfig)

  override def execute(
      input: CompanyResolverInput,
      ctx: PipelineContext
  ): Either[StepError, CompanyResolverOutput] = {
    logger.info(s"[${ctx.runId}] Resolving company for: ${input.url}")

    val companyName = input.extractedData.value.companyName.getOrElse("Unknown Company")

    // Step 1: Use AI to extract company data (website, domain, jobsUrl)
    val enrichedData = enrichmentAgent.enrich(companyName, input.content, input.url) match
      case Right(data) =>
        logger.info(s"[${ctx.runId}] AI extracted company data: $data")
        logger.info(s"[${ctx.runId}] AI extraction summary: domain=${data.domain}, websiteUrl=${data.websiteUrl}, jobsUrl=${data.jobsUrl}")
        Some(data)
      case Left(error: SchemaError) =>
        val errorMessage: String = error match
          case SchemaError.ModelNotSupported(model, prov, _) => s"Model $model not supported by $prov"
          case SchemaError.NetworkError(msg, _)              => msg
          case SchemaError.ParseError(msg, _)                => msg
          case SchemaError.ApiError(msg, _, _)               => msg
          case SchemaError.SchemaConversionError(msg, _)     => msg
          case SchemaError.ConfigurationError(msg)           => msg
        logger.warn(s"[${ctx.runId}] Company enrichment failed: $errorMessage")
        None

    // Step 2: Try to find or create company
    val company = enrichedData match
      case Some(data) =>
        // Try to find existing company by domain first
        val byDomain = data.domain.flatMap { domain =>
          logger.info(s"[${ctx.runId}] Looking up company by domain: $domain")
          supabaseClient.findCompanyByDomain(domain)
        }

        byDomain match
          case Some(existing) =>
            logger.info(s"[${ctx.runId}] Found existing company by domain: ${existing.companyName.getOrElse("?")} (id=${existing.companyId})")
            Some(existing)

          case None =>
            // Try to find by jobsUrl
            val byJobsUrl = data.jobsUrl.flatMap { jobsUrl =>
              logger.info(s"[${ctx.runId}] Looking up company by jobsUrl: $jobsUrl")
              supabaseClient.findCompanyByJobsUrl(jobsUrl)
            }

            byJobsUrl match
              case Some(existing) =>
                logger.info(s"[${ctx.runId}] Found existing company by jobsUrl: ${existing.companyName.getOrElse("?")} (id=${existing.companyId})")
                Some(existing)

              case None =>
                // Create new company only if we have a domain
                data.domain match
                  case Some(domain) =>
                    logger.info(s"[${ctx.runId}] Creating new company: $companyName (domain=$domain)")
                    val newCompany = Company(
                      companyName = Some(companyName),
                      companyDomain = Some(domain),
                      companyJobsUrl = data.jobsUrl,
                      industry = data.industry,
                      companySize = data.size,
                      description = data.description,
                      headquarters = data.headquarters
                    )
                    supabaseClient.insertCompany(newCompany) match
                      case Some(created) =>
                        logger.info(s"[${ctx.runId}] Created company with id=${created.companyId}")
                        Some(created)
                      case None =>
                        logger.error(s"[${ctx.runId}] Failed to insert company: $companyName")
                        None

                  case None =>
                    logger.warn(s"[${ctx.runId}] Cannot create company - no domain extracted for: $companyName")
                    None

      case None =>
        logger.warn(s"[${ctx.runId}] No enrichment data available, cannot resolve company")
        None

    // Return output (company may be None - job will be orphaned)
    Right(
      CompanyResolverOutput(
        extractedData = input.extractedData,
        company = company,
        url = input.url,
        objectId = input.objectId
      )
    )
  }

object CompanyResolver:
  def apply(supabaseClient: SupabaseClient, aiConfig: AIConfig): CompanyResolver =
    new CompanyResolver(supabaseClient, aiConfig)
