package arcata.api.etl.steps

import arcata.api.clients.SupabaseClient
import arcata.api.config.AIConfig
import arcata.api.domain.{ExtractedJobData, ExtractionConfig}
import arcata.api.etl.framework.*
import arcata.api.extraction.{CompletionState, ConfigGenerator, ConfigMatcher, DeterministicExtractor, GenerationResult}

/** Input for the JobExtractor step. */
final case class JobExtractorInput(
    html: String,
    url: String,
    objectId: Option[String]
)

/** Output from the JobExtractor step. */
final case class JobExtractorOutput(
    extractedData: ExtractedJobData,
    url: String,
    objectId: Option[String],
    completionState: CompletionState,
    usedConfig: Option[ExtractionConfig],
    wasConfigGenerated: Boolean
)

/**
 * Extracts job data from HTML using config-driven extraction.
 *
 * Flow:
 * 1. Try to match existing configs from database
 * 2. If config matches, apply it with DeterministicExtractor
 * 3. If no config or extraction insufficient, use AI to generate config
 * 4. Save new config to database for future use
 *
 * This step replaces the old HtmlCleaner + JobParser combination.
 */
final class JobExtractor(
    supabaseClient: SupabaseClient,
    aiConfig: AIConfig
) extends BaseStep[JobExtractorInput, JobExtractorOutput]:

  val name = "JobExtractor"

  private val configGenerator = ConfigGenerator(aiConfig)

  override def execute(
      input: JobExtractorInput,
      ctx: PipelineContext
  ): Either[StepError, JobExtractorOutput] = {
    logger.info(s"[${ctx.runId}] Extracting job data from: ${input.url}")

    // Step 1: Load existing configs and try to match
    val existingConfigs = supabaseClient.getAllExtractionConfigs()
    val matchedConfig = ConfigMatcher.findMatch(input.html, input.url, existingConfigs)

    matchedConfig match
      case Some(config) =>
        // Step 2a: Apply matched config
        logger.info(s"[${ctx.runId}] Found matching config: ${config.name}")
        val result = DeterministicExtractor.extract(input.html, input.url, config)

        if result.completionState == CompletionState.Complete ||
          result.completionState == CompletionState.Sufficient
        then
          // Config worked well
          logger.info(s"[${ctx.runId}] Config extraction successful: ${result.completionState}")
          Right(
            JobExtractorOutput(
              extractedData = result.data,
              url = input.url,
              objectId = input.objectId,
              completionState = result.completionState,
              usedConfig = Some(config),
              wasConfigGenerated = false
            )
          )
        else {
          // Config didn't work well enough, try AI
          logger.info(s"[${ctx.runId}] Config extraction insufficient (${result.completionState}), trying AI")
          generateConfigWithAI(input, ctx)
        }

      case None =>
        // Step 2b: No matching config, use AI to generate one
        logger.info(s"[${ctx.runId}] No matching config found, generating with AI")
        generateConfigWithAI(input, ctx)
  }

  private def generateConfigWithAI(
      input: JobExtractorInput,
      ctx: PipelineContext
  ): Either[StepError, JobExtractorOutput] = {
    configGenerator.generate(input.html, input.url) match
      case Right(genResult) =>
        logger.info(
          s"[${ctx.runId}] AI generated config '${genResult.config.name}' " +
            s"with ${genResult.completionState} after ${genResult.attempts} attempts"
        )

        // Save the generated config to database
        val savedConfig = supabaseClient.insertExtractionConfig(genResult.config)
        savedConfig match
          case Some(saved) =>
            logger.info(s"[${ctx.runId}] Saved new config with id=${saved.id}")
          case None =>
            logger.warn(s"[${ctx.runId}] Failed to save config to database")

        Right(
          JobExtractorOutput(
            extractedData = genResult.extractionResult.data,
            url = input.url,
            objectId = input.objectId,
            completionState = genResult.completionState,
            usedConfig = savedConfig.orElse(Some(genResult.config)),
            wasConfigGenerated = true
          )
        )

      case Left(schemaError) =>
        val errorMessage = schemaError.toString
        logger.error(s"[${ctx.runId}] AI config generation failed: $errorMessage")
        Left(
          StepError.TransformationError(
            message = s"Config generation failed: $errorMessage",
            stepName = name,
            cause = Some(new Exception(errorMessage))
          )
        )
  }

object JobExtractor:
  def apply(supabaseClient: SupabaseClient, aiConfig: AIConfig): JobExtractor =
    new JobExtractor(supabaseClient, aiConfig)
