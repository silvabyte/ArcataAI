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
  notes: Option[String] = None,
)

object JobApplication:
  // Custom ReadWriter to handle snake_case from Supabase
  given ReadWriter[JobApplication] = readwriter[ujson.Value].bimap[JobApplication](
    app => {
      val obj = ujson.Obj(
        "profile_id" -> app.profileId,
        "status_order" -> app.statusOrder,
      )
      app.applicationId.foreach(v => obj("application_id") = ujson.Num(v.toDouble))
      app.jobId.foreach(v => obj("job_id") = ujson.Num(v.toDouble))
      app.jobProfileId.foreach(v => obj("job_profile_id") = ujson.Num(v.toDouble))
      app.statusId.foreach(v => obj("status_id") = ujson.Num(v.toDouble))
      app.applicationDate.foreach(v => obj("application_date") = v)
      app.notes.foreach(v => obj("notes") = v)
      obj
    },
    json => {
      val obj = json.obj
      JobApplication(
        applicationId = obj.get("application_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        jobId = obj.get("job_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        profileId = obj("profile_id").str,
        jobProfileId = obj.get("job_profile_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        statusId = obj.get("status_id").flatMap(v => if v.isNull then None else Some(v.num.toLong)),
        statusOrder = obj.get("status_order").map(v => v.num.toInt).getOrElse(0),
        applicationDate = obj.get("application_date").flatMap(v => if v.isNull then None else Some(v.str)),
        notes = obj.get("notes").flatMap(v => if v.isNull then None else Some(v.str)),
      )
    },
  )
