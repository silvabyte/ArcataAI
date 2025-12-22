import { ChevronDownIcon } from "@heroicons/react/24/outline";
import { useState } from "react";
import { cn } from "../utils";

export type FilterSectionProps = {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
  className?: string;
};

export function FilterSection({
  title,
  children,
  defaultOpen = false,
  className,
}: FilterSectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className={cn("border-gray-200 border-b py-4", className)}>
      <button
        className="flex w-full items-center justify-between text-left"
        onClick={() => setIsOpen(!isOpen)}
        type="button"
      >
        <span className="font-medium text-gray-900 text-sm">{title}</span>
        <ChevronDownIcon
          className={cn(
            "h-5 w-5 text-gray-500 transition-transform duration-200",
            isOpen && "rotate-180"
          )}
        />
      </button>
      {isOpen ? <div className="pt-3">{children}</div> : null}
    </div>
  );
}
