import { BoardIcon } from "@arcata/components";
import { t } from "@arcata/translate";
import { EllipsisVerticalIcon } from "@heroicons/react/24/outline";

export type JobCardActionsProps = {
  onTrack?: () => void;
  onMenuAction?: (action: string) => void;
  isMobile?: boolean;
};

export function JobCardActions({
  onTrack,
  onMenuAction,
  isMobile = false,
}: JobCardActionsProps) {
  return (
    <div className="flex items-center gap-2">
      <button
        className={`inline-flex items-center justify-center gap-2 rounded-lg bg-gray-900 font-medium text-sm text-white hover:bg-gray-800 ${
          isMobile ? "flex-1 px-4 py-3" : "px-4 py-2"
        }`}
        onClick={onTrack}
        type="button"
      >
        <BoardIcon className="h-4 w-4" />
        {t("pages.jobStream.actions.track")}
      </button>
      <button
        className={`rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-100 hover:text-gray-700 ${
          isMobile ? "p-3" : "p-2"
        }`}
        onClick={() => onMenuAction?.("menu")}
        type="button"
      >
        <EllipsisVerticalIcon className="h-5 w-5" />
      </button>
    </div>
  );
}
