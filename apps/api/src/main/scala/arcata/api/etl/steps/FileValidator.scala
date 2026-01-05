package arcata.api.etl.steps

import arcata.api.config.ResumeConfig
import arcata.api.etl.framework.*
import boogieloops.kit.MagicNumber

/**
 * Detected file type from magic bytes.
 *
 * @param name
 *   Human-readable name
 * @param mimeType
 *   MIME type for the file
 */
final case class DetectedFileType(name: String, mimeType: String)

/**
 * Input for the FileValidator step.
 *
 * @param fileBytes
 *   Raw file bytes (full file or at least first 24 bytes for detection)
 * @param fileName
 *   Original file name from upload
 * @param claimedMimeType
 *   MIME type claimed by the client (may be spoofed)
 */
final case class FileValidatorInput(
    fileBytes: Array[Byte],
    fileName: String,
    claimedMimeType: Option[String]
)

/**
 * Output from the FileValidator step.
 *
 * @param fileBytes
 *   Validated file bytes (passed through)
 * @param fileName
 *   Original file name
 * @param detectedType
 *   Actual file type detected from magic bytes
 * @param fileSizeBytes
 *   Size of the file in bytes
 */
final case class FileValidatorOutput(
    fileBytes: Array[Byte],
    fileName: String,
    detectedType: DetectedFileType,
    fileSizeBytes: Long
)

/**
 * ETL step that validates uploaded resume files.
 *
 * Validates:
 * - File size is within limits
 * - File type is allowed (PDF, DOCX, or TXT) using magic byte detection
 * - File is not empty
 *
 * Uses BoogieLoops Kit MagicNumber for file type detection, which prevents file extension spoofing
 * attacks where a malicious file is renamed to appear as a PDF or DOCX.
 */
final class FileValidator(config: ResumeConfig) extends BaseStep[FileValidatorInput, FileValidatorOutput]:

  val name = "FileValidator"

  // Allowed MIME types for resume files
  private val AllowedMimeTypes = Set(
    "application/pdf",
    "application/zip", // DOCX is ZIP-based
    "text/plain"
  )

  override def execute(
      input: FileValidatorInput,
      ctx: PipelineContext
  ): Either[StepError, FileValidatorOutput] = {
    val bytes = input.fileBytes
    val maxSizeBytes = config.maxFileSizeMb * 1024 * 1024

    // Check if file is empty
    if bytes.isEmpty then
      return Left(
        StepError.ValidationError(
          message = "File is empty",
          stepName = name
        )
      )

    // Check file size
    if bytes.length > maxSizeBytes then
      return Left(
        StepError.ValidationError(
          message =
            s"File size (${bytes.length / 1024 / 1024}MB) exceeds maximum allowed size (${config.maxFileSizeMb}MB)",
          stepName = name
        )
      )

    // Detect file type from magic bytes using BoogieLoops Kit
    val detectedType = detectFileType(bytes, input.fileName)

    // Validate file type is allowed
    if !AllowedMimeTypes.contains(detectedType.mimeType) then
      return Left(
        StepError.ValidationError(
          message = s"File type '${detectedType.name}' is not allowed. Accepted types: PDF, DOCX, TXT",
          stepName = name
        )
      )

    // For DOCX, verify it's actually a DOCX and not just any ZIP
    if detectedType.mimeType == "application/zip" && !isDocx(input.fileName) then
      return Left(
        StepError.ValidationError(
          message = "Only DOCX files are accepted for ZIP-based documents. Please upload a .docx file.",
          stepName = name
        )
      )

    logger.info(
      s"[${ctx.runId}] Validated file: ${input.fileName} (${detectedType.name}, ${bytes.length} bytes)"
    )

    Right(
      FileValidatorOutput(
        fileBytes = bytes,
        fileName = input.fileName,
        detectedType = detectedType,
        fileSizeBytes = bytes.length.toLong
      )
    )
  }

  /**
   * Detect file type from magic bytes using BoogieLoops Kit MagicNumber.
   *
   * Falls back to extension-based detection for plain text files which have no magic bytes.
   */
  private def detectFileType(bytes: Array[Byte], fileName: String): DetectedFileType = {
    // Use MagicNumber for detection (needs at least HeaderLength bytes)
    val header = bytes.take(MagicNumber.HeaderLength)

    MagicNumber.detect(header) match
      case Some(sig) =>
        DetectedFileType(sig.name, sig.mimeType)
      case None =>
        // Fall back to extension-based detection for text files (no magic bytes)
        val ext = fileName.toLowerCase.split('.').lastOption.getOrElse("")
        ext match
          case "txt" | "text" => DetectedFileType("Plain Text", "text/plain")
          case "md" => DetectedFileType("Markdown", "text/plain")
          case _ => DetectedFileType("Unknown", "application/octet-stream")
  }

  /**
   * Check if the file name indicates a DOCX file.
   */
  private def isDocx(fileName: String): Boolean = {
    fileName.toLowerCase.endsWith(".docx")
  }

object FileValidator:
  def apply(config: ResumeConfig): FileValidator = new FileValidator(config)
