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
    boogieLoops: BoogieLoopsConfig
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

/** BoogieLoops AI service configuration for job parsing. */
final case class BoogieLoopsConfig(
    apiUrl: String,
    apiKey: String
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
      boogieLoops <- loadBoogieLoopsConfig()
    yield Config(
      server = server,
      supabase = supabase,
      objectStorage = objectStorage,
      boogieLoops = boogieLoops
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
    val portStr = getEnvOrDefault("API_PORT", "8080")

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

  private def loadBoogieLoopsConfig(): Either[ConfigError, BoogieLoopsConfig] = {
    Right(
      BoogieLoopsConfig(
        apiUrl = getEnvOrDefault("BOOGIELOOPS_API_URL", ""),
        apiKey = getEnvOrDefault("BOOGIELOOPS_API_KEY", "")
      )
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
