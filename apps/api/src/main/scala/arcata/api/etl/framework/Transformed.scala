package arcata.api.etl.framework

/**
 * Wrapper indicating data has been through a transformation step.
 *
 * Provides compile-time safety that raw extracted data can't bypass transformation. The pattern is
 * reusable for any data type that needs to flow through a transformer before reaching a loader.
 *
 * @tparam A
 *   The underlying data type
 * @param value
 *   The transformed data
 */
final case class Transformed[A](value: A)
