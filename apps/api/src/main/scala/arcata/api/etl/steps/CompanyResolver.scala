package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.domain.{Company, ExtractedJobData}
import arcata.api.etl.framework.*

import java.net.URI
import scala.util.Try

/** Input for the CompanyResolver step. */
final case class CompanyResolverInput(
    extractedData: ExtractedJobData,
    url: String,
    objectId: Option[String]
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
 */
final class CompanyResolver(supabaseClient: SupabaseClient)
    extends BaseStep[CompanyResolverInput, CompanyResolverOutput]:

  val name = "CompanyResolver"

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
        // Create new company
        val newCompany = Company(
          companyName = input.extractedData.companyName,
          companyDomain = Some(domain)
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
  def apply(supabaseClient: SupabaseClient): CompanyResolver =
    new CompanyResolver(supabaseClient)
