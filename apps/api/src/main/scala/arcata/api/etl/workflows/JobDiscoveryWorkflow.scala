package arcata.api.etl.workflows

import arcata.api.clients.{ObjectStorageClient, SupabaseClient}
import arcata.api.config.AIConfig
import arcata.api.etl.{JobIngestionInput, JobIngestionPipeline}
import arcata.api.etl.framework.*
import arcata.api.etl.greenhouse.{GreenhouseIngestionInput, GreenhouseIngestionPipeline}
import arcata.api.etl.sources.{DiscoveredJob, JobSource}

/**
 * Input for JobDiscoveryWorkflow.
 *
 * @param sourceId
 *   Optional source ID to filter to a specific source (e.g., "greenhouse"). If None, all
 *   registered sources are processed.
 */
case class JobDiscoveryInput(
  sourceId: Option[String] = None
)

/**
 * Output from JobDiscoveryWorkflow.
 *
 * @param totalDiscovered
 *   Total jobs discovered across all sources
 * @param totalIngested
 *   Jobs successfully ingested (new or updated)
 * @param totalSkipped
 *   Jobs skipped (already existed)
 * @param totalFailed
 *   Jobs that failed to ingest
 * @param bySource
 *   Breakdown of discovered jobs per source
 */
case class JobDiscoveryOutput(
  totalDiscovered: Int,
  totalIngested: Int,
  totalSkipped: Int,
  totalFailed: Int,
  bySource: Map[String, Int],
)

/**
 * Async workflow that discovers new jobs from registered sources.
 *
 * This workflow:
 *   1. Iterates through all sources in JobSourceRegistry (or a specific source if filtered)
 *   2. Calls each source's discoverJobs() method to get job URLs
 *   3. Routes each job to the appropriate pipeline based on source type:
 *      - Greenhouse with API URL -> GreenhouseIngestionPipeline (no AI, structured data)
 *      - Other sources -> JobIngestionPipeline (AI-powered extraction)
 *   4. Respects rate limiting delays between requests
 *
 * The pipeline handles deduplication - if a job URL already exists, it's skipped. Jobs are added
 * to the system-level `jobs` table, not to any specific user's stream.
 *
 * Triggered via HTTP POST to /api/v1/cron/job-discovery, runs in background. The endpoint returns
 * 202 Accepted immediately; actual processing happens asynchronously.
 *
 * Usage:
 * {{{
 * // In ApiApp or routes
 * given actorContext: castor.Context = ...
 * val workflow = JobDiscoveryWorkflow(supabaseClient, aiConfig)
 *
 * // Trigger all sources (fire-and-forget)
 * workflow.send(WorkflowRun(JobDiscoveryInput(), "system"))
 *
 * // Trigger specific source
 * workflow.send(WorkflowRun(JobDiscoveryInput(Some("greenhouse")), "system"))
 * }}}
 */
class JobDiscoveryWorkflow(
  supabaseClient: SupabaseClient,
  aiConfig: AIConfig,
  storageClient: Option[ObjectStorageClient] = None,
)(
  using
  ac: castor.Context
) extends BaseWorkflow[JobDiscoveryInput, JobDiscoveryOutput]:

  val name = "JobDiscoveryWorkflow"

  // AI-powered pipeline for unknown sources or jobs without API URLs
  private val aiIngestionPipeline = JobIngestionPipeline(
    supabaseClient,
    aiConfig,
    storageClient,
  )

  // Optimized pipeline for Greenhouse jobs (no AI, uses structured API data)
  private val greenhouseIngestionPipeline = GreenhouseIngestionPipeline(supabaseClient)

  override def execute(
    input: JobDiscoveryInput,
    ctx: PipelineContext,
  ): Either[StepError, JobDiscoveryOutput] = {
    // Get sources to process
    val sources = input.sourceId match
      case Some(id) =>
        JobSource.get(id) match
          case Some(source) => Seq(source)
          case None =>
            logger.warn(s"[${ctx.runId}] Unknown source ID: $id")
            Seq.empty
      case None => JobSource.all

    if sources.isEmpty then Right(JobDiscoveryOutput(0, 0, 0, 0, Map.empty))
    else
      // Process all sources and aggregate results
      val sourceResults = sources.map {
        source =>
          logger.info(s"[${ctx.runId}] Processing source: ${source.name} (${source.sourceId})")
          processSource(source, ctx)
      }

      // Aggregate results from all sources
      val totalDiscovered = sourceResults.map(_.discovered).sum
      val totalIngested = sourceResults.map(_.ingested).sum
      val totalSkipped = sourceResults.map(_.skipped).sum
      val totalFailed = sourceResults.map(_.failed).sum
      val bySource = sourceResults.map(r => r.sourceId -> r.discovered).toMap

      Right(
        JobDiscoveryOutput(
          totalDiscovered = totalDiscovered,
          totalIngested = totalIngested,
          totalSkipped = totalSkipped,
          totalFailed = totalFailed,
          bySource = bySource,
        )
      )
  }

  /** Result from processing a single source. */
  private case class SourceResult(
    sourceId: String,
    discovered: Int,
    ingested: Int,
    skipped: Int,
    failed: Int,
  )

  /** Process a single job source and return aggregated results. */
  private def processSource(
    source: JobSource,
    ctx: PipelineContext,
  ): SourceResult = {
    val jobs = source.discoverJobs(supabaseClient)
    logger.info(s"[${ctx.runId}] Discovered ${jobs.size} jobs from ${source.name}")

    // Process each job and collect results
    val jobResults = jobs.zipWithIndex.map {
      case (job, idx) =>
        // Rate limiting delay (skip on first job)
        if idx > 0 && source.config.delayBetweenRequestsMs > 0 then
          Thread.sleep(source.config.delayBetweenRequestsMs)

        logger.debug(s"[${ctx.runId}] Processing job ${idx + 1}/${jobs.size}: ${job.url}")
        processJob(job, source.sourceId, ctx)
    }

    SourceResult(
      sourceId = source.sourceId,
      discovered = jobs.size,
      ingested = jobResults.count(_ == JobResult.Ingested),
      skipped = jobResults.count(_ == JobResult.Skipped),
      failed = jobResults.count(_ == JobResult.Failed),
    )
  }

  /** Result of processing a single job. */
  private enum JobResult:
    case Ingested, Skipped, Failed

  /**
   * Process a single job through the appropriate pipeline.
   *
   * Routes jobs based on source type:
   *   - Greenhouse with API URL -> GreenhouseIngestionPipeline (optimized, no AI)
   *   - Other sources or missing API URL -> JobIngestionPipeline (AI-powered)
   */
  private def processJob(
    job: DiscoveredJob,
    sourceId: String,
    ctx: PipelineContext,
  ): JobResult = {
    // Route to appropriate pipeline based on source and available data
    val result = (job.source, job.apiUrl) match {
      case (JobSource.Greenhouse, Some(apiUrl)) =>
        // Use optimized Greenhouse pipeline (no AI needed)
        logger.debug(s"[${ctx.runId}] Using Greenhouse pipeline for: ${job.url}")
        greenhouseIngestionPipeline.run(
          GreenhouseIngestionInput(
            apiUrl = apiUrl,
            sourceUrl = job.url,
            companyId = job.companyId,
            profileId = "system",
            source = s"discovery:$sourceId",
          ),
          "system",
        )

      case _ =>
        // Fall back to AI-powered pipeline
        logger.debug(s"[${ctx.runId}] Using AI pipeline for: ${job.url}")
        aiIngestionPipeline.run(
          JobIngestionInput(
            url = job.url,
            profileId = "system",
            source = s"discovery:$sourceId",
            createApplication = false,
          ),
          "system",
        )
    }

    if result.isSuccess then
      logger.debug(s"[${ctx.runId}] Successfully ingested: ${job.url}")
      JobResult.Ingested
    else {
      result.error match
        case Some(err) if isAlreadyExistsError(err) =>
          logger.debug(s"[${ctx.runId}] Skipped (already exists): ${job.url}")
          JobResult.Skipped
        case Some(err) =>
          logger.warn(s"[${ctx.runId}] Failed to ingest ${job.url}: ${err.message}")
          JobResult.Failed
        case None =>
          JobResult.Ingested
    }
  }

  /**
   * Check if an error indicates the job already exists.
   *
   * The JobIngestionPipeline handles existing jobs gracefully - it adds them to the stream without
   * re-creating. For system-level discovery, this is effectively a "skip".
   */
  private def isAlreadyExistsError(error: StepError): Boolean = {
    error.message.toLowerCase.contains("already exists") ||
    error.message.toLowerCase.contains("duplicate")
  }

  override def onSuccess(result: PipelineResult[JobDiscoveryOutput]): Unit = {
    result.output.foreach {
      out =>
        val sourceBreakdown = out.bySource.map { case (k, v) => s"$k=$v" }.mkString(", ")
        logger.info(
          s"[$name] Completed in ${result.durationMs}ms: " +
            s"discovered=${out.totalDiscovered}, ingested=${out.totalIngested}, " +
            s"skipped=${out.totalSkipped}, failed=${out.totalFailed} " +
            s"[by source: $sourceBreakdown]"
        )
    }
  }

  override def onFailure(result: PipelineResult[JobDiscoveryOutput]): Unit = {
    logger.error(
      s"[$name] Failed after ${result.durationMs}ms: ${result.error.map(_.message).getOrElse("unknown error")}"
    )
  }

object JobDiscoveryWorkflow:
  def apply(
    supabaseClient: SupabaseClient,
    aiConfig: AIConfig,
    storageClient: Option[ObjectStorageClient] = None,
  )(
    using
    ac: castor.Context
  ): JobDiscoveryWorkflow =
    new JobDiscoveryWorkflow(supabaseClient, aiConfig, storageClient)
