import type { CSSProperties } from "react";

export type SkeletonVariant = "text" | "circular" | "rectangular";

export type SkeletonProps = {
  /** Shape variant of the skeleton */
  variant?: SkeletonVariant;
  /** Width of the skeleton (CSS value) */
  width?: string | number;
  /** Height of the skeleton (CSS value) */
  height?: string | number;
  /** Additional CSS classes */
  className?: string;
};

const variantClasses: Record<SkeletonVariant, string> = {
  text: "rounded",
  circular: "rounded-full",
  rectangular: "rounded-md",
};

function formatSize(value: string | number | undefined): string | undefined {
  if (value === undefined) {
    return value;
  }
  if (typeof value === "number") {
    return `${value}px`;
  }
  return value;
}

/**
 * A skeleton placeholder for loading states.
 *
 * @example
 * // Text skeleton (default)
 * <Skeleton width={200} height={16} />
 *
 * @example
 * // Circular avatar skeleton
 * <Skeleton variant="circular" width={40} height={40} />
 *
 * @example
 * // Rectangular card skeleton
 * <Skeleton variant="rectangular" width="100%" height={120} />
 */
export function Skeleton({
  variant = "text",
  width,
  height,
  className = "",
}: SkeletonProps) {
  const baseClasses =
    `animate-pulse bg-gray-200 dark:bg-gray-700 ${variantClasses[variant]} ${className}`.trim();

  const style: CSSProperties = {
    width: formatSize(width),
    height: formatSize(height),
  };

  return <div className={baseClasses} style={style} />;
}
