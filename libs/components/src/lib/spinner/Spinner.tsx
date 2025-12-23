export type SpinnerSize = "sm" | "md" | "lg";

export type SpinnerProps = {
  /** Size of the spinner */
  size?: SpinnerSize;
  /** Additional CSS classes */
  className?: string;
};

const sizeClasses: Record<SpinnerSize, string> = {
  sm: "size-4",
  md: "size-5",
  lg: "size-6",
};

/**
 * A simple spinning loader indicator.
 *
 * @example
 * // Default medium size
 * <Spinner />
 *
 * @example
 * // Small spinner in a button
 * <button disabled>
 *   <Spinner size="sm" className="mr-2" />
 *   Processing...
 * </button>
 */
export function Spinner({ size = "md", className = "" }: SpinnerProps) {
  const classes = `animate-spin ${sizeClasses[size]} ${className}`.trim();

  return (
    <svg
      aria-hidden="true"
      className={classes}
      fill="none"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <title>Loading</title>
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        fill="currentColor"
      />
    </svg>
  );
}
