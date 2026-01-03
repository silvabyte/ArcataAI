package arcata.api.etl.greenhouse

import arcata.api.clients.SupabaseClient
import arcata.api.domain.{Company, Job, JobStreamEntry}
import arcata.api.etl.framework.*
import arcata.api.etl.steps.*

/** Input for the GreenhouseIngestionPipeline. */
final case class GreenhouseIngestionInput(
    apiUrl: String,
    sourceUrl: String,
    companyId: Option[Long],
    profileId: String,
    source: String
)

/** Output from the GreenhouseIngestionPipeline. */
final case class GreenhouseIngestionOutput(
    job: Job,
    streamEntry: Option[JobStreamEntry]
)

/**
 * Optimized pipeline for ingesting Greenhouse jobs without AI extraction.
 *
 * This pipeline fetches structured data directly from the Greenhouse Job Board API and parses it
 * into our domain model. This eliminates the need for AI extraction, making it:
 *   - Faster (no LLM API call)
 *   - Cheaper (no token costs)
 *   - More reliable (structured data vs. AI interpretation)
 *
 * Pipeline steps:
 *   1. GreenhouseJobFetcher - GET API with ?pay_transparency=true
 *   2. GreenhouseJobParser - Parse JSON -> ExtractedJobData
 *   3. JobTransformer - Sanitize/normalize data
 *   4. JobLoader - Create job record (with known companyId)
 *   5. StreamLoader - Add to user's job stream
 *
 * Skipped steps (compared to JobIngestionPipeline):
 *   - HtmlFetcher (we fetch JSON, not HTML)
 *   - HtmlCleaner (no HTML to clean)
 *   - JobExtractor (no AI needed - structured data!)
 *   - CompanyResolver (company already known from discovery)
 */
final class GreenhouseIngestionPipeline(
    supabaseClient: SupabaseClient,
    progressEmitter: ProgressEmitter = ProgressEmitter.noop
) extends BasePipeline[GreenhouseIngestionInput, GreenhouseIngestionOutput]:

  val name = "GreenhouseIngestionPipeline"

  // Initialize steps
  private val fetcher = GreenhouseJobFetcher()
  private val parser = GreenhouseJobParser
  private val transformer = JobTransformer
  private val jobLoader = JobLoader(supabaseClient)
  private val streamLoader = StreamLoader(supabaseClient)

  override def execute(
      input: GreenhouseIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, GreenhouseIngestionOutput] = {
    // Step 0: Check if job already exists
    progressEmitter.emit(0, 1, "checking", "Looking up job...")
    supabaseClient.findJobBySourceUrl(input.sourceUrl) match
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
      input: GreenhouseIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, GreenhouseIngestionOutput] = {
    val totalSteps = 2

    progressEmitter.emit(0, totalSteps, "checking", "Job found!")
    progressEmitter.emit(1, totalSteps, "streaming", "Adding to feed...")

    val result = streamLoader.run(
      StreamLoaderInput(
        job = job,
        profileId = input.profileId,
        source = input.source
      ),
      ctx
    ).map { streamOutput =>
      GreenhouseIngestionOutput(
        job = job,
        streamEntry = Some(streamOutput.streamEntry)
      )
    }

    result.foreach(_ => progressEmitter.emit(totalSteps, totalSteps, "complete", "Job added"))
    result
  }

  private def handleNewJob(
      input: GreenhouseIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, GreenhouseIngestionOutput] = {
    val totalSteps = 5

    progressEmitter.emit(1, totalSteps, "fetching", "Getting job from Greenhouse API...")

    for
      // Step 1: Fetch JSON from Greenhouse API
      fetcherOutput <- fetcher.run(
        GreenhouseJobFetcherInput(
          apiUrl = input.apiUrl,
          sourceUrl = input.sourceUrl,
          companyId = input.companyId
        ),
        ctx
      )

      // Step 2: Parse JSON to ExtractedJobData
      parserOutput <- {
        progressEmitter.emit(2, totalSteps, "parsing", "Parsing job data...")
        parser.run(
          GreenhouseJobParserInput(
            json = fetcherOutput.json,
            apiUrl = fetcherOutput.apiUrl,
            sourceUrl = fetcherOutput.sourceUrl,
            companyId = fetcherOutput.companyId
          ),
          ctx
        )
      }

      // Step 3: Transform/sanitize job data
      transformerOutput <- {
        progressEmitter.emit(3, totalSteps, "transforming", "Sanitizing data...")
        transformer.run(
          JobTransformerInput(
            extracted = parserOutput.extracted,
            sourceUrl = parserOutput.sourceUrl,
            objectId = None,
            completionState = parserOutput.completionState
          ),
          ctx
        )
      }

      // Step 4: Load job into database
      // Create a minimal Company wrapper with just the companyId
      // JobLoader only uses company.flatMap(_.companyId) anyway
      company = parserOutput.companyId.map(id => Company(companyId = Some(id)))
      jobOutput <- {
        progressEmitter.emit(4, totalSteps, "loading", "Creating job record...")
        jobLoader.run(
          JobLoaderInput(
            extractedData = transformerOutput.transformed,
            company = company,
            url = transformerOutput.sourceUrl,
            objectId = None,
            completionState = Some(transformerOutput.completionState.toString)
          ),
          ctx
        )
      }

      // Step 5: Add to stream
      streamOutput <- {
        progressEmitter.emit(5, totalSteps, "streaming", "Adding to feed...")
        streamLoader.run(
          StreamLoaderInput(
            job = jobOutput.job,
            profileId = input.profileId,
            source = input.source
          ),
          ctx
        )
      }
    yield {
      progressEmitter.emit(totalSteps, totalSteps, "complete", "Job added successfully")
      GreenhouseIngestionOutput(
        job = streamOutput.job,
        streamEntry = Some(streamOutput.streamEntry)
      )
    }
  }

object GreenhouseIngestionPipeline:
  def apply(
      supabaseClient: SupabaseClient,
      progressEmitter: ProgressEmitter = ProgressEmitter.noop
  ): GreenhouseIngestionPipeline =
    new GreenhouseIngestionPipeline(supabaseClient, progressEmitter)
