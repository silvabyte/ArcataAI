import { Combobox } from "@headlessui/react";
import { ChevronDownIcon } from "@heroicons/react/20/solid";
import { useState } from "react";
import { RemovablePill } from "../pills/RemovablePill";
import { cn } from "../utils";

export type FilterComboboxOption = {
  id: string | number;
  label: string;
};

export type FilterComboboxProps = {
  options: FilterComboboxOption[];
  selected: FilterComboboxOption[];
  onSelect: (option: FilterComboboxOption) => void;
  onRemove: (option: FilterComboboxOption) => void;
  placeholder?: string;
  allowCustom?: boolean;
  className?: string;
};

export function FilterCombobox({
  options,
  selected,
  onSelect,
  onRemove,
  placeholder = "Search...",
  allowCustom = false,
  className,
}: FilterComboboxProps) {
  const [query, setQuery] = useState("");

  // Filter out already selected options and match query
  const filteredOptions =
    query === ""
      ? options.filter((opt) => !selected.some((s) => s.id === opt.id))
      : options.filter(
          (opt) =>
            !selected.some((s) => s.id === opt.id) &&
            opt.label.toLowerCase().includes(query.toLowerCase())
        );

  const handleSelect = (option: FilterComboboxOption | null) => {
    if (option) {
      onSelect(option);
      setQuery("");
    }
  };

  return (
    <div className={cn("space-y-2", className)}>
      <Combobox onChange={handleSelect} value={null}>
        <div className="relative">
          <Combobox.Input
            className="w-full rounded-lg border border-gray-300 py-2 pr-10 pl-3 text-sm placeholder:text-gray-400 focus:border-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-500"
            displayValue={() => query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={placeholder}
          />
          <Combobox.Button className="absolute inset-y-0 right-0 flex items-center rounded-r-lg px-2">
            <ChevronDownIcon
              aria-hidden="true"
              className="h-5 w-5 text-gray-400"
            />
          </Combobox.Button>

          <Combobox.Options className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-lg bg-white py-1 text-sm shadow-lg ring-1 ring-black/5">
            {allowCustom && query.length > 0 ? (
              <Combobox.Option
                className={({ active }) =>
                  cn(
                    "cursor-pointer select-none px-3 py-2 text-gray-900",
                    active && "bg-gray-100"
                  )
                }
                value={{ id: query, label: query }}
              >
                Add "{query}"
              </Combobox.Option>
            ) : null}
            {filteredOptions.length === 0 &&
            query.length > 0 &&
            !allowCustom ? (
              <div className="px-3 py-2 text-gray-500">No results found</div>
            ) : null}
            {filteredOptions.map((option) => (
              <Combobox.Option
                className={({ active }) =>
                  cn(
                    "cursor-pointer select-none px-3 py-2 text-gray-900",
                    active && "bg-gray-100"
                  )
                }
                key={option.id}
                value={option}
              >
                <span className="block truncate">{option.label}</span>
              </Combobox.Option>
            ))}
          </Combobox.Options>
        </div>
      </Combobox>

      {/* Selected pills */}
      {selected.length > 0 ? (
        <div className="flex flex-wrap gap-1.5">
          {selected.map((item) => (
            <RemovablePill
              key={item.id}
              label={item.label}
              onRemove={() => onRemove(item)}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}
