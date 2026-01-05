package arcata.api.utils

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import org.jsoup.Jsoup
import org.jsoup.nodes.{Comment, Document, Node}
import org.jsoup.parser.Parser
import org.jsoup.select.NodeVisitor

import scala.jdk.CollectionConverters.*

/**
 * Utility for cleaning HTML and converting to Markdown.
 *
 * Strips irrelevant content (scripts, styles, comments, etc.) to reduce token count for AI
 * processing.
 */
object HtmlCleaner:

  /** Elements to completely remove (including contents) */
  private val REMOVE_ELEMENTS = Set(
    "style",
    "noscript",
    "iframe",
    "svg",
    "canvas",
    "video",
    "audio",
    "map",
    "object",
    "embed",
    "head",
    "header",
    "footer",
    "nav",
    "aside",
    "form",
    "link", // stylesheet/preload links add noise
    "code", // hidden JSON config blobs (Netflix/Eightfold pattern)
  )

  /** Attributes to strip from all elements */
  private val REMOVE_ATTRIBUTES = Set(
    "style",
    "class",
    "id",
    "onclick",
    "onload",
    "onerror",
    "onmouseover",
    "onmouseout",
    "onfocus",
    "onblur",
    "nonce",
    "crossorigin",
    "integrity",
  )

  /** Maximum content length after cleaning (chars) */
  private val MAX_CONTENT_LENGTH = 180_000

  private val converter = FlexmarkHtmlConverter.builder().build()

  /**
   * Clean HTML by removing irrelevant elements and attributes.
   *
   * Preserves JSON-LD structured data (schema.org) while removing all other scripts,
   * styles, navigation, and other non-content elements.
   *
   * @param html
   *   Raw HTML string
   * @return
   *   Cleaned HTML string
   */
  def clean(html: String): String = {
    val doc = Jsoup.parse(html)

    // IMPORTANT: Extract JSON-LD from anywhere in the document BEFORE removing head
    // Many sites (Ashby, etc.) put JSON-LD in <head>, which we otherwise remove
    val jsonLdScripts = doc.select("script[type=application/ld+json]").asScala.toList
    val jsonLdContents = jsonLdScripts.map {
      script =>
        val rawContent = script.data()
        Parser.unescapeEntities(rawContent, false)
    }

    // Remove unwanted elements entirely (including head, which may contain JSON-LD)
    REMOVE_ELEMENTS.foreach(tag => doc.select(tag).remove())

    // Remove all remaining scripts (JSON-LD already extracted above)
    doc.select("script").remove()

    // Remove HTML comments
    removeComments(doc)

    // Strip data-* attributes and other unwanted attributes
    stripAttributes(doc)

    // Get body content
    val bodyContent = Option(doc.body()).map(_.html()).getOrElse("")

    // Prepend JSON-LD content (as visible text for AI to parse)
    if jsonLdContents.nonEmpty then
      val jsonLdSection = jsonLdContents.mkString("\n\n")
      s"$jsonLdSection\n\n$bodyContent"
    else bodyContent
  }

  /**
   * Convert HTML to Markdown for compact AI consumption.
   *
   * @param html
   *   Raw HTML string
   * @return
   *   Markdown string, truncated if necessary
   */
  def toMarkdown(html: String): String = {
    val cleaned = clean(html)
    val markdown = converter.convert(cleaned)

    // Truncate if still too long
    if markdown.length > MAX_CONTENT_LENGTH then
      markdown.take(MAX_CONTENT_LENGTH) + "\n\n[Content truncated...]"
    else markdown
  }

  /** Remove all comment nodes from the document */
  private def removeComments(doc: Document): Unit = {
    doc.traverse(new NodeVisitor:
      override def head(node: Node, depth: Int): Unit = {
        node match
          case _: Comment => node.remove()
          case _ => ()
      }
      override def tail(node: Node, depth: Int): Unit = ())
  }

  /** Strip unwanted attributes from all elements */
  private def stripAttributes(doc: Document): Unit = {
    doc.getAllElements.asScala.foreach {
      elem =>
        // Remove specific attributes
        REMOVE_ATTRIBUTES.foreach(attr => elem.removeAttr(attr))

        // Remove all data-* attributes
        elem
          .attributes()
          .asScala
          .map(_.getKey)
          .filter(_.startsWith("data-"))
          .toList // Materialize before modifying
          .foreach(elem.removeAttr)
    }
  }
