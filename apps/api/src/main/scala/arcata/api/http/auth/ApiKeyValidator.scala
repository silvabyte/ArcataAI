package arcata.api.http.auth

import cask.model.Request

/**
 * Validates API keys for service-to-service authentication.
 *
 * API keys are passed via the X-API-Key header and validated against the configured set of valid
 * keys from AuthConfig.
 */
object ApiKeyValidator:
  private val headerName = "x-api-key"

  /**
   * Validate an API key from the request headers.
   *
   * @param request
   *   The HTTP request to validate
   * @return
   *   AuthResult.Success with service identity, or AuthResult.Failure with reason
   */
  def validate(request: Request): AuthResult = {
    request.headers.get(headerName).flatMap(_.headOption) match
      case None =>
        AuthResult.Failure("Missing X-API-Key header")

      case Some(key) if AuthConfig.validApiKeys.contains(key) =>
        AuthResult.Success(
          AuthenticatedRequest(
            profileId = "service",
            claims = None,
            authType = AuthType.ApiKey,
            request = request
          )
        )

      case Some(_) =>
        AuthResult.Failure("Invalid API key")
  }
