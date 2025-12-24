package arcata.api.extraction

import arcata.api.config.AIConfig
import arcata.api.domain.{ExtractionConfig, ExtractionRule, ExtractionSource, MatchPattern, MatchPatternType, Transform}
import boogieloops.ai.{Agent, RequestMetadata, SchemaError}
import boogieloops.ai.providers.OpenAICompatibleProvider
import boogieloops.schema.derivation.Schematic
import boogieloops.schema.derivation.CollectionSchemas.given
import org.jsoup.Jsoup
import upickle.default.*

import scala.jdk.CollectionConverters.*
import scala.util.Try

/**
 * AI-generated extraction config in a simplified format.
 * Uses simple strings for enums so AI can easily generate valid output.
 */
final case class GeneratedConfig(
    name: String,
    matchPatterns: List[GeneratedMatchPattern],
    extractRules: Map[String, List[GeneratedRule]]
) derives ReadWriter, Schematic

final case class GeneratedMatchPattern(
    patternType: String,
    selector: Option[String] = None,
    pattern: Option[String] = None,
    contentContains: Option[String] = None
) derives ReadWriter, Schematic

final case class GeneratedRule(
    source: String,
    path: Option[String] = None,
    selector: Option[String] = None,
    name: Option[String] = None,
    pattern: Option[String] = None,
    transforms: Option[List[String]] = None
) derives ReadWriter, Schematic

/**
 * Result of config generation.
 *
 * @param config The generated extraction config
 * @param extractionResult Result from applying the config
 * @param attempts Number of attempts made
 */
case class GenerationResult(
    config: ExtractionConfig,
    extractionResult: DeterministicExtractor.ExtractionResult,
    attempts: Int
):
  def completionState: CompletionState = extractionResult.completionState
  def isSuccessful: Boolean =
    completionState == CompletionState.Complete || completionState == CompletionState.Sufficient

/**
 * Generates extraction configs using AI.
 *
 * The generator analyzes HTML structure and creates configs that can
 * deterministically extract job data. It uses a retry loop with feedback
 * to refine configs until extraction is sufficient.
 */
final class ConfigGenerator(aiConfig: AIConfig):

  private val provider = OpenAICompatibleProvider(
    baseUrl = aiConfig.baseUrl,
    apiKey = aiConfig.apiKey,
    modelId = aiConfig.model,
    strictModelValidation = false
  )

  private val agent = Agent(
    name = "ConfigGenerator",
    instructions = ConfigGenerator.SystemPrompt,
    provider = provider,
    model = aiConfig.model,
    temperature = Some(0.1) // Low temperature for consistent structured output
  )

  /**
   * Generate an extraction config for the given HTML.
   *
   * @param html Raw HTML content
   * @param url Source URL
   * @param maxAttempts Maximum attempts (default 3)
   * @return Best generation result, or error
   */
  def generate(
      html: String,
      url: String,
      maxAttempts: Int = 3
  ): Either[SchemaError, GenerationResult] = {
    // Pre-extract JSON-LD to include in prompt (since it's the most valuable data source)
    val jsonLdContent = extractJsonLd(html)
    val pageAnalysis = analyzePage(html, url, jsonLdContent)

    generateWithRetry(
      html = html,
      url = url,
      jsonLd = jsonLdContent,
      pageAnalysis = pageAnalysis,
      attempt = 1,
      maxAttempts = maxAttempts,
      previousFeedback = None,
      bestResult = None
    )
  }

  private def generateWithRetry(
      html: String,
      url: String,
      jsonLd: Option[String],
      pageAnalysis: String,
      attempt: Int,
      maxAttempts: Int,
      previousFeedback: Option[String],
      bestResult: Option[GenerationResult]
  ): Either[SchemaError, GenerationResult] = {

    val prompt = buildPrompt(url, jsonLd, pageAnalysis, previousFeedback)

    agent
      .generateObjectWithoutHistory[GeneratedConfig](prompt, RequestMetadata())
      .map(_.data) match

      case Left(error) =>
        // If AI failed but we have a previous result, return that
        bestResult.map(Right(_)).getOrElse(Left(error))

      case Right(generated) =>
        // Convert and test the config
        val extractionConfig = toExtractionConfig(generated)
        val result = DeterministicExtractor.extract(html, url, extractionConfig)
        val genResult = GenerationResult(extractionConfig, result, attempt)

        // Track best result (highest score)
        val newBest = bestResult match
          case Some(prev) if prev.extractionResult.scoringResult.score >= result.scoringResult.score =>
            prev
          case _ =>
            genResult

        // Check if good enough or out of attempts
        if genResult.isSuccessful || attempt >= maxAttempts then
          Right(newBest)
        else {
          // Build feedback and retry
          val feedback = buildFeedback(result, attempt)
          generateWithRetry(
            html = html,
            url = url,
            jsonLd = jsonLd,
            pageAnalysis = pageAnalysis,
            attempt = attempt + 1,
            maxAttempts = maxAttempts,
            previousFeedback = Some(feedback),
            bestResult = Some(newBest)
          )
        }
  }

  /**
   * Extract JSON-LD content from HTML for inclusion in prompt.
   */
  private def extractJsonLd(html: String): Option[String] = {
    Try {
      val doc = Jsoup.parse(html)
      val scripts = doc.select("script[type='application/ld+json']").asScala
      val jsonLdContents = scripts.map(_.html()).filter(_.contains("JobPosting"))
      if jsonLdContents.nonEmpty then Some(jsonLdContents.mkString("\n\n"))
      else None
    }.getOrElse(None)
  }

  /**
   * Analyze page structure to help AI understand what's available.
   */
  private def analyzePage(html: String, url: String, jsonLd: Option[String]): String = {
    val doc = Try(Jsoup.parse(html)).getOrElse(return "Could not parse HTML")

    val hasJsonLd = jsonLd.isDefined
    val metaTags = doc.select("meta[name], meta[property]").asScala
      .map(m => s"${m.attr("name")}${m.attr("property")}: ${m.attr("content").take(50)}")
      .take(10)
      .mkString("\n  ")

    val mainContent = doc.select("main, article, [role=main], .job-description, .job-details")
      .asScala.headOption.map(_.text().take(500)).getOrElse("")

    s"""Page Analysis:
- URL: $url
- Has JSON-LD JobPosting: $hasJsonLd
- Meta tags found:
  $metaTags
- Main content preview: ${mainContent.take(200)}..."""
  }

  private def buildPrompt(
      url: String,
      jsonLd: Option[String],
      pageAnalysis: String,
      feedback: Option[String]
  ): String = {
    val jsonLdSection = jsonLd.map(j => s"""
## JSON-LD Data (PRIMARY SOURCE - use this first!)
```json
$j
```
""").getOrElse("## No JSON-LD found - use CSS selectors and meta tags")

    val feedbackSection = feedback.map(f => s"""
## IMPORTANT: Previous Attempt Failed
$f

You MUST fix the issues mentioned above. Try different selectors or paths.
""").getOrElse("")

    s"""Generate an extraction config for this job posting page.

$pageAnalysis
$jsonLdSection
$feedbackSection

Remember:
- If JSON-LD exists with JobPosting, use jsonld source with JSONPath (e.g., $$.title, $$.description)
- For nested JSON-LD: $$.hiringOrganization.name, $$.baseSalary.value.minValue
- Provide fallback rules using css or meta sources
- The config will be reused for similar pages from this site"""
  }

  private def buildFeedback(result: DeterministicExtractor.ExtractionResult, attempt: Int): String = {
    val score = result.scoringResult
    val missing = score.missingRequired ++ score.missingOptional.take(5)
    val present = score.presentFields

    val failureDetails = result.failedRules.toSeq.sortBy(_._1).map { case (field, errors) =>
      s"  $field: ${errors.head}"
    }.mkString("\n")

    s"""Attempt $attempt scored ${score.scorePercent} (${score.state}).

Extracted successfully: ${present.mkString(", ")}
MISSING fields: ${missing.mkString(", ")}

${if failureDetails.nonEmpty then s"Extraction failures:\n$failureDetails" else ""}

FIX THE CONFIG to extract the missing fields."""
  }

  private def toExtractionConfig(generated: GeneratedConfig): ExtractionConfig = {
    val matchPatterns = generated.matchPatterns.flatMap { mp =>
      val patternType = mp.patternType.toLowerCase.replace("-", "_") match
        case "css_exists" | "cssexists" => Some(MatchPatternType.CssExists)
        case "url_pattern" | "urlpattern" => Some(MatchPatternType.UrlPattern)
        case "content_contains" | "contentcontains" => Some(MatchPatternType.ContentContains)
        case _ => None

      patternType.map(pt => {
        MatchPattern(
          patternType = pt,
          selector = mp.selector,
          pattern = mp.pattern,
          contentContains = mp.contentContains
        )
      })
    }

    val extractRules = generated.extractRules.map { case (field, rules) =>
      val convertedRules = rules.flatMap { rule =>
        val source = rule.source.toLowerCase.replace("-", "_").replace(" ", "") match
          case "jsonld" | "json_ld" | "ld+json" => Some(ExtractionSource.JsonLd)
          case "css" => Some(ExtractionSource.Css)
          case "meta" => Some(ExtractionSource.Meta)
          case "regex" => Some(ExtractionSource.Regex)
          case _ => None

        source.map { s =>
          val transforms = rule.transforms.getOrElse(Seq.empty).flatMap { t =>
            t.toLowerCase.replace("-", "_").replace(" ", "") match
              case "html_decode" | "htmldecode" | "decode" => Some(Transform.HtmlDecode)
              case "inner_text" | "innertext" | "text" => Some(Transform.InnerText)
              case "parse_number" | "parsenumber" | "number" => Some(Transform.ParseNumber)
              case _ => None
          }

          ExtractionRule(
            source = s,
            path = rule.path,
            selector = rule.selector,
            name = rule.name,
            pattern = rule.pattern,
            transforms = transforms
          )
        }
      }
      field -> convertedRules
    }.filter(_._2.nonEmpty)

    // Ensure we have at least one match pattern
    val finalPatterns = if matchPatterns.isEmpty then
      Seq(MatchPattern.contentContains("job"))
    else matchPatterns

    ExtractionConfig.create(
      name = generated.name,
      matchPatterns = finalPatterns,
      extractRules = extractRules
    )
  }

object ConfigGenerator:
  def apply(config: AIConfig): ConfigGenerator = new ConfigGenerator(config)

  val SystemPrompt: String =
    """You generate extraction configs for job posting pages. Your config will be used to deterministically extract job data without AI.

OUTPUT FORMAT - Generate a JSON object with:
{
  "name": "Site Name - Extraction Type",
  "matchPatterns": [...],
  "extractRules": {...}
}

MATCH PATTERNS - Identify pages this config applies to:
- {"patternType": "css_exists", "selector": "script[type='application/ld+json']", "contentContains": "JobPosting"}
- {"patternType": "url_pattern", "pattern": ".*\\.greenhouse\\.io/.*"}
- {"patternType": "content_contains", "contentContains": "Apply Now"}

EXTRACT RULES - Map field names to extraction rules (tried in order):
{
  "title": [
    {"source": "jsonld", "path": "$.title"},
    {"source": "css", "selector": "h1.job-title"}
  ],
  "companyName": [
    {"source": "jsonld", "path": "$.hiringOrganization.name"},
    {"source": "meta", "name": "og:site_name"}
  ],
  "salaryMin": [
    {"source": "jsonld", "path": "$.baseSalary.value.minValue"},
    {"source": "css", "selector": ".salary", "transforms": ["parse_number"]}
  ]
}

SOURCES (in preference order):
1. jsonld - Extract from JSON-LD using JSONPath. Examples:
   - $.title
   - $.description
   - $.hiringOrganization.name
   - $.jobLocation.address.addressLocality
   - $.baseSalary.value.minValue
   - $.baseSalary.value.maxValue
2. css - Extract text from CSS selector
3. meta - Extract from <meta name="X"> or <meta property="X">
4. regex - Apply regex to element (needs selector + pattern with capture group)

TRANSFORMS (optional, applied after extraction):
- html_decode: Decode &amp; etc.
- inner_text: Strip HTML tags
- parse_number: Extract numeric value only

FIELDS TO EXTRACT:
- title (REQUIRED)
- companyName (REQUIRED)
- description (REQUIRED)
- location
- salaryMin, salaryMax
- qualifications
- responsibilities
- benefits
- jobType
- experienceLevel

CRITICAL RULES:
1. ALWAYS check for JSON-LD first - it's the most reliable source
2. Provide multiple rules per field as fallbacks
3. For JSON-LD paths, use $ prefix: $.title NOT title
4. Be specific with CSS selectors to avoid wrong matches
5. Name configs descriptively: "Netflix Careers - JSON-LD" not "Config 1"
"""
