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
    notes: Option[String] = None,
    skipStream: Boolean = false
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
 * 2. Clean HTML to markdown (strips bloat, preserves JSON-LD)
 * 3. Extract job data using AI-powered extraction
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
  private val jobExtractor = JobExtractor(aiConfig)
  private val htmlCleaner = HtmlCleaner()
  private val jobTransformer = JobTransformer
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
    // For service calls (skipStream=true), just return the existing job
    if input.skipStream then
      logger.info(s"[${ctx.runId}] Service call - returning existing job without stream/application")
      progressEmitter.emit(1, 1, "complete", "Job already exists")
      return Right(JobIngestionOutput(job = job, streamEntry = None, application = None))

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
    // For service calls (skipStream=true), we only do steps 1-6 (fetch, clean, extract, transform, resolve, load)
    val baseSteps = 6
    val totalSteps = {
      if input.skipStream then baseSteps
      else if input.createApplication then baseSteps + 3 // +stream +app
      else baseSteps + 2 // +stream
    }

    progressEmitter.emit(1, totalSteps, "fetching", "Getting job page...")

    val result = {
      for
        // Step 1: Fetch HTML (raw HTML stored in ObjectStorage)
        fetcherOutput <- htmlFetcher.run(
          HtmlFetcherInput(url = input.url, profileId = input.profileId),
          ctx
        )

        // Step 2: Clean HTML to markdown
        cleanerOutput <- {
          progressEmitter.emit(2, totalSteps, "cleaning", "Processing content...")
          htmlCleaner.run(
            HtmlCleanerInput(
              html = fetcherOutput.html,
              url = fetcherOutput.url,
              objectId = fetcherOutput.objectId
            ),
            ctx
          )
        }

        // Step 3: Extract job data using AI (now uses cleaned markdown)
        extractorOutput <- {
          progressEmitter.emit(3, totalSteps, "extracting", "Extracting job details...")
          jobExtractor.run(
            JobExtractorInput(
              content = cleanerOutput.markdown,
              url = cleanerOutput.url,
              objectId = cleanerOutput.objectId
            ),
            ctx
          )
        }

        // Step 4: Transform/sanitize job data
        transformerOutput <- {
          progressEmitter.emit(4, totalSteps, "transforming", "Sanitizing job data...")
          jobTransformer.run(
            JobTransformerInput(
              extracted = extractorOutput.extractedData,
              sourceUrl = extractorOutput.url,
              objectId = extractorOutput.objectId,
              completionState = extractorOutput.completionState
            ),
            ctx
          )
        }

        // Step 5: Resolve company (uses markdown for AI enrichment)
        companyOutput <- {
          progressEmitter.emit(5, totalSteps, "resolving", "Finding company...")
          companyResolver.run(
            CompanyResolverInput(
              extractedData = transformerOutput.transformed,
              url = transformerOutput.sourceUrl,
              objectId = transformerOutput.objectId,
              content = cleanerOutput.markdown
            ),
            ctx
          )
        }

        // Step 6: Load job (company is Option[Company] from CompanyResolver)
        jobOutput <- {
          progressEmitter.emit(6, totalSteps, "loading", "Creating job record...")
          jobLoader.run(
            JobLoaderInput(
              extractedData = companyOutput.extractedData,
              company = companyOutput.company, // Option[Company] - JobLoader validates this
              url = companyOutput.url,
              objectId = companyOutput.objectId,
              completionState = Some(transformerOutput.completionState.toString)
            ),
            ctx
          )
        }

        // For service calls, skip stream and application - just return the job
        finalOutput <-
          if input.skipStream then
            logger.info(s"[${ctx.runId}] Service call - skipping stream and application creation")
            Right(JobIngestionOutput(job = jobOutput.job, streamEntry = None, application = None))
          else {
            // Step 7: Add to stream
            for
              streamOutput <- {
                progressEmitter.emit(7, totalSteps, "streaming", "Adding to your feed...")
                streamLoader.run(
                  StreamLoaderInput(
                    job = jobOutput.job,
                    profileId = input.profileId,
                    source = input.source
                  ),
                  ctx
                )
              }

              // Step 8: Optionally create application
              applicationOutput <-
                if input.createApplication then
                  progressEmitter.emit(8, totalSteps, "tracking", "Creating application...")
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
      yield finalOutput
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
