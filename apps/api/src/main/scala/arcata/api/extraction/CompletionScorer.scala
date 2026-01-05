package arcata.api.extraction

import arcata.api.domain.ExtractedJobData

/**
 * Scores job data extraction completeness using weighted fields.
 *
 * Field weights are calibrated based on user value:
 *   - Required fields (title, company, description) have highest weights
 *   - Salary information is valuable but optional
 *   - Other fields provide incremental value
 *
 * Thresholds:
 *   - Complete: 90%+ score
 *   - Sufficient: 70-90% score
 *   - Partial: 50-70% score
 *   - Minimal: <50% but has required fields
 *   - Failed: Missing required fields
 */
object CompletionScorer:

  /** Minimum character length for a field to be considered "present" */
  private val MinFieldLength = 5

  /** Field weights for scoring (total = 100) */
  private val FieldWeights: Map[String, Int] = Map(
    "title" -> 20,
    "companyName" -> 15,
    "description" -> 25,
    "location" -> 10,
    "salaryMin" -> 5,
    "salaryMax" -> 5,
    "qualifications" -> 5,
    "responsibilities" -> 5,
    "benefits" -> 5,
    "jobType" -> 3,
    "experienceLevel" -> 2,
  )

  /** Fields that must be present for extraction to not be Failed */
  private val RequiredFields: Set[String] = Set("title", "companyName", "description")

  /** Total possible score */
  private val MaxScore: Int = FieldWeights.values.sum

  /** Score thresholds */
  private val CompleteThreshold = 0.90
  private val SufficientThreshold = 0.70
  private val PartialThreshold = 0.50

  /**
   * Score extraction result from a map of field names to values.
   *
   * @param fields
   *   Map of field name to extracted value (empty/null = not extracted)
   * @return
   *   Tuple of (CompletionState, score percentage, missing fields)
   */
  def score(fields: Map[String, Option[String]]): ScoringResult = {
    val presentFields = fields.filter {
      case (_, value) =>
        value.exists(v => v.trim.length >= MinFieldLength)
    }.keySet

    val missingRequired = RequiredFields.diff(presentFields)
    val missingOptional = FieldWeights.keySet.diff(presentFields).diff(RequiredFields)

    // Convert to Seq to avoid Set deduplication of same weights
    val earnedScore = presentFields.toSeq.flatMap(FieldWeights.get).sum
    val scorePercentage = earnedScore.toDouble / MaxScore

    val state = {
      if missingRequired.nonEmpty then CompletionState.Failed
      else if scorePercentage >= CompleteThreshold then CompletionState.Complete
      else if scorePercentage >= SufficientThreshold then CompletionState.Sufficient
      else if scorePercentage >= PartialThreshold then CompletionState.Partial
      else CompletionState.Minimal
    }

    ScoringResult(
      state = state,
      score = scorePercentage,
      earnedPoints = earnedScore,
      maxPoints = MaxScore,
      presentFields = presentFields.toSeq.sorted,
      missingRequired = missingRequired.toSeq.sorted,
      missingOptional = missingOptional.toSeq.sorted,
    )
  }

  /**
   * Score an ExtractedJobData instance.
   *
   * @param data
   *   The extracted job data
   * @return
   *   Scoring result with state and details
   */
  def scoreExtractedData(data: ExtractedJobData): ScoringResult = {
    val fields: Map[String, Option[String]] = Map(
      "title" -> Some(data.title), // title is required (not Option)
      "companyName" -> data.companyName,
      "description" -> data.description,
      "location" -> data.location,
      "salaryMin" -> data.salaryMin.map(_.toString),
      "salaryMax" -> data.salaryMax.map(_.toString),
      "qualifications" -> data.qualifications.filter(_.nonEmpty).map(_.mkString(", ")),
      "responsibilities" -> data.responsibilities.filter(_.nonEmpty).map(_.mkString(", ")),
      "benefits" -> data.benefits.filter(_.nonEmpty).map(_.mkString(", ")),
      "jobType" -> data.jobType,
      "experienceLevel" -> data.experienceLevel,
    )
    score(fields)
  }

/**
 * Result of scoring an extraction.
 *
 * @param state
 *   The computed CompletionState
 * @param score
 *   Score as a percentage (0.0 to 1.0)
 * @param earnedPoints
 *   Points earned from present fields
 * @param maxPoints
 *   Maximum possible points
 * @param presentFields
 *   Fields that were successfully extracted
 * @param missingRequired
 *   Required fields that are missing
 * @param missingOptional
 *   Optional fields that are missing
 */
case class ScoringResult(
  state: CompletionState,
  score: Double,
  earnedPoints: Int,
  maxPoints: Int,
  presentFields: Seq[String],
  missingRequired: Seq[String],
  missingOptional: Seq[String],
):
  /** Format score as percentage string */
  def scorePercent: String = f"${score * 100}%.1f%%"

  /** Human-readable summary */
  def summary: String =
    s"${state} (${scorePercent}, ${earnedPoints}/${maxPoints} points)"

  /** Check if extraction has all required fields */
  def hasRequiredFields: Boolean = missingRequired.isEmpty
