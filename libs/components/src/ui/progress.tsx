import * as React from "react";

import { cn } from "../lib/utils";

type ProgressProps = React.HTMLAttributes<HTMLDivElement> & {
  value: number;
  max?: number;
};

const Progress = React.forwardRef<HTMLDivElement, ProgressProps>(
  ({ className, value, max = 100, ...props }, ref) => {
    const percentage = Math.min(100, Math.max(0, (value / max) * 100));

    return (
      <div
        className={cn("overflow-hidden rounded-full bg-gray-200", className)}
        ref={ref}
        {...props}
      >
        <div
          className="h-1.5 rounded-full bg-indigo-600 transition-all"
          style={{ width: `${percentage}%` }}
        />
      </div>
    );
  }
);
Progress.displayName = "Progress";

export { Progress };
