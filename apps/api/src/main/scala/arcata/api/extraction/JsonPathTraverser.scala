package arcata.api.extraction

import scala.util.Try
import scala.util.matching.Regex

/**
 * Simple JSONPath-like traverser for ujson.Value.
 *
 * Supports a subset of JSONPath syntax sufficient for extraction configs:
 *   - $.foo           - Root property access
 *   - $.foo.bar       - Nested property access
 *   - $.foo[0]        - Array index access
 *   - $.foo[0].bar    - Combined access
 *   - $.foo.bar[0].baz.qux[1] - Arbitrary depth
 *
 * Does NOT support (by design - not needed for extraction):
 *   - Wildcards: $.foo[*], $..bar
 *   - Filters: $.foo[?(@.price > 10)]
 *   - Functions: $.foo.length()
 *
 * Thread-safe and stateless.
 */
object JsonPathTraverser:

  /** Pattern to parse path segments */
  private val SegmentPattern: Regex = """([^\.\[\]]+)|\[(\d+)\]""".r

  /**
   * Extract a value from JSON using a simple path expression.
   *
   * @param json
   *   The ujson.Value to traverse
   * @param path
   *   Path like "$.foo.bar[0].baz" or "foo.bar" ($ prefix optional)
   * @return
   *   Some(value) if found, None if path doesn't exist
   */
  def get(json: ujson.Value, path: String): Option[ujson.Value] = {
    val normalizedPath = path.stripPrefix("$").stripPrefix(".")
    if normalizedPath.isEmpty then Some(json)
    else traverse(json, parseSegments(normalizedPath))
  }

  /**
   * Extract a string value from JSON.
   *
   * @param json
   *   The ujson.Value to traverse
   * @param path
   *   Path expression
   * @return
   *   Some(string) if found and is a string, None otherwise
   */
  def getString(json: ujson.Value, path: String): Option[String] =
    get(json, path).flatMap(v => Try(v.str).toOption)

  /**
   * Extract a numeric value from JSON as Double.
   *
   * @param json
   *   The ujson.Value to traverse
   * @param path
   *   Path expression
   * @return
   *   Some(number) if found and is numeric, None otherwise
   */
  def getNumber(json: ujson.Value, path: String): Option[Double] =
    get(json, path).flatMap(v => Try(v.num).toOption)

  /**
   * Extract an integer value from JSON.
   *
   * @param json
   *   The ujson.Value to traverse
   * @param path
   *   Path expression
   * @return
   *   Some(int) if found and is numeric, None otherwise
   */
  def getInt(json: ujson.Value, path: String): Option[Int] =
    getNumber(json, path).map(_.toInt)

  /**
   * Extract a boolean value from JSON.
   *
   * @param json
   *   The ujson.Value to traverse
   * @param path
   *   Path expression
   * @return
   *   Some(bool) if found and is boolean, None otherwise
   */
  def getBool(json: ujson.Value, path: String): Option[Boolean] =
    get(json, path).flatMap(v => Try(v.bool).toOption)

  /**
   * Extract an array from JSON.
   *
   * @param json
   *   The ujson.Value to traverse
   * @param path
   *   Path expression
   * @return
   *   Some(array) if found and is array, None otherwise
   */
  def getArray(json: ujson.Value, path: String): Option[Seq[ujson.Value]] =
    get(json, path).flatMap(v => Try(v.arr.toSeq).toOption)

  /** Sealed trait for path segments */
  private sealed trait Segment
  private case class Property(name: String) extends Segment
  private case class Index(idx: Int) extends Segment

  /** Parse a path string into segments */
  private def parseSegments(path: String): List[Segment] = {
    val segments = scala.collection.mutable.ListBuffer[Segment]()

    // Split by dots, but handle array indices specially
    var remaining = path
    while remaining.nonEmpty do
      // Check for array index at start
      if remaining.startsWith("[") then
        val endBracket = remaining.indexOf(']')
        if endBracket > 0 then
          val idxStr = remaining.substring(1, endBracket)
          Try(idxStr.toInt).foreach(idx => segments += Index(idx))
          remaining = remaining.substring(endBracket + 1).stripPrefix(".")
        else remaining = ""
      else {
        // Find next delimiter (. or [)
        val dotIdx = remaining.indexOf('.')
        val bracketIdx = remaining.indexOf('[')
        val nextDelim = {
          if dotIdx < 0 && bracketIdx < 0 then remaining.length
          else if dotIdx < 0 then bracketIdx
          else if bracketIdx < 0 then dotIdx
          else math.min(dotIdx, bracketIdx)
        }

        val segment = remaining.substring(0, nextDelim)
        if segment.nonEmpty then segments += Property(segment)

        remaining = {
          if nextDelim >= remaining.length then ""
          else if remaining.charAt(nextDelim) == '.' then remaining.substring(nextDelim + 1)
          else remaining.substring(nextDelim)
        }
      }

    segments.toList
  }

  /** Traverse JSON following segments */
  private def traverse(json: ujson.Value, segments: List[Segment]): Option[ujson.Value] = {
    segments match
      case Nil => Some(json)
      case Property(name) :: rest =>
        Try(json.obj.get(name)).toOption.flatten.flatMap(v => traverse(v, rest))
      case Index(idx) :: rest =>
        Try(json.arr.lift(idx)).toOption.flatten.flatMap(v => traverse(v, rest))
  }
