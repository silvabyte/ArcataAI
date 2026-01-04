package arcata.api.etl.steps

import java.io.ByteArrayInputStream

import scala.util.{Failure, Success, Try, Using}

import arcata.api.etl.framework.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument

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
    objectId: String
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
    pageCount: Option[Int] = None
)

/**
 * ETL step that extracts plain text from resume documents.
 *
 * Supports:
 * - PDF: Uses Apache PDFBox for text extraction
 * - DOCX: Uses Apache POI for text extraction
 * - TXT: Passes through as-is (UTF-8 decoded)
 */
final class TextExtractor extends BaseStep[TextExtractorInput, TextExtractorOutput]:

  val name = "TextExtractor"

  override def execute(
      input: TextExtractorInput,
      ctx: PipelineContext
  ): Either[StepError, TextExtractorOutput] = {
    logger.info(
      s"[${ctx.runId}] Extracting text from ${input.fileName} (${input.detectedType.name})"
    )

    val result = input.detectedType.mimeType match
      case "application/pdf" => extractFromPdf(input.fileBytes, ctx)
      case "application/zip" => extractFromDocx(input.fileBytes, ctx)
      case "text/plain"      => extractFromText(input.fileBytes, ctx)
      case other =>
        Left(
          StepError.ExtractionError(
            message = s"Unsupported file type for text extraction: $other",
            stepName = name
          )
        )

    result.map { case (text, pageCount) =>
      logger.info(s"[${ctx.runId}] Extracted ${text.length} characters from ${input.fileName}")
      TextExtractorOutput(
        text = text,
        fileName = input.fileName,
        objectId = input.objectId,
        pageCount = pageCount
      )
    }
  }

  /**
   * Extract text from PDF using PDFBox.
   */
  private def extractFromPdf(
      bytes: Array[Byte],
      ctx: PipelineContext
  ): Either[StepError, (String, Option[Int])] = {
    Try {
      Using.resource(Loader.loadPDF(bytes)) { document =>
        val stripper = new PDFTextStripper()
        val text = stripper.getText(document)
        val pageCount = document.getNumberOfPages
        (text.trim, Some(pageCount))
      }
    } match
      case Success(result) => Right(result)
      case Failure(e) =>
        logger.error(s"[${ctx.runId}] PDF extraction failed: ${e.getMessage}")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract text from PDF: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )
  }

  /**
   * Extract text from DOCX using Apache POI.
   */
  private def extractFromDocx(
      bytes: Array[Byte],
      ctx: PipelineContext
  ): Either[StepError, (String, Option[Int])] = {
    Try {
      Using.resource(new ByteArrayInputStream(bytes)) { bis =>
        Using.resource(new XWPFDocument(bis)) { document =>
          Using.resource(new XWPFWordExtractor(document)) { extractor =>
            val text = extractor.getText
            (text.trim, None)
          }
        }
      }
    } match
      case Success(result) => Right(result)
      case Failure(e) =>
        logger.error(s"[${ctx.runId}] DOCX extraction failed: ${e.getMessage}")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract text from DOCX: ${e.getMessage}",
            stepName = name,
            cause = Some(e)
          )
        )
  }

  /**
   * Extract text from plain text file.
   */
  private def extractFromText(
      bytes: Array[Byte],
      ctx: PipelineContext
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
            cause = Some(e)
          )
        )
  }

object TextExtractor:
  def apply(): TextExtractor = new TextExtractor()
