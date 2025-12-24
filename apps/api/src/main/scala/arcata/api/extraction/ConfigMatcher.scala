package arcata.api.extraction

import arcata.api.domain.{ExtractionConfig, MatchPattern, MatchPatternType}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex
import scala.util.Try

/**
 * Matches HTML pages against extraction configs using pattern matching.
 *
 * Given an HTML page and URL, finds the best matching config from a set of candidates.
 * All patterns in a config must match for the config to be considered a match.
 */
object ConfigMatcher:

  /**
   * Result of attempting to match patterns against a page.
   *
   * @param config
   *   The config that matched
   * @param matchedPatterns
   *   Number of patterns that matched
   * @param totalPatterns
   *   Total number of patterns in the config
   */
  case class MatchResult(
      config: ExtractionConfig,
      matchedPatterns: Int,
      totalPatterns: Int
  ):
    /** A full match requires all patterns to match AND at least one pattern to exist */
    def isFullMatch: Boolean = totalPatterns > 0 && matchedPatterns == totalPatterns
    def matchScore: Double = if totalPatterns > 0 then matchedPatterns.toDouble / totalPatterns else 0.0

  /**
   * Find the best matching config for a page.
   *
   * @param html
   *   HTML content of the page
   * @param url
   *   URL of the page
   * @param configs
   *   Candidate configs to match against
   * @return
   *   The best matching config, if any config fully matches
   */
  def findMatch(html: String, url: String, configs: Seq[ExtractionConfig]): Option[ExtractionConfig] = {
    val doc = Jsoup.parse(html, url)

    val matches = configs
      .map(config => matchConfig(doc, url, html, config))
      .filter(_.isFullMatch)
      .sortBy(r => -r.matchedPatterns) // Most specific (most patterns) first

    matches.headOption.map(_.config)
  }

  /**
   * Find all matching configs, sorted by relevance.
   *
   * @param html
   *   HTML content of the page
   * @param url
   *   URL of the page
   * @param configs
   *   Candidate configs to match against
   * @return
   *   All fully-matching configs, sorted by specificity (most patterns first)
   */
  def findAllMatches(html: String, url: String, configs: Seq[ExtractionConfig]): Seq[MatchResult] = {
    val doc = Jsoup.parse(html, url)

    configs
      .map(config => matchConfig(doc, url, html, config))
      .filter(_.isFullMatch)
      .sortBy(r => -r.matchedPatterns)
  }

  /**
   * Check if a single config matches a page.
   *
   * @param doc
   *   Parsed HTML document
   * @param url
   *   URL of the page
   * @param html
   *   Raw HTML content
   * @param config
   *   Config to match against
   * @return
   *   MatchResult with match statistics
   */
  private def matchConfig(doc: Document, url: String, html: String, config: ExtractionConfig): MatchResult = {
    val matchedCount = config.matchPatterns.count(pattern => matchPattern(doc, url, html, pattern))
    MatchResult(config, matchedCount, config.matchPatterns.size)
  }

  /**
   * Check if a single pattern matches a page.
   *
   * @param doc
   *   Parsed HTML document
   * @param url
   *   URL of the page
   * @param html
   *   Raw HTML content
   * @param pattern
   *   Pattern to check
   * @return
   *   true if the pattern matches
   */
  private def matchPattern(doc: Document, url: String, html: String, pattern: MatchPattern): Boolean = {
    pattern.patternType match
      case MatchPatternType.CssExists =>
        pattern.selector match
          case Some(selector) =>
            val elements = doc.select(selector)
            if elements.isEmpty then false
            else {
              // If contentContains is specified, check if any element contains it
              pattern.contentContains match
                case Some(text) =>
                  elements.asScala.exists(_.html().contains(text))
                case None => true
            }
          case None => false

      case MatchPatternType.UrlPattern =>
        pattern.pattern match
          case Some(regexStr) =>
            Try(Regex(regexStr).findFirstIn(url).isDefined).getOrElse(false)
          case None => false

      case MatchPatternType.ContentContains =>
        pattern.contentContains match
          case Some(text) => html.contains(text)
          case None => false
  }

  /**
   * Compute a hash for the set of patterns for fast lookup.
   * This should match the match_hash stored in the database.
   *
   * @param patterns
   *   Match patterns
   * @return
   *   SHA-256 hash of the canonical pattern representation
   */
  def computeHash(patterns: Seq[MatchPattern]): String =
    ExtractionConfig.computeMatchHash(patterns)
