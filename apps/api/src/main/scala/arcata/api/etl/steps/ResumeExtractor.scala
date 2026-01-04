package arcata.api.etl.steps

import arcata.api.ai.ResumeExtractionAgent
import arcata.api.config.AIConfig
import arcata.api.domain.ExtractedResumeData
import arcata.api.etl.framework.*

/**
 * Input for the ResumeExtractor step.
 *
 * @param text
 *   Plain text content extracted from the resume
 * @param fileName
 *   Original file name (used as context for AI)
 * @param objectId
 *   ObjectStorage ID (passed through)
 */
final case class ResumeExtractorInput(
    text: String,
    fileName: String,
    objectId: String
)

/**
 * Output from the ResumeExtractor step.
 *
 * @param extractedData
 *   Structured resume data extracted by AI
 * @param fileName
 *   Original file name (passed through)
 * @param objectId
 *   ObjectStorage ID (passed through)
 */
final case class ResumeExtractorOutput(
    extractedData: ExtractedResumeData,
    fileName: String,
    objectId: String
)

/**
 * ETL step that extracts structured resume data using AI.
 *
 * Uses the ResumeExtractionAgent to parse plain text and extract structured data matching the
 * ExtractedResumeData schema. This data can then be normalized and converted to the Lexical editor
 * format.
 */
final class ResumeExtractor(aiConfig: AIConfig) extends BaseStep[ResumeExtractorInput, ResumeExtractorOutput]:

  val name = "ResumeExtractor"

  private lazy val agent = ResumeExtractionAgent(aiConfig)

  override def execute(
      input: ResumeExtractorInput,
      ctx: PipelineContext
  ): Either[StepError, ResumeExtractorOutput] = {
    logger.info(s"[${ctx.runId}] Extracting structured data from ${input.fileName}")

    // Check if text is too short to be a valid resume
    if input.text.trim.length < 50 then
      return Left(
        StepError.ValidationError(
          message = "Extracted text is too short to be a valid resume (less than 50 characters)",
          stepName = name
        )
      )

    agent.extract(input.text, input.fileName) match
      case Left(schemaError) =>
        val errorMessage = schemaError.toString
        logger.error(s"[${ctx.runId}] AI extraction failed: $errorMessage")
        Left(
          StepError.ExtractionError(
            message = s"Failed to extract resume data: $errorMessage",
            stepName = name,
            cause = Some(new Exception(errorMessage))
          )
        )

      case Right(extracted) =>
        // Log what was extracted
        val contactName = extracted.contact.flatMap(_.fullName).getOrElse("unknown")
        val expCount = extracted.experience.map(_.size).getOrElse(0)
        val eduCount = extracted.education.map(_.size).getOrElse(0)
        val skillCount = extracted.skills.map(_.size).getOrElse(0)
        val customCount = extracted.customSections.map(_.size).getOrElse(0)

        logger.info(
          s"[${ctx.runId}] Extracted: contact=$contactName, experience=$expCount, " +
            s"education=$eduCount, skills=$skillCount, customSections=$customCount"
        )

        Right(
          ResumeExtractorOutput(
            extractedData = extracted,
            fileName = input.fileName,
            objectId = input.objectId
          )
        )
  }

object ResumeExtractor:
  def apply(aiConfig: AIConfig): ResumeExtractor = new ResumeExtractor(aiConfig)
