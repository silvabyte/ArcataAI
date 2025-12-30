package arcata.api

import arcata.api.clients.{ObjectStorageClient, SupabaseClient}
import arcata.api.config.Config
import arcata.api.etl.JobIngestionPipeline
import arcata.api.http.middleware.{CorsConfig, CorsRoutes}
import arcata.api.http.routes.{IndexRoutes, JobRoutes}
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

  // Initialize clients
  lazy val supabaseClient: SupabaseClient = SupabaseClient(config.supabase)

  lazy val storageClient: Option[ObjectStorageClient] = {
    if (config.objectStorage.baseUrl.nonEmpty) {
      Some(
        ObjectStorageClient(
          baseUrl = config.objectStorage.baseUrl,
          tenantId = config.objectStorage.tenantId,
          apiKey = config.objectStorage.apiKey
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
    storageClient = storageClient
  )

  // Initialize routes
  lazy val indexRoutes: IndexRoutes = IndexRoutes()

  lazy val jobRoutes: JobRoutes = JobRoutes(
    basePath = "/api/v1",
    pipeline = jobIngestionPipeline,
    corsConfig = corsConfig
  )

  // CORS routes (handles OPTIONS preflight requests)
  lazy val corsRoutes: CorsRoutes = CorsRoutes(corsConfig)

  // Register all routes
  override def allRoutes: Seq[cask.Routes] = Seq(
    corsRoutes,
    indexRoutes,
    jobRoutes
  )

  // Server configuration
  override def host: String = config.server.host
  override def port: Int = config.server.port

  override def main(args: Array[String]): Unit = {
    Log.info(
      s"Starting Arcata API server",
      Map(
        "host" -> config.server.host,
        "port" -> config.server.port.toString
      )
    )
    super.main(args)
  }
}
