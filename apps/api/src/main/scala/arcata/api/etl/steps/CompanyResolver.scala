package arcata.api.etl.steps

import arcata.api.ai.CompanyEnrichmentAgent
import arcata.api.clients.SupabaseClient
import arcata.api.config.AIConfig
import arcata.api.domain.{Company, ExtractedJobData}
import arcata.api.etl.framework.*
import boogieloops.ai.SchemaError

import java.net.URI
import scala.util.Try

/** Input for the CompanyResolver step. */
final case class CompanyResolverInput(
    extractedData: ExtractedJobData,
    url: String,
    objectId: Option[String],
    html: String
)

/** Output from the CompanyResolver step. */
final case class CompanyResolverOutput(
    extractedData: ExtractedJobData,
    company: Company,
    url: String,
    objectId: Option[String]
)

/**
 * Resolves or creates a company based on the job URL and extracted data.
 *
 * This is a transformation step that looks up existing companies by domain or creates new ones.
 * For new companies, uses AI enrichment to extract additional company information.
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

    // Extract domain from URL
    val domain = extractDomain(input.url) match
      case Some(d) => d
      case None =>
        return Left(
          StepError.ValidationError(
            message = s"Could not extract domain from URL: ${input.url}",
            stepName = name
          )
        )

    // Try to find existing company by domain
    supabaseClient.findCompanyByDomain(domain) match
      case Some(existingCompany) =>
        logger.info(
          s"[${ctx.runId}] Found existing company: ${existingCompany.companyName.getOrElse(domain)}"
        )
        Right(
          CompanyResolverOutput(
            extractedData = input.extractedData,
            company = existingCompany,
            url = input.url,
            objectId = input.objectId
          )
        )

      case None =>
        // Enrich company data using AI before creating
        val companyName = input.extractedData.companyName.getOrElse(domain)

        val enrichedData = enrichmentAgent.enrich(companyName, input.html, input.url) match
          case Right(data) =>
            logger.info(s"[${ctx.runId}] Successfully enriched company data for: $companyName")
            Some(data)
          case Left(error: SchemaError) =>
            val errorMessage: String = error match
              case SchemaError.ModelNotSupported(model, prov, _) => s"Model $model not supported by $prov"
              case SchemaError.NetworkError(msg, _) => msg
              case SchemaError.ParseError(msg, _) => msg
              case SchemaError.ApiError(msg, _, _) => msg
              case SchemaError.SchemaConversionError(msg, _) => msg
              case SchemaError.ConfigurationError(msg) => msg
            logger.warn(s"[${ctx.runId}] Company enrichment failed: $errorMessage, proceeding without enrichment")
            None

        // Create company with enriched data (or basic data if enrichment failed)
        val newCompany = Company(
          companyName = Some(companyName),
          companyDomain = Some(domain),
          industry = enrichedData.flatMap(_.industry),
          companySize = enrichedData.flatMap(_.size),
          description = enrichedData.flatMap(_.description),
          headquarters = enrichedData.flatMap(_.headquarters)
        )

        supabaseClient.insertCompany(newCompany) match
          case Some(createdCompany) =>
            logger.info(
              s"[${ctx.runId}] Created new company: ${createdCompany.companyName.getOrElse(domain)}"
            )
            Right(
              CompanyResolverOutput(
                extractedData = input.extractedData,
                company = createdCompany,
                url = input.url,
                objectId = input.objectId
              )
            )

          case None =>
            Left(
              StepError.LoadError(
                message = s"Failed to create company for domain: $domain",
                stepName = name
              )
            )
  }

  /** Extract the root domain from a URL. */
  private def extractDomain(url: String): Option[String] = {
    Try {
      val uri = new URI(url)
      val host = uri.getHost
      if host == null then None
      else {
        // Remove www. prefix if present
        val cleanHost = if host.startsWith("www.") then host.drop(4) else host
        Some(cleanHost.toLowerCase)
      }
    }.toOption.flatten
  }

object CompanyResolver:
  def apply(supabaseClient: SupabaseClient, aiConfig: AIConfig): CompanyResolver =
    new CompanyResolver(supabaseClient, aiConfig)
