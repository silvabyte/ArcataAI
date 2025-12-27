package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.domain.{Company, ExtractedJobData, Job}
import arcata.api.etl.framework.*

/** Input for the JobLoader step. */
final case class JobLoaderInput(
    extractedData: Transformed[ExtractedJobData],
    company: Company,
    url: String,
    objectId: Option[String],
    completionState: Option[String] = None
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
    val data = input.extractedData.value
    logger.info(s"[${ctx.runId}] Loading job: ${data.title}")

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
          title = data.title,
          description = data.description,
          location = data.location,
          jobType = data.jobType,
          experienceLevel = data.experienceLevel,
          educationLevel = data.educationLevel,
          salaryMin = data.salaryMin,
          salaryMax = data.salaryMax,
          salaryCurrency = data.salaryCurrency,
          qualifications = data.qualifications,
          preferredQualifications = data.preferredQualifications,
          responsibilities = data.responsibilities,
          benefits = data.benefits,
          category = data.category,
          sourceUrl = Some(input.url),
          applicationUrl = data.applicationUrl,
          isRemote = data.isRemote,
          postedDate = data.postedDate,
          closingDate = data.closingDate,
          completionState = input.completionState
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
                message = s"Failed to create job: ${data.title}",
                stepName = name
              )
            )
  }

object JobLoader:
  def apply(supabaseClient: SupabaseClient): JobLoader =
    new JobLoader(supabaseClient)
