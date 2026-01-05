package arcata.api.etl.steps

import arcata.api.clients.{ObjectStorageClient, StorageError}
import arcata.api.etl.framework.*

/**
 * Input for the FileStorer step.
 *
 * @param fileBytes
 *   Validated file bytes to store
 * @param fileName
 *   Original file name
 * @param detectedType
 *   Detected file type from FileValidator
 * @param profileId
 *   User's profile ID for storage isolation
 */
final case class FileStorerInput(
  fileBytes: Array[Byte],
  fileName: String,
  detectedType: DetectedFileType,
  profileId: String,
)

/**
 * Output from the FileStorer step.
 *
 * @param fileBytes
 *   File bytes (passed through for text extraction)
 * @param fileName
 *   Original file name
 * @param detectedType
 *   Detected file type
 * @param objectId
 *   UUID of the stored object in ObjectStorage
 */
final case class FileStorerOutput(
  fileBytes: Array[Byte],
  fileName: String,
  detectedType: DetectedFileType,
  objectId: String,
)

/**
 * ETL step that stores the resume file in ObjectStorage.
 *
 * Stores the file and returns the objectId for later retrieval. The objectId is stored in the
 * job_profiles.resume_file_id column to allow downloading the original file.
 *
 * ObjectStorage handles deduplication via content-addressed storage (SHA-256 checksum), so storing
 * the same file multiple times won't consume additional space.
 */
final class FileStorer(storageClient: ObjectStorageClient)
  extends BaseStep[FileStorerInput, FileStorerOutput]:

  val name = "FileStorer"

  override def execute(
    input: FileStorerInput,
    ctx: PipelineContext,
  ): Either[StepError, FileStorerOutput] = {
    logger.info(s"[${ctx.runId}] Storing file: ${input.fileName} (${input.fileBytes.length} bytes)")

    storageClient.upload(
      content = input.fileBytes,
      fileName = input.fileName,
      mimeType = Some(input.detectedType.mimeType),
      userId = input.profileId,
    ) match
      case Left(err) =>
        val message = err match
          case StorageError.NetworkError(msg, _) => s"Failed to connect to storage: $msg"
          case StorageError.ApiError(code, msg) => s"Storage API error ($code): $msg"
          case StorageError.NotFound(id) => s"Storage not found: $id"
        Left(
          StepError.LoadError(
            message = message,
            stepName = name,
            cause = err match
              case StorageError.NetworkError(_, cause) => cause
              case _ => None,
          )
        )

      case Right(storedObject) =>
        logger.info(s"[${ctx.runId}] Stored file as objectId=${storedObject.objectId}")
        Right(
          FileStorerOutput(
            fileBytes = input.fileBytes,
            fileName = input.fileName,
            detectedType = input.detectedType,
            objectId = storedObject.objectId,
          )
        )
  }

object FileStorer:
  def apply(storageClient: ObjectStorageClient): FileStorer = new FileStorer(storageClient)
