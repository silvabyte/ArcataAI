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
 * Steps:
 * 1. Fetch HTML from URL
 * 2. Extract job data using config-driven extraction (with AI fallback)
 * 3. Clean HTML to markdown (for company enrichment only)
 * 4. Resolve/create company
 * 5. Load job into database
 * 6. Add to user's job stream
 * 7. Optionally create an application
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
  private val jobExtractor = JobExtractor(supabaseClient, aiConfig)
  private val htmlCleaner = HtmlCleaner() // Still needed for company enrichment
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
    progressEmitter.emit(1, totalSteps, "streaming", "Adding to your feed...")

    val result = {
      for
        streamOutput <- streamLoader.run(
          StreamLoaderInput(
            job = job,
            profileId = input.profileId,
            source = input.source
          ),
          ctx
        )

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
      yield JobIngestionOutput(
        job = job,
        streamEntry = Some(streamOutput.streamEntry),
        application = applicationOutput
      )
    }

    result.foreach(_ => progressEmitter.emit(totalSteps, totalSteps, "complete", "Job added successfully"))
    result
  }

  private def handleNewJob(
      input: JobIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, JobIngestionOutput] = {
    val totalSteps = if input.createApplication then 8 else 7

    progressEmitter.emit(1, totalSteps, "fetching", "Getting job page...")

    val result = {
      for
        // Step 1: Fetch HTML (raw HTML stored in ObjectStorage)
        fetcherOutput <- htmlFetcher.run(
          HtmlFetcherInput(url = input.url, profileId = input.profileId),
          ctx
        )

        // Step 2: Extract job data using config-driven extraction
        extractorOutput <- {
          progressEmitter.emit(2, totalSteps, "extracting", "Extracting job details...")
          jobExtractor.run(
            JobExtractorInput(
              html = fetcherOutput.html,
              url = fetcherOutput.url,
              objectId = fetcherOutput.objectId
            ),
            ctx
          )
        }

        // Step 3: Clean HTML to markdown (for company enrichment only)
        cleanerOutput <- {
          progressEmitter.emit(3, totalSteps, "cleaning", "Processing content...")
          htmlCleaner.run(
            HtmlCleanerInput(
              html = fetcherOutput.html,
              url = fetcherOutput.url,
              objectId = fetcherOutput.objectId
            ),
            ctx
          )
        }

        // Step 4: Resolve company (uses markdown for AI enrichment)
        companyOutput <- {
          progressEmitter.emit(4, totalSteps, "resolving", "Finding company...")
          companyResolver.run(
            CompanyResolverInput(
              extractedData = extractorOutput.extractedData,
              url = extractorOutput.url,
              objectId = extractorOutput.objectId,
              content = cleanerOutput.markdown
            ),
            ctx
          )
        }

        // Step 5: Load job
        jobOutput <- {
          progressEmitter.emit(5, totalSteps, "loading", "Creating job record...")
          jobLoader.run(
            JobLoaderInput(
              extractedData = companyOutput.extractedData,
              company = companyOutput.company,
              url = companyOutput.url,
              objectId = companyOutput.objectId,
              completionState = Some(extractorOutput.completionState.toString)
            ),
            ctx
          )
        }

        // Step 6: Add to stream
        streamOutput <- {
          progressEmitter.emit(6, totalSteps, "streaming", "Adding to your feed...")
          streamLoader.run(
            StreamLoaderInput(
              job = jobOutput.job,
              profileId = input.profileId,
              source = input.source
            ),
            ctx
          )
        }

        // Step 7: Optionally create application
        applicationOutput <-
          if input.createApplication then
            progressEmitter.emit(7, totalSteps, "tracking", "Creating application...")
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
      yield JobIngestionOutput(
        job = streamOutput.job,
        streamEntry = Some(streamOutput.streamEntry),
        application = applicationOutput
      )
    }

    result.foreach(_ => progressEmitter.emit(totalSteps, totalSteps, "complete", "Job added successfully"))
    result
  }

object JobIngestionPipeline:
  def apply(
      supabaseClient: SupabaseClient,
      aiConfig: AIConfig,
      storageClient: Option[ObjectStorageClient] = None,
      progressEmitter: ProgressEmitter = ProgressEmitter.noop
  ): JobIngestionPipeline =
    new JobIngestionPipeline(supabaseClient, aiConfig, storageClient, progressEmitter)
