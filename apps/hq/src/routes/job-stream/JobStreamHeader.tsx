import { t } from "@arcata/translate";
import {
  AdjustmentsHorizontalIcon,
  ArrowPathIcon,
  BarsArrowDownIcon,
  BarsArrowUpIcon,
  MagnifyingGlassIcon,
} from "@heroicons/react/24/outline";
import { useState } from "react";

export type JobStreamHeaderProps = {
  onToggleFullInfo?: (enabled: boolean) => void;
  onFilterClick?: () => void;
  fullInfoEnabled?: boolean;
};

export function JobStreamHeader({
  onToggleFullInfo,
  onFilterClick,
  fullInfoEnabled = false,
}: JobStreamHeaderProps) {
  const [isFullInfo, setIsFullInfo] = useState(fullInfoEnabled);

  const handleToggle = () => {
    const newValue = !isFullInfo;
    setIsFullInfo(newValue);
    onToggleFullInfo?.(newValue);
  };

  return (
    <div className="flex items-center justify-between border-gray-200 border-b bg-white px-4 py-3 lg:px-6 lg:py-4">
      <h1 className="font-semibold text-gray-900 text-lg lg:text-xl">
        {t("pages.jobStream.title")}
      </h1>

      <div className="flex items-center gap-2 lg:gap-6">
        {/* Full Info Toggle - hidden on mobile */}
        <div className="hidden items-center gap-2 lg:flex">
          <span className="text-gray-600 text-sm">
            {t("pages.jobStream.fullInfo")}
          </span>
          <button
            aria-checked={isFullInfo}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              isFullInfo ? "bg-gray-900" : "bg-gray-200"
            }`}
            onClick={handleToggle}
            role="switch"
            type="button"
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                isFullInfo ? "translate-x-6" : "translate-x-1"
              }`}
            />
          </button>
        </div>

        {/* Action Icons */}
        <div className="flex items-center gap-1">
          {/* Filter button - mobile only */}
          <button
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 lg:hidden"
            onClick={onFilterClick}
            type="button"
          >
            <AdjustmentsHorizontalIcon className="h-5 w-5" />
          </button>
          <button
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700"
            type="button"
          >
            <MagnifyingGlassIcon className="h-5 w-5" />
          </button>
          {/* Sort icons - hidden on mobile */}
          <button
            className="hidden rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 lg:block"
            type="button"
          >
            <BarsArrowDownIcon className="h-5 w-5" />
          </button>
          <button
            className="hidden rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 lg:block"
            type="button"
          >
            <BarsArrowUpIcon className="h-5 w-5" />
          </button>
          <button
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700"
            type="button"
          >
            <ArrowPathIcon className="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  );
}
