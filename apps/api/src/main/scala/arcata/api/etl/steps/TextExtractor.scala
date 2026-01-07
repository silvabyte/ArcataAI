package arcata.api.etl.steps

import java.io.ByteArrayInputStream
import java.nio.file.Files

import scala.util.{Failure, Success, Try, Using}

import arcata.api.etl.framework.*
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.jsoup.Jsoup

/**
 * Input for the TextExtractor step.
 *
 * @param fileBytes
 *   File bytes to extract text from
 * @param fileName
 *   Original file name
 * @param detectedType
 *   Detected file type (determines extraction method)
 * @param objectId
 *   ObjectStorage ID (passed through)
 */
final case class TextExtractorInput(
  fileBytes: Array[Byte],
  fileName: String,
  detectedType: DetectedFileType,
  objectId: String,
)

/**
 * Output from the TextExtractor step.
 *
 * @param text
 *   Extracted plain text content
 * @param fileName
 *   Original file name (passed through)
 * @param objectId
 *   ObjectStorage ID (passed through)
 * @param pageCount
 *   Number of pages (for PDF) or None for other formats
 */
final case class TextExtractorOutput(
  text: String,
  fileName: String,
  objectId: String,
  pageCount: Option[Int] = None,
)

/**
 * ETL step that extracts plain text from resume documents.
 *
 * Supports:
 * - PDF: Uses OpenPDF (lightweight, no logging deps)
 * - DOC/DOCX: Uses pandoc CLI (external tool)
 * - RTF: Uses pandoc CLI (external tool)
 * - HTML: Uses Jsoup for HTML parsing
 * - TXT/Markdown: Passes through as-is (UTF-8 decoded)
 */
final class TextExtractor extends BaseStep[TextExtractorInput, TextExtractorOutput]:

  val name = "TextExtractor"

  override def execute(
    input: TextExtractorInput,
    ctx: PipelineContext,
  ): Either[StepError, TextExtractorOutput] = {
    logger.info(
      s"[${ctx.runId}] Extracting text from ${input.fileName} (${input.detectedType.name})"
    )

    val result = input.detectedType.mimeType match
      case "application/pdf" =>
        extractFromPdf(input.fileBytes, ctx)
      // DOCX (modern Word format)
      case "application/zip" | "application/vnd.openxmlformats-officedocument.wordprocessingml.document" =>
        extractWithPandoc(input.fileBytes, "docx", ctx)
      // DOC (legacy Word format)
      case "application/x-cfbf" =>
        extractWithPandoc(input.fileBytes, "doc", ctx)
      // RTF
      case "application/rtf" =>
        extractWithPandoc(input.fileBytes, "rtf", ctx)
      // HTML
      case "text/html" =>
        extractFromHtml(input.fileBytes, ctx)
      // Plain text formats (TXT, Markdown, JSON, etc.)
      case mime if mime.startsWith("text/") || mime == "application/json" =>
        extractFromText(input.fileBytes, ctx)
      case other =>
        Left(
          StepError.ExtractionError(
            message =
              s"Unsupported file type '${input.detectedType.name}' ($other). Supported formats: PDF, DOC, DOCX, RTF, HTML, TXT, Markdown",
            stepName = name,
          )
        )

    result.map {
      case (text, pageCount) =>
        logger.info(s"[${ctx.runId}] Extracted ${text.length} characters from ${input.fileName}")
        TextExtractorOutput(
          text = text,
          fileName = input.fileName,
          objectId = input.objectId,
          pageCount = pageCount,
        )
    }
  }

  /**
   * Extract text from PDF using OpenPDF.
   */
  private def extractFromPdf(
    bytes: Array[Byte],
    ctx: PipelineContext,
  ): Either[StepError, (String, Option[Int])] = {
    Try {
      val reader = new PdfReader(bytes)
      try
        val pageCount = reader.getNumberOfPages
        val extractor = new PdfTextExtractor(reader)
        val text = (1 to pageCount)
          .map(page => extractor.getTextFromPage(page))
          .mkString("\n")
        (text.trim, Some(pageCount))
      finally reader.close()
    } match
      case Success(result) => Right(result)
      case Failure(e) =>
        logger.error(s"[${ctx.runId}] PDF extraction failed: ${e.getMessage}")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract text from PDF: ${e.getMessage}",
            stepName = name,
            cause = Some(e),
          )
        )
  }

  /**
   * Extract text using pandoc CLI.
   * Pandoc handles DOC, DOCX, RTF, and many other formats.
   */
  private def extractWithPandoc(
    bytes: Array[Byte],
    inputFormat: String,
    ctx: PipelineContext,
  ): Either[StepError, (String, Option[Int])] = {
    // Write bytes to temp file, run pandoc, clean up
    val tempFile = Files.createTempFile("resume-", s".$inputFormat")
    try
      Files.write(tempFile, bytes)

      val result = os.proc("pandoc", "-f", inputFormat, "-t", "plain", tempFile.toString)
        .call(check = false, stderr = os.Pipe)

      if result.exitCode == 0 then Right((result.out.text().trim, None))
      else
        val errorMsg = result.err.text()
        logger.error(s"[${ctx.runId}] pandoc extraction failed: $errorMsg")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract text from $inputFormat: $errorMsg",
            stepName = name,
          )
        )
    catch
      case e: java.io.IOException if e.getMessage.contains("Cannot run program") =>
        logger.error(s"[${ctx.runId}] pandoc not found")
        Left(
          StepError.ExtractionError(
            message = "pandoc is not installed. Please install pandoc to extract text from DOC/DOCX/RTF files.",
            stepName = name,
            cause = Some(e),
          )
        )
      case e: Exception =>
        logger.error(s"[${ctx.runId}] pandoc extraction failed: ${e.getMessage}")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract text from $inputFormat: ${e.getMessage}",
            stepName = name,
            cause = Some(e),
          )
        )
    finally Files.deleteIfExists(tempFile)
  }

  /**
   * Extract text from plain text file.
   */
  private def extractFromText(
    bytes: Array[Byte],
    ctx: PipelineContext,
  ): Either[StepError, (String, Option[Int])] = {
    Try {
      val text = new String(bytes, "UTF-8")
      (text.trim, None)
    } match
      case Success(result) => Right(result)
      case Failure(e) =>
        logger.error(s"[${ctx.runId}] Text extraction failed: ${e.getMessage}")
        Left(
          StepError.ExtractionError(
            message = s"Failed to read text file: ${e.getMessage}",
            stepName = name,
            cause = Some(e),
          )
        )
  }

  /**
   * Extract text from HTML using Jsoup.
   */
  private def extractFromHtml(
    bytes: Array[Byte],
    ctx: PipelineContext,
  ): Either[StepError, (String, Option[Int])] = {
    Try {
      val html = new String(bytes, "UTF-8")
      val doc = Jsoup.parse(html)
      val text = doc.body().text()
      (text.trim, None)
    } match
      case Success(result) => Right(result)
      case Failure(e) =>
        logger.error(s"[${ctx.runId}] HTML extraction failed: ${e.getMessage}")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract text from HTML: ${e.getMessage}",
            stepName = name,
            cause = Some(e),
          )
        )
  }

object TextExtractor:
  def apply(): TextExtractor = new TextExtractor()
