package arcata.api.etl

import arcata.api.clients.{ObjectStorageClient, SupabaseClient}
import arcata.api.config.BoogieLoopsConfig
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
    boogieLoopsConfig: BoogieLoopsConfig,
    storageClient: Option[ObjectStorageClient] = None
) extends BasePipeline[JobIngestionInput, JobIngestionOutput]:

  val name = "JobIngestionPipeline"

  // Initialize steps
  private val htmlFetcher = HtmlFetcher(storageClient)
  private val jobParser = JobParser(boogieLoopsConfig)
  private val companyResolver = CompanyResolver(supabaseClient)
  private val jobLoader = JobLoader(supabaseClient)
  private val streamLoader = StreamLoader(supabaseClient)
  private val applicationLoader = ApplicationLoader(supabaseClient)

  override def execute(
      input: JobIngestionInput,
      ctx: PipelineContext
  ): Either[StepError, JobIngestionOutput] = {
    for
      // Step 1: Fetch HTML
      fetcherOutput <- htmlFetcher.run(
        HtmlFetcherInput(url = input.url, profileId = input.profileId),
        ctx
      )

      // Step 2: Parse job
      parserOutput <- jobParser.run(
        JobParserInput(
          html = fetcherOutput.html,
          url = fetcherOutput.url,
          objectId = fetcherOutput.objectId
        ),
        ctx
      )

      // Step 3: Resolve company
      companyOutput <- companyResolver.run(
        CompanyResolverInput(
          extractedData = parserOutput.extractedData,
          url = parserOutput.url,
          objectId = parserOutput.objectId
        ),
        ctx
      )

      // Step 4: Load job
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

object JobIngestionPipeline:
  def apply(
      supabaseClient: SupabaseClient,
      boogieLoopsConfig: BoogieLoopsConfig,
      storageClient: Option[ObjectStorageClient] = None
  ): JobIngestionPipeline =
    new JobIngestionPipeline(supabaseClient, boogieLoopsConfig, storageClient)
