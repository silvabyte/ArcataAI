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
 *   - SUPABASE_URL: Required - used to derive JWKS endpoint for JWT validation
 *   - API_KEYS: Optional comma-separated list of valid API keys for service auth
 */
object AuthConfig:

  /** JWKS provider for fetching Supabase public keys. */
  private lazy val jwksProvider: JwksProvider = {
    val supabaseUrl = sys.env.getOrElse(
      "SUPABASE_URL",
      throw ConfigError("Required environment variable 'SUPABASE_URL' is not set"), // scalafix:ok DisableSyntax.throw

    )
    JwksProvider(JwksProvider.jwksUrlFor(supabaseUrl))
  }

  /** JWT validator for user authentication via Supabase tokens. */
  lazy val jwtValidator: JwtValidator = JwtValidator(jwksProvider)

  /** Valid API keys for service-to-service authentication. */
  lazy val validApiKeys: Set[String] = {
    sys.env
      .get("API_KEYS")
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  }

  /** Check if API key authentication is configured. */
  def apiKeysConfigured: Boolean = validApiKeys.nonEmpty
