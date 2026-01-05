package arcata.api.etl

import arcata.api.clients.ObjectStorageClient
import arcata.api.config.{AIConfig, ResumeConfig}
import arcata.api.domain.ExtractedResumeData
import arcata.api.etl.framework.*
import arcata.api.etl.steps.*

/**
 * Input for the resume parsing pipeline.
 *
 * @param fileBytes
 *   Raw file bytes from upload
 * @param fileName
 *   Original file name
 * @param claimedMimeType
 *   MIME type from upload request (may be spoofed)
 * @param profileId
 *   User's profile ID
 */
final case class ResumeParsingInput(
  fileBytes: Array[Byte],
  fileName: String,
  claimedMimeType: Option[String],
  profileId: String,
)

/**
 * Output from the resume parsing pipeline.
 *
 * @param extractedData
 *   Structured resume data ready for the Lexical editor
 * @param objectId
 *   ObjectStorage ID of the original file
 * @param fileName
 *   Original file name
 */
final case class ResumeParsingOutput(
  extractedData: ExtractedResumeData,
  objectId: String,
  fileName: String,
)

/**
 * Pipeline that parses uploaded resume files and extracts structured data.
 *
 * Steps:
 * 1. Validate file (size, type via magic bytes)
 * 2. Store file in ObjectStorage
 * 3. Extract text (PDF/DOCX/TXT)
 * 4. Extract structured data using AI
 * 5. Normalize and sanitize data
 *
 * The pipeline is synchronous - it runs all steps in sequence and returns the result. This is
 * appropriate for resume parsing since it's a user-initiated action that needs immediate feedback.
 */
final class ResumeParsingPipeline(
  resumeConfig: ResumeConfig,
  aiConfig: AIConfig,
  storageClient: ObjectStorageClient,
  progressEmitter: ProgressEmitter = ProgressEmitter.noop,
) extends BasePipeline[ResumeParsingInput, ResumeParsingOutput]:

  val name = "ResumeParsingPipeline"

  // Initialize steps
  private val fileValidator = FileValidator(resumeConfig)
  private val fileStorer = FileStorer(storageClient)
  private val textExtractor = TextExtractor()
  private val resumeExtractor = ResumeExtractor(aiConfig)

  override def execute(
    input: ResumeParsingInput,
    ctx: PipelineContext,
  ): Either[StepError, ResumeParsingOutput] = {
    val totalSteps = 5

    progressEmitter.emit(1, totalSteps, "validating", "Validating file...")

    val result = for {
      // Step 1: Validate file
      validatorOutput <- fileValidator.run(
        FileValidatorInput(
          fileBytes = input.fileBytes,
          fileName = input.fileName,
          claimedMimeType = input.claimedMimeType,
        ),
        ctx,
      )

      // Step 2: Store file
      storerOutput <- {
        progressEmitter.emit(2, totalSteps, "storing", "Uploading file...")
        fileStorer.run(
          FileStorerInput(
            fileBytes = validatorOutput.fileBytes,
            fileName = validatorOutput.fileName,
            detectedType = validatorOutput.detectedType,
            profileId = input.profileId,
          ),
          ctx,
        )
      }

      // Step 3: Extract text
      textOutput <- {
        progressEmitter.emit(3, totalSteps, "extracting_text", "Reading document...")
        textExtractor.run(
          TextExtractorInput(
            fileBytes = storerOutput.fileBytes,
            fileName = storerOutput.fileName,
            detectedType = storerOutput.detectedType,
            objectId = storerOutput.objectId,
          ),
          ctx,
        )
      }

      // Step 4: Extract structured data using AI
      extractorOutput <- {
        progressEmitter.emit(4, totalSteps, "extracting_data", "Analyzing resume...")
        resumeExtractor.run(
          ResumeExtractorInput(
            text = textOutput.text,
            fileName = textOutput.fileName,
            objectId = textOutput.objectId,
          ),
          ctx,
        )
      }

      // Step 5: Normalize data
      normalizerOutput <- {
        progressEmitter.emit(5, totalSteps, "normalizing", "Finalizing data...")
        ResumeDataNormalizer.run(
          ResumeDataNormalizerInput(
            extractedData = extractorOutput.extractedData,
            fileName = extractorOutput.fileName,
            objectId = extractorOutput.objectId,
          ),
          ctx,
        )
      }
    } yield ResumeParsingOutput(
      extractedData = normalizerOutput.normalizedData,
      objectId = normalizerOutput.objectId,
      fileName = normalizerOutput.fileName,
    )

    result match
      case Right(_) =>
        progressEmitter.emit(totalSteps, totalSteps, "complete", "Resume parsed successfully")
      case Left(err) =>
        progressEmitter.emit(0, totalSteps, "error", err.message)

    result
  }

object ResumeParsingPipeline:
  def apply(
    resumeConfig: ResumeConfig,
    aiConfig: AIConfig,
    storageClient: ObjectStorageClient,
    progressEmitter: ProgressEmitter = ProgressEmitter.noop,
  ): ResumeParsingPipeline =
    new ResumeParsingPipeline(resumeConfig, aiConfig, storageClient, progressEmitter)
