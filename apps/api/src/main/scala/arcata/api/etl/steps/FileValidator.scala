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
  claimedMimeType: Option[String],
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
  fileSizeBytes: Long,
)

/**
 * ETL step that validates uploaded resume files.
 *
 * Validates:
 * - File is not empty
 * - File size is within limits
 * - File type can be detected (for routing to appropriate extractor)
 *
 * Uses BoogieLoops Kit MagicNumber for file type detection. The actual format support
 * is determined by the TextExtractor step - this step just validates basic constraints
 * and detects the file type for routing.
 */
final class FileValidator(config: ResumeConfig) extends BaseStep[FileValidatorInput, FileValidatorOutput]:

  val name = "FileValidator"

  override def execute(
    input: FileValidatorInput,
    ctx: PipelineContext,
  ): Either[StepError, FileValidatorOutput] = {
    val bytes = input.fileBytes
    val maxSizeBytes = config.maxFileSizeMb * 1024 * 1024

    if bytes.isEmpty then
      Left(
        StepError.ValidationError(
          message = "File is empty",
          stepName = name,
        )
      )
    else if bytes.length > maxSizeBytes then
      Left(
        StepError.ValidationError(
          message =
            s"File size (${bytes.length / 1024 / 1024}MB) exceeds maximum allowed size (${config.maxFileSizeMb}MB)",
          stepName = name,
        )
      )
    else
      // Detect file type from magic bytes using BoogieLoops Kit
      val detectedType = detectFileType(bytes, input.fileName)

      logger.info(
        s"[${ctx.runId}] Validated file: ${input.fileName} (${detectedType.name}, ${bytes.length} bytes)"
      )

      Right(
        FileValidatorOutput(
          fileBytes = bytes,
          fileName = input.fileName,
          detectedType = detectedType,
          fileSizeBytes = bytes.length.toLong,
        )
      )
  }

  /**
   * Detect file type from magic bytes using BoogieLoops Kit MagicNumber.
   *
   * Falls back to extension-based detection for text-based formats which have no magic bytes.
   */
  private def detectFileType(bytes: Array[Byte], fileName: String): DetectedFileType = {
    // Use MagicNumber for detection (needs at least HeaderLength bytes)
    val header = bytes.take(MagicNumber.HeaderLength)
    val ext = fileName.toLowerCase.split('.').lastOption.getOrElse("")

    MagicNumber.detect(header) match
      case Some(sig) =>
        // For ZIP-based formats, check extension to distinguish DOCX/ODT/etc.
        if sig.mimeType == "application/zip" then
          ext match
            case "docx" => DetectedFileType(
                "Word Document (DOCX)",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
              )
            case "odt" => DetectedFileType("OpenDocument Text", "application/vnd.oasis.opendocument.text")
            case "epub" => DetectedFileType("EPUB", "application/epub+zip")
            case _ => DetectedFileType(sig.name, sig.mimeType)
        else
          DetectedFileType(sig.name, sig.mimeType)
      case None =>
        // Fall back to extension-based detection for text-based formats (no magic bytes)
        ext match
          case "txt" | "text" => DetectedFileType("Plain Text", "text/plain")
          case "md" | "markdown" => DetectedFileType("Markdown", "text/markdown")
          case "rtf" => DetectedFileType("Rich Text Format", "application/rtf")
          case "html" | "htm" => DetectedFileType("HTML", "text/html")
          case "json" => DetectedFileType("JSON", "application/json")
          case _ => DetectedFileType("Unknown", "application/octet-stream")
  }

object FileValidator:
  def apply(config: ResumeConfig): FileValidator = new FileValidator(config)
