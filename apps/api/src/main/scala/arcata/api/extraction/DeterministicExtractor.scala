package arcata.api.extraction

import arcata.api.domain.{ExtractionConfig, ExtractionRule, ExtractionSource, ExtractedJobData, Transform}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.matching.Regex

/**
 * Extracts job data from HTML using extraction rules.
 *
 * Given an HTML page and an ExtractionConfig, applies the rules deterministically
 * to extract structured job data. Each field tries multiple rules in order until
 * one succeeds.
 */
object DeterministicExtractor:

  /**
   * Result of extraction including the data and scoring.
   *
   * @param data
   *   Extracted job data
   * @param scoringResult
   *   Completeness scoring
   * @param failedRules
   *   Rules that failed (for debugging)
   */
  case class ExtractionResult(
      data: ExtractedJobData,
      scoringResult: ScoringResult,
      failedRules: Map[String, Seq[String]] = Map.empty
  ):
    def completionState: CompletionState = scoringResult.state

  /**
   * Extract job data from HTML using the given config.
   *
   * @param html
   *   HTML content to extract from
   * @param url
   *   URL of the page (for base URL resolution)
   * @param config
   *   Extraction configuration with rules
   * @return
   *   Extraction result with data and scoring
   */
  def extract(html: String, url: String, config: ExtractionConfig): ExtractionResult = {
    val doc = Jsoup.parse(html, url)
    val jsonLdData = extractJsonLd(doc)

    val extractedFields = scala.collection.mutable.Map[String, String]()
    val failedRules = scala.collection.mutable.Map[String, Seq[String]]()

    // Extract each field using the config rules
    for (fieldName, rules) <- config.extractRules do
      val (value, errors) = tryRules(doc, jsonLdData, rules)
      value match
        case Some(v) => extractedFields(fieldName) = v
        case None => if errors.nonEmpty then failedRules(fieldName) = errors

    // Build ExtractedJobData from extracted fields
    val data = buildJobData(extractedFields.toMap)

    // Score the extraction
    val fieldMap = buildScoringFields(extractedFields.toMap)
    val scoringResult = CompletionScorer.score(fieldMap)

    ExtractionResult(data, scoringResult, failedRules.toMap)
  }

  /**
   * Try multiple rules in order until one succeeds.
   *
   * @param doc
   *   Parsed HTML document
   * @param jsonLdData
   *   Pre-extracted JSON-LD data (if any)
   * @param rules
   *   Rules to try
   * @return
   *   Extracted value (if any) and list of error messages
   */
  private def tryRules(
      doc: Document,
      jsonLdData: Option[ujson.Value],
      rules: Seq[ExtractionRule]
  ): (Option[String], Seq[String]) = {
    // Try rules in order until one succeeds
    @scala.annotation.tailrec
    def tryNext(remaining: Seq[ExtractionRule], errors: Seq[String]): (Option[String], Seq[String]) = {
      remaining match
        case Nil => (None, errors)
        case rule +: rest =>
          applyRule(doc, jsonLdData, rule) match
            case Right(value) if value.trim.nonEmpty =>
              (Some(value), Seq.empty)
            case Right(_) =>
              tryNext(rest, errors :+ s"${rule.source}: returned empty value")
            case Left(error) =>
              tryNext(rest, errors :+ s"${rule.source}: $error")
    }

    tryNext(rules, Seq.empty)
  }

  /**
   * Apply a single extraction rule.
   *
   * @param doc
   *   Parsed HTML document
   * @param jsonLdData
   *   Pre-extracted JSON-LD data
   * @param rule
   *   Rule to apply
   * @return
   *   Either an error message or the extracted value
   */
  private def applyRule(
      doc: Document,
      jsonLdData: Option[ujson.Value],
      rule: ExtractionRule
  ): Either[String, String] = {
    val rawValue = rule.source match
      case ExtractionSource.JsonLd =>
        rule.path match
          case Some(path) =>
            jsonLdData match
              case Some(json) => extractJsonPath(json, path)
              case None => Left("No JSON-LD data found")
          case None => Left("No JSONPath specified")

      case ExtractionSource.Css =>
        rule.selector match
          case Some(selector) =>
            Try(doc.select(selector).first())
              .map(Option(_))
              .getOrElse(None) match
              case Some(el) => Right(el.text())
              case None => Left(s"Selector '$selector' matched no elements")
          case None => Left("No CSS selector specified")

      case ExtractionSource.Meta =>
        rule.name match
          case Some(name) =>
            val meta = doc.select(s"meta[name='$name'], meta[property='$name']").first()
            Option(meta).map(_.attr("content")) match
              case Some(content) => Right(content)
              case None => Left(s"Meta tag '$name' not found")
          case None => Left("No meta tag name specified")

      case ExtractionSource.Regex =>
        (rule.selector, rule.pattern) match
          case (Some(selector), Some(pattern)) =>
            Try(doc.select(selector).first())
              .map(Option(_))
              .getOrElse(None) match
              case Some(el) =>
                val text = el.text()
                Try(Regex(pattern).findFirstMatchIn(text).map(_.group(1)))
                  .getOrElse(None) match
                  case Some(captured) => Right(captured)
                  case None => Left(s"Regex '$pattern' did not match")
              case None => Left(s"Selector '$selector' matched no elements")
          case (None, _) => Left("No selector specified for regex extraction")
          case (_, None) => Left("No regex pattern specified")

    // Apply transforms
    rawValue.flatMap(value => applyTransforms(value, rule.transforms))
  }

  /**
   * Apply a sequence of transforms to a value.
   */
  private def applyTransforms(value: String, transforms: Seq[Transform]): Either[String, String] = {
    transforms.foldLeft(Right(value): Either[String, String]) { (acc, transform) =>
      acc.flatMap(v => applyTransform(v, transform))
    }
  }

  /**
   * Apply a single transform.
   */
  private def applyTransform(value: String, transform: Transform): Either[String, String] = {
    transform match
      case Transform.HtmlDecode =>
        Right(org.jsoup.parser.Parser.unescapeEntities(value, false))

      case Transform.InnerText =>
        Right(Jsoup.parse(value).text())

      case Transform.ParseNumber =>
        val digitsOnly = value.replaceAll("[^0-9.]", "")
        if digitsOnly.nonEmpty then Right(digitsOnly)
        else Left("No numeric value found")
  }

  /**
   * Extract JSON-LD data from the document, specifically finding JobPosting schema.
   *
   * Pages may have multiple JSON-LD blocks (e.g., WebSite, Organization, JobPosting).
   * We need to find the one with @type: "JobPosting".
   */
  private def extractJsonLd(doc: Document): Option[ujson.Value] = {
    val scripts = doc.select("script[type='application/ld+json']").asScala

    val allJsonLd = scripts.flatMap { script =>
      Try(ujson.read(script.html())).toOption
    }

    // Find JobPosting specifically, or fall back to first block
    allJsonLd
      .find(isJobPosting)
      .orElse(allJsonLd.headOption)
  }

  /**
   * Check if a JSON-LD block is a JobPosting.
   * Handles both string @type and array @type.
   */
  private def isJobPosting(json: ujson.Value): Boolean = {
    Try {
      json("@type") match
        case s: ujson.Str => s.str == "JobPosting"
        case arr: ujson.Arr => arr.value.exists {
            case s: ujson.Str => s.str == "JobPosting"
            case _ => false
          }
        case _ => false
    }.getOrElse(false)
  }

  /**
   * Extract a value from JSON using a JSONPath-like expression.
   * Uses our JsonPathTraverser utility.
   */
  private def extractJsonPath(json: ujson.Value, path: String): Either[String, String] = {
    JsonPathTraverser.get(json, path) match
      case Some(v) => jsonValueToString(v)
      case None => Left(s"JSONPath '$path' matched nothing")
  }

  private def jsonValueToString(v: ujson.Value): Either[String, String] = {
    v match
      case s: ujson.Str => Right(s.str)
      case n: ujson.Num => Right(n.num.toString)
      case b: ujson.Bool => Right(b.bool.toString)
      case arr: ujson.Arr => Right(arr.value.map(_.toString).mkString(", "))
      case obj: ujson.Obj => Right(ujson.write(obj))
      case ujson.Null => Left("JSONPath returned null")
  }

  /** Parse a string as Int, handling both "100000" and "100000.0" formats */
  private def parseIntOrDouble(s: String): Option[Int] = {
    Try(s.toInt).toOption
      .orElse(Try(s.toDouble.toInt).toOption)
  }

  /**
   * Build ExtractedJobData from extracted fields map.
   */
  private def buildJobData(fields: Map[String, String]): ExtractedJobData = {
    ExtractedJobData(
      title = fields.getOrElse("title", "Unknown Title"),
      companyName = fields.get("companyName"),
      description = fields.get("description"),
      location = fields.get("location"),
      jobType = fields.get("jobType"),
      experienceLevel = fields.get("experienceLevel"),
      educationLevel = fields.get("educationLevel"),
      salaryMin = fields.get("salaryMin").flatMap(parseIntOrDouble),
      salaryMax = fields.get("salaryMax").flatMap(parseIntOrDouble),
      salaryCurrency = fields.get("salaryCurrency"),
      qualifications = fields.get("qualifications").map(_.split(",").map(_.trim).toList),
      preferredQualifications = fields.get("preferredQualifications").map(_.split(",").map(_.trim).toList),
      responsibilities = fields.get("responsibilities").map(_.split(",").map(_.trim).toList),
      benefits = fields.get("benefits").map(_.split(",").map(_.trim).toList),
      category = fields.get("category"),
      applicationUrl = fields.get("applicationUrl"),
      isRemote = fields.get("isRemote").map(v => {
        v.equalsIgnoreCase("true") ||
          v.equalsIgnoreCase("TELECOMMUTE") ||
          v.toLowerCase.contains("remote")
      }),
      postedDate = fields.get("postedDate"),
      closingDate = fields.get("closingDate")
    )
  }

  /**
   * Build scoring fields map from extracted fields.
   */
  private def buildScoringFields(fields: Map[String, String]): Map[String, Option[String]] = {
    Map(
      "title" -> fields.get("title"),
      "companyName" -> fields.get("companyName"),
      "description" -> fields.get("description"),
      "location" -> fields.get("location"),
      "salaryMin" -> fields.get("salaryMin"),
      "salaryMax" -> fields.get("salaryMax"),
      "qualifications" -> fields.get("qualifications"),
      "responsibilities" -> fields.get("responsibilities"),
      "benefits" -> fields.get("benefits"),
      "jobType" -> fields.get("jobType"),
      "experienceLevel" -> fields.get("experienceLevel")
    )
  }
