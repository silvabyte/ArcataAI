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
 *   Match score against best matching job profile (0-100)
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
    bestMatchScore: Option[Int] = None,
    bestMatchJobProfileId: Option[Long] = None,
    profileMatches: Option[ujson.Value] = None
) derives ReadWriter
