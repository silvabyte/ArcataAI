package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.domain.{Company, ExtractedJobData, Job}
import arcata.api.etl.framework.*

/** Input for the JobLoader step. */
final case class JobLoaderInput(
    extractedData: ExtractedJobData,
    company: Company,
    url: String,
    objectId: Option[String]
)

/** Output from the JobLoader step. */
final case class JobLoaderOutput(
    job: Job,
    company: Company
)

/**
 * Loads a job into the database.
 *
 * This is a load step that creates a job record from extracted data.
 */
final class JobLoader(supabaseClient: SupabaseClient)
    extends BaseStep[JobLoaderInput, JobLoaderOutput]:

  val name = "JobLoader"

  override def execute(
      input: JobLoaderInput,
      ctx: PipelineContext
  ): Either[StepError, JobLoaderOutput] = {
    logger.info(s"[${ctx.runId}] Loading job: ${input.extractedData.title}")

    // Check if job already exists by source URL
    supabaseClient.findJobBySourceUrl(input.url) match
      case Some(existingJob) =>
        logger.info(s"[${ctx.runId}] Job already exists with ID: ${existingJob.jobId}")
        Right(
          JobLoaderOutput(
            job = existingJob,
            company = input.company
          )
        )

      case None =>
        val companyId = input.company.companyId.getOrElse {
          return Left(
            StepError.ValidationError(
              message = "Company must have an ID before creating a job",
              stepName = name
            )
          )
        }

        val job = Job(
          companyId = companyId,
          title = input.extractedData.title,
          description = input.extractedData.description,
          location = input.extractedData.location,
          jobType = input.extractedData.jobType,
          experienceLevel = input.extractedData.experienceLevel,
          educationLevel = input.extractedData.educationLevel,
          salaryMin = input.extractedData.salaryMin,
          salaryMax = input.extractedData.salaryMax,
          salaryCurrency = input.extractedData.salaryCurrency,
          qualifications = input.extractedData.qualifications,
          preferredQualifications = input.extractedData.preferredQualifications,
          responsibilities = input.extractedData.responsibilities,
          benefits = input.extractedData.benefits,
          category = input.extractedData.category,
          sourceUrl = Some(input.url),
          applicationUrl = input.extractedData.applicationUrl,
          isRemote = input.extractedData.isRemote,
          postedDate = input.extractedData.postedDate,
          closingDate = input.extractedData.closingDate
        )

        supabaseClient.insertJob(job) match
          case Some(createdJob) =>
            logger.info(s"[${ctx.runId}] Created job with ID: ${createdJob.jobId}")
            Right(
              JobLoaderOutput(
                job = createdJob,
                company = input.company
              )
            )

          case None =>
            Left(
              StepError.LoadError(
                message = s"Failed to create job: ${input.extractedData.title}",
                stepName = name
              )
            )
  }

object JobLoader:
  def apply(supabaseClient: SupabaseClient): JobLoader =
    new JobLoader(supabaseClient)
