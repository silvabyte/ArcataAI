import { CheckIcon } from "@heroicons/react/20/solid";
import { cn } from "../utils";

export type FilterCheckboxProps = {
  id: string;
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  className?: string;
};

export function FilterCheckbox({
  id,
  label,
  checked,
  onChange,
  className,
}: FilterCheckboxProps) {
  return (
    <label
      className={cn("flex cursor-pointer items-center gap-2", className)}
      htmlFor={id}
    >
      <div className="relative">
        <input
          checked={checked}
          className="peer sr-only"
          id={id}
          onChange={(e) => onChange(e.target.checked)}
          type="checkbox"
        />
        <div
          className={cn(
            "flex h-4 w-4 items-center justify-center rounded border transition-colors",
            checked
              ? "border-gray-900 bg-gray-900"
              : "border-gray-300 bg-white peer-hover:border-gray-400"
          )}
        >
          {checked ? <CheckIcon className="h-3 w-3 text-white" /> : null}
        </div>
      </div>
      <span className="text-gray-700 text-sm">{label}</span>
    </label>
  );
}
