package arcata.api.config

import scala.util.{Failure, Success, Try}

/**
 * Application configuration loaded from environment variables.
 *
 * All required environment variables must be set, or the application will fail to start. Optional
 * variables have sensible defaults.
 */
final case class Config(
    server: ServerConfig,
    supabase: SupabaseConfig,
    objectStorage: ObjectStorageConfig,
    ai: AIConfig,
    resume: ResumeConfig
)

/** HTTP server configuration. */
final case class ServerConfig(
    host: String,
    port: Int,
    corsOrigins: List[String]
)

/** Supabase configuration for database access. JWT validation uses JWKS from the URL. */
final case class SupabaseConfig(
    url: String,
    serviceRoleKey: String
)

/** Object storage configuration for s3.audetic.link service. */
final case class ObjectStorageConfig(
    baseUrl: String,
    tenantId: String,
    apiKey: String
)

/** AI provider configuration for job parsing and company enrichment. */
final case class AIConfig(
    baseUrl: String, // Vercel AI Gateway: https://api.vercel.ai/v1
    apiKey: String, // VERCEL_AI_GATEWAY_API_KEY
    model: String // anthropic/claude-sonnet-4-20250514
)

/** Resume parsing configuration. */
final case class ResumeConfig(
    maxFileSizeMb: Int // Maximum allowed resume file size in megabytes
)

object Config:
  /**
   * Load configuration from environment variables.
   *
   * @return
   *   Either a ConfigError if required variables are missing, or the Config
   */
  def load(): Either[ConfigError, Config] = {
    for
      server <- loadServerConfig()
      supabase <- loadSupabaseConfig()
      objectStorage <- loadObjectStorageConfig()
      ai <- loadAIConfig()
      resume <- loadResumeConfig()
    yield Config(
      server = server,
      supabase = supabase,
      objectStorage = objectStorage,
      ai = ai,
      resume = resume
    )
  }

  /**
   * Load configuration, throwing on error. Useful for application startup.
   *
   * @throws ConfigError
   *   if required environment variables are missing
   */
  def loadOrThrow(): Config = {
    load() match
      case Right(config) => config
      case Left(error) => throw error // scalafix:ok DisableSyntax.throw
  }

  private def loadServerConfig(): Either[ConfigError, ServerConfig] = {
    val host = getEnvOrDefault("API_HOST", "0.0.0.0")
    val portStr = getEnvOrDefault("API_PORT", "4203")
    val corsOriginsStr = getEnvOrDefault(
      "CORS_ORIGINS",
      "http://localhost:4201,http://localhost:4200,https://f6c0bn-pical.spa.godeploy.app"
    )
    val corsOrigins = corsOriginsStr.split(",").map(_.trim).filter(_.nonEmpty).toList

    Try(portStr.toInt) match
      case Success(port) =>
        Right(ServerConfig(host = host, port = port, corsOrigins = corsOrigins))
      case Failure(_) =>
        Left(ConfigError(s"API_PORT must be a valid integer, got: '$portStr'"))
  }

  private def loadSupabaseConfig(): Either[ConfigError, SupabaseConfig] = {
    for
      url <- getEnvRequired("SUPABASE_URL")
      serviceRoleKey <- getEnvRequired("SUPABASE_SERVICE_ROLE_KEY")
    yield SupabaseConfig(
      url = url,
      serviceRoleKey = serviceRoleKey
    )
  }

  private def loadObjectStorageConfig(): Either[ConfigError, ObjectStorageConfig] = {
    Right(
      ObjectStorageConfig(
        baseUrl = getEnvOrDefault("OBJECT_STORAGE_URL", "https://s3.audetic.link/api/v1"),
        // this app does not have multi-tenancy, so just provide a fixed tenant here
        tenantId = getEnvOrDefault("OBJECT_STORAGE_TENANT_ID", "arcata-ai"),
        apiKey = getEnvOrDefault("OBJECT_STORAGE_API_KEY", "")
      )
    )
  }

  private def loadAIConfig(): Either[ConfigError, AIConfig] = {
    for
      apiKey <- getEnvRequired("VERCEL_AI_GATEWAY_API_KEY")
    yield AIConfig(
      baseUrl = getEnvOrDefault("VERCEL_AI_GATEWAY_URL", "https://api.vercel.ai/v1"),
      apiKey = apiKey,
      model = getEnvOrDefault("AI_MODEL", "anthropic/claude-haiku-4.5")
    )
  }

  private def loadResumeConfig(): Either[ConfigError, ResumeConfig] = {
    val maxFileSizeStr = getEnvOrDefault("RESUME_MAX_FILE_SIZE_MB", "10")
    Try(maxFileSizeStr.toInt) match
      case Success(maxSize) if maxSize > 0 =>
        Right(ResumeConfig(maxFileSizeMb = maxSize))
      case Success(_) =>
        Left(ConfigError("RESUME_MAX_FILE_SIZE_MB must be a positive integer"))
      case Failure(_) =>
        Left(ConfigError(s"RESUME_MAX_FILE_SIZE_MB must be a valid integer, got: '$maxFileSizeStr'"))
  }

  private def getEnvRequired(name: String): Either[ConfigError, String] = {
    sys.env.get(name).toRight(
      ConfigError(s"Required environment variable '$name' is not set")
    )
  }

  private def getEnvOrDefault(name: String, default: String): String =
    sys.env.getOrElse(name, default)

/** Error when configuration is invalid or incomplete. */
final case class ConfigError(message: String) extends Exception(message)
