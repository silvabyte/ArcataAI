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
    ai: AIConfig
)

/** HTTP server configuration. */
final case class ServerConfig(
    host: String,
    port: Int
)

/** Supabase configuration for database access and JWT validation. */
final case class SupabaseConfig(
    url: String,
    anonKey: String,
    serviceRoleKey: String,
    jwtSecret: String
)

/** Object storage configuration for s3.audetic.link service. */
final case class ObjectStorageConfig(
    baseUrl: String,
    tenantId: String
)

/** AI provider configuration for job parsing and company enrichment. */
final case class AIConfig(
    baseUrl: String,    // Vercel AI Gateway: https://api.vercel.ai/v1
    apiKey: String,     // VERCEL_AI_GATEWAY_API_KEY
    model: String       // anthropic/claude-sonnet-4-20250514
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
    yield Config(
      server = server,
      supabase = supabase,
      objectStorage = objectStorage,
      ai = ai
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
      case Left(error) => throw error
  }

  private def loadServerConfig(): Either[ConfigError, ServerConfig] = {
    val host = getEnvOrDefault("API_HOST", "0.0.0.0")
    val portStr = getEnvOrDefault("API_PORT", "4203")

    Try(portStr.toInt) match
      case Success(port) => Right(ServerConfig(host = host, port = port))
      case Failure(_) =>
        Left(ConfigError(s"API_PORT must be a valid integer, got: '$portStr'"))
  }

  private def loadSupabaseConfig(): Either[ConfigError, SupabaseConfig] = {
    for
      url <- getEnvRequired("SUPABASE_URL")
      anonKey <- getEnvRequired("SUPABASE_ANON_KEY")
      serviceRoleKey <- getEnvRequired("SUPABASE_SERVICE_ROLE_KEY")
      jwtSecret <- getEnvRequired("SUPABASE_JWT_SECRET")
    yield SupabaseConfig(
      url = url,
      anonKey = anonKey,
      serviceRoleKey = serviceRoleKey,
      jwtSecret = jwtSecret
    )
  }

  private def loadObjectStorageConfig(): Either[ConfigError, ObjectStorageConfig] = {
    Right(
      ObjectStorageConfig(
        baseUrl = getEnvOrDefault("OBJECT_STORAGE_URL", "https://s3.audetic.link/api/v1"),
        tenantId = getEnvOrDefault("OBJECT_STORAGE_TENANT_ID", "arcata")
      )
    )
  }

  private def loadAIConfig(): Either[ConfigError, AIConfig] = {
    for
      apiKey <- getEnvRequired("VERCEL_AI_GATEWAY_API_KEY")
    yield AIConfig(
      baseUrl = getEnvOrDefault("VERCEL_AI_GATEWAY_URL", "https://api.vercel.ai/v1"),
      apiKey = apiKey,
      model = getEnvOrDefault("AI_MODEL", "anthropic/claude-sonnet-4-20250514")
    )
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
