package arcata.api.etl.framework

import java.util.UUID
import java.time.Instant

/**
 * Context passed through the ETL pipeline, carrying metadata and shared state.
 *
 * @param runId
 *   Unique identifier for this pipeline run
 * @param profileId
 *   The user profile ID initiating the pipeline
 * @param startedAt
 *   When the pipeline execution started
 * @param metadata
 *   Arbitrary key-value metadata for step communication
 */
final case class PipelineContext(
  runId: String,
  profileId: String,
  startedAt: Instant,
  metadata: Map[String, String] = Map.empty,
):
  /** Add a metadata entry to the context. */
  def withMetadata(key: String, value: String): PipelineContext =
    copy(metadata = metadata + (key -> value))

  /** Get a metadata value by key. */
  def getMetadata(key: String): Option[String] =
    metadata.get(key)

  /** Add multiple metadata entries. */
  def withMetadataEntries(entries: (String, String)*): PipelineContext =
    copy(metadata = metadata ++ entries.toMap)

object PipelineContext:
  /** Create a new pipeline context with a generated run ID. */
  def create(profileId: String): PipelineContext = {
    PipelineContext(
      runId = UUID.randomUUID().toString,
      profileId = profileId,
      startedAt = Instant.now(),
    )
  }

  /** Create a context with a specific run ID (useful for testing). */
  def withRunId(runId: String, profileId: String): PipelineContext = {
    PipelineContext(
      runId = runId,
      profileId = profileId,
      startedAt = Instant.now(),
    )
  }
