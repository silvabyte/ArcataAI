package arcata.api.domain

import upickle.default.*

/**
 * Match pattern types for identifying which pages a config applies to.
 */
enum MatchPatternType derives ReadWriter:
  /** CSS selector must match at least one element */
  case CssExists

  /** URL must match the regex pattern */
  case UrlPattern

  /** Page content must contain the specified string */
  case ContentContains

/**
 * A single pattern for matching pages.
 *
 * @param patternType
 *   Type of pattern matching to use
 * @param selector
 *   CSS selector (for CssExists type)
 * @param pattern
 *   Regex pattern (for UrlPattern and ContentContains types)
 * @param contentContains
 *   Optional content filter (for CssExists - e.g., require JSON-LD to contain "JobPosting")
 */
final case class MatchPattern(
    patternType: MatchPatternType,
    selector: Option[String] = None,
    pattern: Option[String] = None,
    contentContains: Option[String] = None
) derives ReadWriter

object MatchPattern:
  /** Create a CSS exists pattern */
  def cssExists(selector: String, contentContains: Option[String] = None): MatchPattern =
    MatchPattern(MatchPatternType.CssExists, selector = Some(selector), contentContains = contentContains)

  /** Create a URL pattern */
  def urlPattern(regex: String): MatchPattern =
    MatchPattern(MatchPatternType.UrlPattern, pattern = Some(regex))

  /** Create a content contains pattern */
  def contentContains(text: String): MatchPattern =
    MatchPattern(MatchPatternType.ContentContains, contentContains = Some(text))

/**
 * Extraction source types.
 */
enum ExtractionSource derives ReadWriter:
  /** Extract from JSON-LD script tags */
  case JsonLd

  /** Extract from CSS selector */
  case Css

  /** Extract from meta tags */
  case Meta

  /** Extract using regex pattern */
  case Regex

/**
 * Transform to apply after extraction.
 */
enum Transform derives ReadWriter:
  /** Decode HTML entities (&amp; -> &) */
  case HtmlDecode

  /** Get text content, strip tags */
  case InnerText

  /** Remove non-numeric chars, parse as number */
  case ParseNumber

/**
 * A single extraction rule for a field.
 *
 * @param source
 *   Where to extract from (jsonld, css, meta, regex)
 * @param path
 *   JSONPath expression (for JsonLd source)
 * @param selector
 *   CSS selector (for Css and Regex sources)
 * @param name
 *   Meta tag name (for Meta source)
 * @param pattern
 *   Regex pattern with capture group (for Regex source)
 * @param transforms
 *   Transformations to apply after extraction
 */
final case class ExtractionRule(
    source: ExtractionSource,
    path: Option[String] = None,
    selector: Option[String] = None,
    name: Option[String] = None,
    pattern: Option[String] = None,
    transforms: Seq[Transform] = Seq.empty
) derives ReadWriter

object ExtractionRule:
  /** Create a JSON-LD extraction rule */
  def jsonLd(path: String, transforms: Seq[Transform] = Seq.empty): ExtractionRule =
    ExtractionRule(ExtractionSource.JsonLd, path = Some(path), transforms = transforms)

  /** Create a CSS extraction rule */
  def css(selector: String, transforms: Seq[Transform] = Seq.empty): ExtractionRule =
    ExtractionRule(ExtractionSource.Css, selector = Some(selector), transforms = transforms)

  /** Create a meta tag extraction rule */
  def meta(name: String, transforms: Seq[Transform] = Seq.empty): ExtractionRule =
    ExtractionRule(ExtractionSource.Meta, name = Some(name), transforms = transforms)

  /** Create a regex extraction rule */
  def regex(selector: String, pattern: String, transforms: Seq[Transform] = Seq.empty): ExtractionRule =
    ExtractionRule(ExtractionSource.Regex, selector = Some(selector), pattern = Some(pattern), transforms = transforms)

/**
 * An extraction configuration that defines how to extract job data from HTML.
 *
 * @param id
 *   Unique identifier (UUID)
 * @param name
 *   Human-readable name for the config
 * @param version
 *   Version number (increments on updates)
 * @param matchPatterns
 *   Patterns to match pages this config applies to
 * @param matchHash
 *   Deterministic hash of matchPatterns for fast lookup
 * @param extractRules
 *   Map of field names to extraction rules (tried in order)
 * @param createdAt
 *   When this config was created
 * @param updatedAt
 *   When this config was last updated
 */
final case class ExtractionConfig(
    id: Option[String] = None,
    name: String,
    version: Int = 1,
    matchPatterns: Seq[MatchPattern],
    matchHash: String,
    extractRules: Map[String, Seq[ExtractionRule]],
    createdAt: Option[String] = None,
    updatedAt: Option[String] = None
)

object ExtractionConfig:
  import java.security.MessageDigest
  import java.nio.charset.StandardCharsets

  /**
   * Compute a deterministic hash from match patterns for fast lookup.
   */
  def computeMatchHash(patterns: Seq[MatchPattern]): String = {
    // Sort patterns for deterministic ordering
    val sortedPatterns = patterns.sortBy(p =>
      (p.patternType.toString, p.selector.getOrElse(""), p.pattern.getOrElse(""), p.contentContains.getOrElse(""))
    )
    val json = write(sortedPatterns)
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(json.getBytes(StandardCharsets.UTF_8))
    hash.map("%02x".format(_)).mkString
  }

  /**
   * Create a new config with computed hash.
   */
  def create(
      name: String,
      matchPatterns: Seq[MatchPattern],
      extractRules: Map[String, Seq[ExtractionRule]]
  ): ExtractionConfig = {
    ExtractionConfig(
      name = name,
      matchPatterns = matchPatterns,
      matchHash = computeMatchHash(matchPatterns),
      extractRules = extractRules
    )
  }

  // Custom ReadWriter to handle snake_case from Supabase
  given ReadWriter[ExtractionConfig] = readwriter[ujson.Value].bimap[ExtractionConfig](
    config => {
      val obj = ujson.Obj(
        "name" -> config.name,
        "version" -> config.version,
        "match_patterns" -> writeJs(config.matchPatterns),
        "match_hash" -> config.matchHash,
        "extract_rules" -> writeJs(config.extractRules)
      )
      config.id.foreach(v => obj("id") = v)
      config.createdAt.foreach(v => obj("created_at") = v)
      config.updatedAt.foreach(v => obj("updated_at") = v)
      obj
    },
    json => {
      val obj = json.obj
      ExtractionConfig(
        id = obj.get("id").flatMap(v => if v.isNull then None else Some(v.str)),
        name = obj("name").str,
        version = obj.get("version").map(_.num.toInt).getOrElse(1),
        matchPatterns = read[Seq[MatchPattern]](obj("match_patterns")),
        matchHash = obj("match_hash").str,
        extractRules = read[Map[String, Seq[ExtractionRule]]](obj("extract_rules")),
        createdAt = obj.get("created_at").flatMap(v => if v.isNull then None else Some(v.str)),
        updatedAt = obj.get("updated_at").flatMap(v => if v.isNull then None else Some(v.str))
      )
    }
  )
