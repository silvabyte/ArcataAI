package arcata.api.extraction

import upickle.default.*

/**
 * Represents the completeness of job data extraction.
 *
 * Used to track data quality and determine if re-extraction is needed.
 */
enum CompletionState derives ReadWriter:
  /** 90%+ score - All important fields extracted successfully */
  case Complete

  /** 70-90% score - Required fields + enough optional for user value */
  case Sufficient

  /** 50-70% score - Some fields extracted, missing important ones */
  case Partial

  /** <50% score but has required fields - Only basic fields (title, company) */
  case Minimal

  /** Missing required fields - Extraction essentially failed */
  case Failed

  /** Unknown state - Legacy data or not yet evaluated */
  case Unknown

object CompletionState:
  /** Convert to database-friendly string */
  def toDbString(state: CompletionState): String = state match
    case Complete => "complete"
    case Sufficient => "sufficient"
    case Partial => "partial"
    case Minimal => "minimal"
    case Failed => "failed"
    case Unknown => "unknown"

  /** Parse from database string */
  def fromDbString(s: String): CompletionState = s.toLowerCase match
    case "complete" => Complete
    case "sufficient" => Sufficient
    case "partial" => Partial
    case "minimal" => Minimal
    case "failed" => Failed
    case _ => Unknown

  /** Parse from string, returning Option for safety */
  def fromString(s: String): Option[CompletionState] = s.toLowerCase match
    case "complete" => Some(Complete)
    case "sufficient" => Some(Sufficient)
    case "partial" => Some(Partial)
    case "minimal" => Some(Minimal)
    case "failed" => Some(Failed)
    case "unknown" => Some(Unknown)
    case _ => None
