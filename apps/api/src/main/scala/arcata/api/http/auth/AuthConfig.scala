package arcata.api.http.auth

import arcata.api.config.ConfigError

/**
 * Self-contained auth configuration loaded from environment variables.
 *
 * This singleton lazy-loads auth config so the auth module is self-contained and decorators can be
 * used with simple imports. Routes don't need to receive auth dependencies - they just import and
 * use the decorators.
 *
 * Environment variables:
 *   - SUPABASE_JWT_SECRET: Required for JWT validation
 *   - API_KEYS: Optional comma-separated list of valid API keys for service auth
 */
object AuthConfig:

  /** JWT validator for user authentication via Supabase tokens. */
  lazy val jwtValidator: JwtValidator = {
    // scalafix:ok DisableSyntax.throw - Intentional fail-fast at startup for required config
    val secret = sys.env.getOrElse(
      "SUPABASE_JWT_SECRET",
      throw ConfigError("Required environment variable 'SUPABASE_JWT_SECRET' is not set") // scalafix:ok DisableSyntax.throw
    )
    JwtValidator(secret)
  }

  /** Valid API keys for service-to-service authentication. */
  lazy val validApiKeys: Set[String] = {
    sys.env
      .get("API_KEYS")
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  }

  /** Check if API key authentication is configured. */
  def apiKeysConfigured: Boolean = validApiKeys.nonEmpty
