package arcata.api.etl.steps

import arcata.api.etl.framework.*
import arcata.api.utils.HtmlCleaner as HtmlCleanerUtil

/** Input for the HtmlCleaner step. */
final case class HtmlCleanerInput(
  html: String,
  url: String,
  objectId: Option[String],
)

/** Output from the HtmlCleaner step. */
final case class HtmlCleanerOutput(
  markdown: String,
  url: String,
  objectId: Option[String],
)

/**
 * Cleans HTML and converts to Markdown for AI processing.
 *
 * This step removes irrelevant content (scripts, styles, etc.) and converts to Markdown to reduce
 * token count.
 */
final class HtmlCleaner extends BaseStep[HtmlCleanerInput, HtmlCleanerOutput]:

  val name = "HtmlCleaner"

  override def execute(
    input: HtmlCleanerInput,
    ctx: PipelineContext,
  ): Either[StepError, HtmlCleanerOutput] = {
    val markdown = HtmlCleanerUtil.toMarkdown(input.html)

    Right(
      HtmlCleanerOutput(
        markdown = markdown,
        url = input.url,
        objectId = input.objectId,
      )
    )
  }

object HtmlCleaner:
  def apply(): HtmlCleaner = new HtmlCleaner()
