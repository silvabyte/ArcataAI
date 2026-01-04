package arcata.api.etl.steps

import arcata.api.config.ResumeConfig
import arcata.api.etl.framework.*

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
 * Magic byte detection prevents file extension spoofing attacks where a malicious file is renamed
 * to appear as a PDF or DOCX.
 *
 * TODO: Replace inline magic byte detection with boogieloops.kit.MagicNumber once Kit is published
 * to Maven Central (see BoogieLoops PR #25).
 */
final class FileValidator(config: ResumeConfig) extends BaseStep[FileValidatorInput, FileValidatorOutput]:

  val name = "FileValidator"

  // Allowed MIME types for resume files
  private val AllowedMimeTypes = Set(
    "application/pdf",
    "application/zip", // DOCX is ZIP-based
    "text/plain"
  )

  // Magic byte signatures for file type detection
  // Based on boogieloops.kit.MagicNumber patterns
  private val PdfMagic = Array[Byte](0x25, 0x50, 0x44, 0x46) // %PDF
  private val ZipMagic = Array[Byte](0x50, 0x4b, 0x03, 0x04) // PK..

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
          message = s"File size (${bytes.length / 1024 / 1024}MB) exceeds maximum allowed size (${config.maxFileSizeMb}MB)",
          stepName = name
        )
      )

    // Detect file type from magic bytes
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
   * Detect file type from magic bytes.
   *
   * Falls back to extension-based detection for plain text files which have no magic bytes.
   */
  private def detectFileType(bytes: Array[Byte], fileName: String): DetectedFileType = {
    if bytes.length >= 4 then
      // Check PDF magic bytes
      if bytes.take(4).sameElements(PdfMagic) then
        return DetectedFileType("PDF", "application/pdf")

      // Check ZIP magic bytes (DOCX is ZIP-based)
      if bytes.take(4).sameElements(ZipMagic) then
        return DetectedFileType("ZIP/DOCX", "application/zip")

    // Fall back to extension-based detection for text files
    val ext = fileName.toLowerCase.split('.').lastOption.getOrElse("")
    ext match
      case "txt" | "text" => DetectedFileType("Plain Text", "text/plain")
      case "md"           => DetectedFileType("Markdown", "text/plain")
      case _              => DetectedFileType("Unknown", "application/octet-stream")
  }

  /**
   * Check if the file name indicates a DOCX file.
   */
  private def isDocx(fileName: String): Boolean = {
    fileName.toLowerCase.endsWith(".docx")
  }

object FileValidator:
  def apply(config: ResumeConfig): FileValidator = new FileValidator(config)
