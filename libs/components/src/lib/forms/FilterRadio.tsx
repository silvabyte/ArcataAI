import { cn } from "../utils";

export type FilterRadioProps = {
  id: string;
  name: string;
  label: string;
  value: string;
  checked: boolean;
  onChange: (value: string) => void;
  className?: string;
};

export function FilterRadio({
  id,
  name,
  label,
  value,
  checked,
  onChange,
  className,
}: FilterRadioProps) {
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
          name={name}
          onChange={() => onChange(value)}
          type="radio"
          value={value}
        />
        <div
          className={cn(
            "flex h-4 w-4 items-center justify-center rounded-full border transition-colors",
            checked
              ? "border-gray-900 bg-gray-900"
              : "border-gray-300 bg-white peer-hover:border-gray-400"
          )}
        >
          {checked ? (
            <div className="h-1.5 w-1.5 rounded-full bg-white" />
          ) : null}
        </div>
      </div>
      <span className="text-gray-700 text-sm">{label}</span>
    </label>
  );
}
