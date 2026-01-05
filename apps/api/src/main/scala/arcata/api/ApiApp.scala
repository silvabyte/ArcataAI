package arcata.api

import scala.concurrent.ExecutionContext

import arcata.api.clients.{ObjectStorageClient, SupabaseClient}
import arcata.api.config.Config
import arcata.api.etl.{JobIngestionPipeline, ResumeParsingPipeline}
import arcata.api.etl.workflows.{JobDiscoveryWorkflow, JobStatusWorkflow}
import arcata.api.http.middleware.{CorsConfig, CorsRoutes}
import arcata.api.http.routes.{CronRoutes, IndexRoutes, JobRoutes, ResumeRoutes}
import arcata.api.logging.Log

/**
 * Main entry point for the Arcata API server.
 *
 * This application provides a REST API for job ingestion using Cask HTTP server
 * with AI-powered job extraction via Vercel AI Gateway.
 */
object ApiApp extends cask.Main {

  // Load configuration from environment (throws on missing required vars)
  lazy val config: Config = Config.loadOrThrow()

  // Override the actor context for async workflows (Castor)
  // Uses global execution context and logs actor errors
  override val actorContext: castor.Context = new castor.Context.Simple(
    ExecutionContext.global,
    (e: Throwable) => Log.error(s"Actor error: ${e.getMessage}", Map("exception" -> e.toString)),
  )

  // Initialize clients
  lazy val supabaseClient: SupabaseClient = SupabaseClient(config.supabase)

  lazy val storageClient: Option[ObjectStorageClient] = {
    if (config.objectStorage.baseUrl.nonEmpty) {
      Some(
        ObjectStorageClient(
          baseUrl = config.objectStorage.baseUrl,
          tenantId = config.objectStorage.tenantId,
          apiKey = config.objectStorage.apiKey,
        )
      )
    } else {
      Log.info("Object storage not configured")
      None
    }
  }

  // Initialize CORS configuration
  lazy val corsConfig: CorsConfig = CorsConfig(
    allowedOrigins = config.server.corsOrigins
  )

  // Initialize pipeline
  lazy val jobIngestionPipeline: JobIngestionPipeline = JobIngestionPipeline(
    supabaseClient = supabaseClient,
    aiConfig = config.ai,
    storageClient = storageClient,
  )

  // Initialize async workflows
  lazy val jobStatusWorkflow: JobStatusWorkflow = {
    given castor.Context = actorContext
    JobStatusWorkflow(supabaseClient)
  }

  lazy val jobDiscoveryWorkflow: JobDiscoveryWorkflow = {
    given castor.Context = actorContext
    JobDiscoveryWorkflow(supabaseClient, config.ai, storageClient)
  }

  // Initialize routes
  lazy val indexRoutes: IndexRoutes = IndexRoutes()

  lazy val jobRoutes: JobRoutes = JobRoutes(
    basePath = "/api/v1",
    pipeline = jobIngestionPipeline,
    corsConfig = corsConfig,
  )

  // Resume parsing pipeline (requires storage client)
  lazy val resumeParsingPipeline: Option[ResumeParsingPipeline] = storageClient.map {
    storage =>
      ResumeParsingPipeline(
        resumeConfig = config.resume,
        aiConfig = config.ai,
        storageClient = storage,
      )
  }

  // Resume routes (only available if storage is configured)
  lazy val resumeRoutes: Option[ResumeRoutes] = resumeParsingPipeline.map {
    pipeline =>
      ResumeRoutes(
        basePath = "/api/v1",
        pipeline = pipeline,
        corsConfig = corsConfig,
      )
  }

  // Cron routes for background workflows (authenticated via API key)
  lazy val cronRoutes: CronRoutes = CronRoutes(
    jobStatusWorkflow = jobStatusWorkflow,
    jobDiscoveryWorkflow = jobDiscoveryWorkflow,
    corsConfig = corsConfig,
  )

  // CORS routes (handles OPTIONS preflight requests)
  lazy val corsRoutes: CorsRoutes = CorsRoutes(corsConfig)

  // Register all routes
  override def allRoutes: Seq[cask.Routes] = {
    val baseRoutes = Seq(
      corsRoutes,
      indexRoutes,
      jobRoutes,
      cronRoutes,
    )
    // Add resume routes if storage is configured
    baseRoutes ++ resumeRoutes.toSeq
  }

  // Server configuration
  override def host: String = config.server.host
  override def port: Int = config.server.port

  override def main(args: Array[String]): Unit = {
    Log.info(
      s"Starting Arcata API server",
      Map(
        "host" -> config.server.host,
        "port" -> config.server.port.toString,
        "corsOrigins" -> config.server.corsOrigins.mkString(", "),
      ),
    )
    super.main(args)
  }
}
