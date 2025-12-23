package arcata.api.domain

import upickle.default.*

/**
 * An entry in a user's job stream.
 *
 * Represents a job that has been added to a user's stream for review and potential application.
 *
 * @param streamId
 *   Unique identifier (auto-generated)
 * @param jobId
 *   Foreign key to jobs table
 * @param profileId
 *   User's profile ID
 * @param source
 *   How the job was added (e.g., "manual", "scrape", "recommendation")
 * @param status
 *   Stream status (e.g., "new", "reviewed", "hidden")
 * @param bestMatchScore
 *   Match score against best matching job profile (0.0-1.0)
 * @param bestMatchJobProfileId
 *   ID of the job profile with highest match
 * @param profileMatches
 *   JSON object with match scores per job profile
 */
final case class JobStreamEntry(
    streamId: Option[Long] = None,
    jobId: Long,
    profileId: String,
    source: String,
    status: Option[String] = Some("new"),
    bestMatchScore: Option[Double] = None,
    bestMatchJobProfileId: Option[Long] = None,
    profileMatches: Option[ujson.Value] = None
)

object JobStreamEntry:
  // Custom ReadWriter to handle snake_case from Supabase
  given ReadWriter[JobStreamEntry] = readwriter[ujson.Value].bimap[JobStreamEntry](
    entry => {
      val obj = ujson.Obj(
        "job_id" -> ujson.Num(entry.jobId.toDouble),
        "profile_id" -> entry.profileId,
        "source" -> entry.source
      )
      entry.streamId.foreach(v => obj("stream_id") = ujson.Num(v.toDouble))
      entry.status.foreach(v => obj("status") = v)
      entry.bestMatchScore.foreach(v => obj("best_match_score") = v)
      entry.bestMatchJobProfileId.foreach(v => obj("best_match_job_profile_id") = ujson.Num(v.toDouble))
      entry.profileMatches.foreach(v => obj("profile_matches") = v)
      obj
    },
    json => {
      val obj = json.obj
      JobStreamEntry(
        streamId = obj.get("stream_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        jobId = obj("job_id").num.toLong,
        profileId = obj("profile_id").str,
        source = obj("source").str,
        status = obj.get("status").flatMap(v => if v.isNull then None else Some(v.str)),
        bestMatchScore = obj.get("best_match_score").flatMap(v => if v.isNull then None else Some(v.num)),
        bestMatchJobProfileId =
          obj.get("best_match_job_profile_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        profileMatches = obj.get("profile_matches").flatMap(v => if v.isNull then None else Some(v))
      )
    }
  )
