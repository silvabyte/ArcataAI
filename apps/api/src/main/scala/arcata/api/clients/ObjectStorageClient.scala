package arcata.api.clients

import scala.util.{Failure, Success, Try}

import scribe.Logging
import upickle.default.*

/** Metadata for a stored object in ObjectStorage. */
final case class StoredObject(
    objectId: String,
    fileName: String,
    size: Long,
    bucket: String,
    lastModified: String,
    createdAt: String,
    mimeType: Option[String] = None,
    checksum: Option[String] = None,
    etag: Option[String] = None,
    contentType: Option[String] = None,
    metadata: Option[Map[String, String]] = None
) derives ReadWriter

/** Response wrapper from ObjectStorage API. */
final case class FileResponse(
    file: StoredObject,
    status: Option[String] = None
) derives ReadWriter

/** Error response from ObjectStorage API. */
final case class ErrorResponse(
    message: String,
    status: String
) derives ReadWriter

/** Errors that can occur during storage operations. */
sealed trait StorageError

object StorageError {
  final case class NotFound(objectId: String) extends StorageError
  final case class ApiError(statusCode: Int, message: String) extends StorageError
  final case class NetworkError(message: String, cause: Option[Throwable] = None) extends StorageError
}

/**
 * Client for the ObjectStorage service at s3.audetic.link.
 *
 * @param baseUrl
 *   Base URL for the API (e.g., https://s3.audetic.link/api/v1)
 * @param tenantId
 *   Tenant identifier (e.g., "arcata")
 */
class ObjectStorageClient(
    baseUrl: String,
    tenantId: String
) extends Logging {

  private def tenantHeaders(userId: String): Map[String, String] = Map(
    "x-tenant-id" -> tenantId,
    "x-user-id" -> userId
  )

  /**
   * Upload a file to storage.
   *
   * @param content
   *   File content as bytes
   * @param fileName
   *   Name for the file
   * @param mimeType
   *   Optional MIME type
   * @param userId
   *   User ID for multi-tenant isolation
   * @return
   *   StoredObject on success, StorageError on failure
   */
  def upload(
      content: Array[Byte],
      fileName: String,
      mimeType: Option[String],
      userId: String
  ): Either[StorageError, StoredObject] = {
    val headers = tenantHeaders(userId) ++ Map(
      "x-file-name" -> fileName,
      "Content-Type" -> mimeType.getOrElse("application/octet-stream")
    ) ++ mimeType.map(m => "x-mimetype" -> m)

    Try {
      requests.post(
        s"$baseUrl/files",
        headers = headers,
        data = content
      )
    } match {
      case Failure(e) =>
        Left(StorageError.NetworkError(e.getMessage, Some(e)))

      case Success(response) if response.statusCode == 201 || response.statusCode == 200 =>
        parseFileResponse(response.text())

      case Success(response) =>
        parseErrorResponse(response)
    }
  }

  /**
   * Download a file's content.
   *
   * @param objectId
   *   UUID of the stored object
   * @param userId
   *   User ID for multi-tenant isolation
   * @return
   *   File bytes on success, StorageError on failure
   */
  def download(objectId: String, userId: String): Either[StorageError, Array[Byte]] = {
    Try {
      requests.get(
        s"$baseUrl/files/$objectId",
        headers = tenantHeaders(userId)
      )
    } match {
      case Failure(e) =>
        Left(StorageError.NetworkError(e.getMessage, Some(e)))

      case Success(response) if response.statusCode == 200 =>
        Right(response.bytes)

      case Success(response) if response.statusCode == 404 =>
        Left(StorageError.NotFound(objectId))

      case Success(response) =>
        val message = Try(read[ErrorResponse](response.text()))
          .map(_.message)
          .getOrElse(response.text())
        Left(StorageError.ApiError(response.statusCode, message))
    }
  }

  /**
   * Get metadata for a stored object.
   *
   * @param objectId
   *   UUID of the stored object
   * @param userId
   *   User ID for multi-tenant isolation
   * @return
   *   StoredObject metadata on success, StorageError on failure
   */
  def getMetadata(objectId: String, userId: String): Either[StorageError, StoredObject] = {
    Try {
      requests.get(
        s"$baseUrl/files/$objectId/metadata",
        headers = tenantHeaders(userId)
      )
    } match {
      case Failure(e) =>
        Left(StorageError.NetworkError(e.getMessage, Some(e)))

      case Success(response) if response.statusCode == 200 =>
        parseFileResponse(response.text())

      case Success(response) if response.statusCode == 404 =>
        Left(StorageError.NotFound(objectId))

      case Success(response) =>
        parseErrorResponse(response)
    }
  }

  /**
   * Delete a stored object.
   *
   * @param objectId
   *   UUID of the stored object
   * @param userId
   *   User ID for multi-tenant isolation
   * @return
   *   Deleted StoredObject on success, StorageError on failure
   */
  def delete(objectId: String, userId: String): Either[StorageError, StoredObject] = {
    Try {
      requests.delete(
        s"$baseUrl/files/$objectId",
        headers = tenantHeaders(userId)
      )
    } match {
      case Failure(e) =>
        Left(StorageError.NetworkError(e.getMessage, Some(e)))

      case Success(response) if response.statusCode == 200 =>
        parseFileResponse(response.text())

      case Success(response) if response.statusCode == 404 =>
        Left(StorageError.NotFound(objectId))

      case Success(response) =>
        parseErrorResponse(response)
    }
  }

  /**
   * Find a file by its SHA-256 checksum (for deduplication).
   *
   * @param checksum
   *   SHA-256 checksum to search for
   * @param userId
   *   User ID for multi-tenant isolation
   * @return
   *   Some(StoredObject) if found, None if not found, StorageError on failure
   */
  def findByChecksum(checksum: String, userId: String): Either[StorageError, Option[StoredObject]] = {
    Try {
      requests.get(
        s"$baseUrl/files/checksum/$checksum",
        headers = tenantHeaders(userId)
      )
    } match {
      case Failure(e) =>
        Left(StorageError.NetworkError(e.getMessage, Some(e)))

      case Success(response) if response.statusCode == 200 =>
        parseFileResponse(response.text()).map(Some(_))

      case Success(response) if response.statusCode == 404 =>
        Right(None)

      case Success(response) =>
        parseErrorResponse(response).map(_ => None)
    }
  }

  private def parseFileResponse(text: String): Either[StorageError, StoredObject] = {
    Try(read[FileResponse](text)) match {
      case Success(response) => Right(response.file)
      case Failure(e) =>
        logger.error(s"Failed to parse FileResponse: ${e.getMessage}")
        Left(StorageError.ApiError(500, s"Invalid response format: ${e.getMessage}"))
    }
  }

  private def parseErrorResponse(response: requests.Response): Either[StorageError, StoredObject] = {
    val message = Try(read[ErrorResponse](response.text()))
      .map(_.message)
      .getOrElse(response.text())
    Left(StorageError.ApiError(response.statusCode, message))
  }
}

object ObjectStorageClient {

  /**
   * Create an ObjectStorageClient from configuration.
   *
   * @param baseUrl
   *   Base URL (e.g., https://s3.audetic.link/api/v1)
   * @param tenantId
   *   Tenant ID (e.g., "arcata")
   */
  def apply(baseUrl: String, tenantId: String): ObjectStorageClient =
    new ObjectStorageClient(baseUrl, tenantId)
}
