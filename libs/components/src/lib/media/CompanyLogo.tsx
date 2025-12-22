import { useState } from "react";
import { cn } from "../utils";

export type CompanyLogoProps = {
  name: string;
  logoUrl?: string;
  size?: "sm" | "md" | "lg";
  className?: string;
};

const sizeClasses = {
  sm: "h-8 w-8 text-sm",
  md: "h-12 w-12 text-lg",
  lg: "h-16 w-16 text-xl",
};

const sizePx = {
  sm: 32,
  md: 48,
  lg: 64,
};

// Generate a consistent background color based on company name
function getBackgroundColor(name: string): string {
  const colors = [
    "bg-blue-500",
    "bg-green-500",
    "bg-purple-500",
    "bg-orange-500",
    "bg-pink-500",
    "bg-teal-500",
    "bg-indigo-500",
    "bg-rose-500",
  ];

  let hash = 0;
  for (const char of name) {
    hash = char.charCodeAt(0) + (hash * 31 - hash);
  }

  return colors[Math.abs(hash) % colors.length];
}

export function CompanyLogo({
  name,
  logoUrl,
  size = "md",
  className,
}: CompanyLogoProps) {
  const [imageError, setImageError] = useState(false);
  const showFallback = !logoUrl || imageError;
  const initial = name.charAt(0).toUpperCase();

  if (showFallback) {
    return (
      <div
        className={cn(
          "flex items-center justify-center rounded-lg font-semibold text-white",
          sizeClasses[size],
          getBackgroundColor(name),
          className
        )}
      >
        {initial}
      </div>
    );
  }

  const handleError = () => setImageError(true);

  return (
    // biome-ignore lint/a11y/noNoninteractiveElementInteractions: onError is for image load fallback, not user interaction
    <img
      alt={`${name} logo`}
      className={cn("rounded-lg object-cover", sizeClasses[size], className)}
      height={sizePx[size]}
      onError={handleError}
      src={logoUrl}
      width={sizePx[size]}
    />
  );
}
