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

  /**
   * JWKS provider for fetching Supabase public keys.
   *
   * Initialized lazily from SUPABASE_URL environment variable. Throws ConfigError if the
   * environment variable is not set.
   */
  private lazy val jwksProvider: JwksProvider = {
    val supabaseUrl = sys.env.getOrElse(
      "SUPABASE_URL",
      throw ConfigError("Required environment variable 'SUPABASE_URL' is not set"), // scalafix:ok DisableSyntax.throw

    )
    JwksProvider(JwksProvider.jwksUrlFor(supabaseUrl))
  }

  /**
   * JWT validator for user authentication via Supabase tokens.
   *
   * Uses the JWKS provider to validate ES256-signed JWTs from Supabase Auth.
   */
  lazy val jwtValidator: JwtValidator = JwtValidator(jwksProvider)

  /**
   * Valid API keys for service-to-service authentication.
   *
   * Loaded from the API_KEYS environment variable as a comma-separated list. Empty set if not
   * configured.
   */
  lazy val validApiKeys: Set[String] = {
    sys.env
      .get("API_KEYS")
      .map(_.split(",").map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  }

  /**
   * Check if API key authentication is configured.
   *
   * @return
   *   true if at least one API key is configured, false otherwise
   */
  def apiKeysConfigured: Boolean = validApiKeys.nonEmpty
