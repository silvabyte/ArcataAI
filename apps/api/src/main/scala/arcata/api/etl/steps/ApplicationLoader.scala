package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.domain.{Job, JobApplication}
import arcata.api.etl.framework.*

import java.time.LocalDate

/** Input for the ApplicationLoader step. */
final case class ApplicationLoaderInput(
  job: Job,
  profileId: String,
  notes: Option[String] = None,
)

/** Output from the ApplicationLoader step. */
final case class ApplicationLoaderOutput(
  job: Job,
  application: JobApplication,
)

/**
 * Loads a job application into the database.
 *
 * This is a load step that creates an application record when a user applies to a job.
 */
final class ApplicationLoader(supabaseClient: SupabaseClient)
  extends BaseStep[ApplicationLoaderInput, ApplicationLoaderOutput]:

  val name = "ApplicationLoader"

  override def execute(
    input: ApplicationLoaderInput,
    ctx: PipelineContext,
  ): Either[StepError, ApplicationLoaderOutput] = {
    logger.info(s"[${ctx.runId}] Creating application for profile: ${input.profileId}")

    input.job.jobId match
      case None =>
        Left(
          StepError.ValidationError(
            message = "Job must have an ID before creating application",
            stepName = name,
          )
        )

      case Some(jobId) =>
        // Get the default status for this user
        val statusId = supabaseClient.getDefaultStatusId(input.profileId)

        val application = JobApplication(
          jobId = Some(jobId),
          profileId = input.profileId,
          statusId = statusId,
          statusOrder = 0,
          applicationDate = Some(LocalDate.now().toString),
          notes = input.notes,
        )

        supabaseClient.insertJobApplication(application) match
          case Some(createdApp) =>
            logger.info(s"[${ctx.runId}] Created application with ID: ${createdApp.applicationId}")
            Right(
              ApplicationLoaderOutput(
                job = input.job,
                application = createdApp,
              )
            )

          case None =>
            Left(
              StepError.LoadError(
                message = s"Failed to create application for job: ${input.job.title}",
                stepName = name,
              )
            )
  }

object ApplicationLoader:
  def apply(supabaseClient: SupabaseClient): ApplicationLoader =
    new ApplicationLoader(supabaseClient)
