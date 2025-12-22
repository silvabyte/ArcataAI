package arcata.api.domain

import upickle.default.*

/**
 * A job application submitted by a user.
 *
 * @param applicationId
 *   Unique identifier (auto-generated)
 * @param jobId
 *   Foreign key to jobs table
 * @param profileId
 *   User's profile ID
 * @param jobProfileId
 *   ID of the job profile used for this application
 * @param statusId
 *   Foreign key to application_statuses table
 * @param statusOrder
 *   Order within the status column (for kanban)
 * @param applicationDate
 *   When the application was submitted
 * @param notes
 *   User's notes about the application
 */
final case class JobApplication(
    applicationId: Option[Long] = None,
    jobId: Option[Long] = None,
    profileId: String,
    jobProfileId: Option[Long] = None,
    statusId: Option[Long] = None,
    statusOrder: Int = 0,
    applicationDate: Option[String] = None,
    notes: Option[String] = None
) derives ReadWriter
