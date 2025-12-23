package arcata.api.etl

import arcata.api.clients.{ObjectStorageClient, SupabaseClient}
import arcata.api.config.AIConfig
import arcata.api.domain.{Job, JobApplication, JobStreamEntry}
import arcata.api.etl.framework.*
import arcata.api.etl.steps.*

/** Input for the job ingestion pipeline. */
final case class JobIngestionInput(
    url: String,
    profileId: String,
    source: String = "manual",
    createApplication: Boolean = false,
    notes: Option[String] = None
)

/** Output from the job ingestion pipeline. */
final case class JobIngestionOutput(
    job: Job,
    streamEntry: Option[JobStreamEntry],
    application: Option[JobApplication]
)

/**
 * Pipeline that ingests a job from a URL.
 *
 * Steps: 1. Fetch HTML from URL 2. Parse job details using AI 3. Resolve/create company 4. Load
 * job into database 5. Add to user's job stream 6. Optionally create an application
 */
final class JobIngestionPipeline(
    supabaseClient: SupabaseClient,
    aiConfig: AIConfig,
    storageClient: Option[ObjectStorageClient] = None,
    progressEmitter: ProgressEmitter = ProgressEmitter.noop
) extends BasePipeline[JobIngestionInput, JobIngestionOutput]:

  val name = "JobIngestionPipeline"

  // Initialize steps
  private val htmlFetcher = HtmlFetcher(storageClient)
  private val jobParser = JobParser(aiConfig)
  private val companyResolver = CompanyResolver(supabaseClient, aiConfig)
  private val jobLoader = JobLoader(supabaseClient)
  private val streamLoader = StreamLoader(supabaseClient)
  private val applicationLoader = ApplicationLoader(supabaseClient)

  override def execute(
      input: JobIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, JobIngestionOutput] = {
    // Step 0: Check if job already exists
    progressEmitter.emit(0, 1, "checking", "Looking up job...")
    supabaseClient.findJobBySourceUrl(input.url) match
      case Some(existingJob) =>
        logger.info(s"[${ctx.runId}] Job already exists (id=${existingJob.jobId}), skipping to stream")
        handleExistingJob(existingJob, input, ctx) match
          case Left(err) =>
            progressEmitter.emit(0, 1, "error", err.message)
            Left(err)
          case result => result
      case None =>
        handleNewJob(input, ctx) match
          case Left(err) =>
            progressEmitter.emit(0, 1, "error", err.message)
            Left(err)
          case result => result
  }

  private def handleExistingJob(
      job: Job,
      input: JobIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, JobIngestionOutput] = {
    val totalSteps = if input.createApplication then 3 else 2

    progressEmitter.emit(0, totalSteps, "checking", "Job found!")

    for
      // Skip directly to adding to stream
      _ = progressEmitter.emit(1, totalSteps, "streaming", "Adding to your feed...")
      streamOutput <- streamLoader.run(
        StreamLoaderInput(
          job = job,
          profileId = input.profileId,
          source = input.source
        ),
        ctx
      )

      // Optionally create application
      applicationOutput <-
        if input.createApplication then
          progressEmitter.emit(2, totalSteps, "tracking", "Creating application...")
          applicationLoader
            .run(
              ApplicationLoaderInput(
                job = job,
                profileId = input.profileId,
                notes = input.notes
              ),
              ctx
            )
            .map(out => Some(out.application))
        else Right(None)

      _ = progressEmitter.emit(totalSteps, totalSteps, "complete", "Job added successfully")
    yield JobIngestionOutput(
      job = job,
      streamEntry = Some(streamOutput.streamEntry),
      application = applicationOutput
    )
  }

  private def handleNewJob(
      input: JobIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, JobIngestionOutput] = {
    val totalSteps = if input.createApplication then 7 else 6

    for
      // Step 1: Fetch HTML
      _ = progressEmitter.emit(1, totalSteps, "fetching", "Getting job page...")
      fetcherOutput <- htmlFetcher.run(
        HtmlFetcherInput(url = input.url, profileId = input.profileId),
        ctx
      )

      // Step 2: Parse job
      _ = progressEmitter.emit(2, totalSteps, "parsing", "Extracting job details...")
      parserOutput <- jobParser.run(
        JobParserInput(
          html = fetcherOutput.html,
          url = fetcherOutput.url,
          objectId = fetcherOutput.objectId
        ),
        ctx
      )

      // Step 3: Resolve company
      _ = progressEmitter.emit(3, totalSteps, "resolving", "Finding company...")
      companyOutput <- companyResolver.run(
        CompanyResolverInput(
          extractedData = parserOutput.extractedData,
          url = parserOutput.url,
          objectId = parserOutput.objectId,
          html = fetcherOutput.html
        ),
        ctx
      )

      // Step 4: Load job
      _ = progressEmitter.emit(4, totalSteps, "loading", "Creating job record...")
      jobOutput <- jobLoader.run(
        JobLoaderInput(
          extractedData = companyOutput.extractedData,
          company = companyOutput.company,
          url = companyOutput.url,
          objectId = companyOutput.objectId
        ),
        ctx
      )

      // Step 5: Add to stream
      _ = progressEmitter.emit(5, totalSteps, "streaming", "Adding to your feed...")
      streamOutput <- streamLoader.run(
        StreamLoaderInput(
          job = jobOutput.job,
          profileId = input.profileId,
          source = input.source
        ),
        ctx
      )

      // Step 6: Optionally create application
      applicationOutput <-
        if input.createApplication then
          progressEmitter.emit(6, totalSteps, "tracking", "Creating application...")
          applicationLoader
            .run(
              ApplicationLoaderInput(
                job = jobOutput.job,
                profileId = input.profileId,
                notes = input.notes
              ),
              ctx
            )
            .map(out => Some(out.application))
        else Right(None)

      _ = progressEmitter.emit(totalSteps, totalSteps, "complete", "Job added successfully")
    yield JobIngestionOutput(
      job = streamOutput.job,
      streamEntry = Some(streamOutput.streamEntry),
      application = applicationOutput
    )
  }

object JobIngestionPipeline:
  def apply(
      supabaseClient: SupabaseClient,
      aiConfig: AIConfig,
      storageClient: Option[ObjectStorageClient] = None,
      progressEmitter: ProgressEmitter = ProgressEmitter.noop
  ): JobIngestionPipeline =
    new JobIngestionPipeline(supabaseClient, aiConfig, storageClient, progressEmitter)
