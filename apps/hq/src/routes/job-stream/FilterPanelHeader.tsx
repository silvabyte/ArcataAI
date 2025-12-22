import { t } from "@arcata/translate";

export type FilterPanelHeaderProps = {
  jobCount: number;
  onClearFilters: () => void;
  hasActiveFilters: boolean;
};

export function FilterPanelHeader({
  jobCount,
  onClearFilters,
  hasActiveFilters,
}: FilterPanelHeaderProps) {
  return (
    <div className="mb-4 flex items-center justify-between">
      <span className="font-medium text-gray-900 text-sm">
        {t("pages.jobStream.filters.jobsFound", { count: jobCount })}
      </span>
      {hasActiveFilters ? (
        <button
          className="font-medium text-gray-600 text-sm hover:text-gray-900 hover:underline"
          onClick={onClearFilters}
          type="button"
        >
          {t("pages.jobStream.filters.clearFilters")}
        </button>
      ) : null}
    </div>
  );
}
