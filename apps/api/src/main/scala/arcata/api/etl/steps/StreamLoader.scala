package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.domain.{Job, JobStreamEntry}
import arcata.api.etl.framework.*

/** Input for the StreamLoader step. */
final case class StreamLoaderInput(
  job: Job,
  profileId: String,
  source: String,
)

/** Output from the StreamLoader step. */
final case class StreamLoaderOutput(
  job: Job,
  streamEntry: JobStreamEntry,
)

/**
 * Loads a job stream entry into the database.
 *
 * This is a load step that adds a job to a user's stream for review.
 */
final class StreamLoader(supabaseClient: SupabaseClient)
  extends BaseStep[StreamLoaderInput, StreamLoaderOutput]:

  val name = "StreamLoader"

  override def execute(
    input: StreamLoaderInput,
    ctx: PipelineContext,
  ): Either[StepError, StreamLoaderOutput] = {
    logger.info(s"[${ctx.runId}] Adding job to stream for profile: ${input.profileId}")

    input.job.jobId match
      case None =>
        Left(
          StepError.ValidationError(
            message = "Job must have an ID before adding to stream",
            stepName = name,
          )
        )

      case Some(jobId) =>
        val streamEntry = JobStreamEntry(
          jobId = jobId,
          profileId = input.profileId,
          source = input.source,
          status = Some("new"),
        )

        supabaseClient.insertJobStreamEntry(streamEntry) match
          case Some(createdEntry) =>
            logger.info(s"[${ctx.runId}] Created stream entry with ID: ${createdEntry.streamId}")
            Right(
              StreamLoaderOutput(
                job = input.job,
                streamEntry = createdEntry,
              )
            )

          case None =>
            Left(
              StepError.LoadError(
                message = s"Failed to create stream entry for job: ${input.job.title}",
                stepName = name,
              )
            )
  }

object StreamLoader:
  def apply(supabaseClient: SupabaseClient): StreamLoader =
    new StreamLoader(supabaseClient)
