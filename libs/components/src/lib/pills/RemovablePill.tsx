import { cn } from "../utils";

export type RemovablePillProps = {
  label: string;
  onRemove: () => void;
  className?: string;
};

export function RemovablePill({
  label,
  onRemove,
  className,
}: RemovablePillProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-x-1 rounded-md bg-gray-100 px-2 py-1 font-medium text-gray-700 text-xs",
        className
      )}
    >
      {label}
      <button
        className="group relative -mr-0.5 h-3.5 w-3.5 rounded-sm hover:bg-gray-500/20"
        onClick={onRemove}
        type="button"
      >
        <span className="sr-only">Remove {label}</span>
        <svg
          aria-hidden="true"
          className="h-3.5 w-3.5 stroke-gray-600/50 group-hover:stroke-gray-600/75"
          fill="none"
          viewBox="0 0 14 14"
        >
          <path d="M4 4l6 6m0-6l-6 6" strokeLinecap="round" />
        </svg>
        <span className="absolute -inset-1" />
      </button>
    </span>
  );
}
