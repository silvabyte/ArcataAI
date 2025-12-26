import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  KanbanCard,
  Progress,
} from "@arcata/components";
import { t } from "@arcata/translate";
import {
  ArrowPathIcon,
  EllipsisVerticalIcon,
} from "@heroicons/react/24/outline";
import {
  BuildingOfficeIcon,
  ClockIcon,
  MapPinIcon,
} from "@heroicons/react/24/solid";
import type { KanbanApplication } from "./types";

type JobCardProps = {
  item: KanbanApplication;
  isSaving: boolean;
  columnIndex: number;
  totalColumns: number;
  onViewDetails: (applicationId: number) => void;
  onRemove: (applicationId: number) => void;
};

function formatRelativeTime(dateString: string | null): string {
  if (!dateString) {
    return "";
  }
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60_000);
  const diffHours = Math.floor(diffMs / 3_600_000);
  const diffDays = Math.floor(diffMs / 86_400_000);

  if (diffMins < 1) {
    return "just now";
  }
  if (diffMins < 60) {
    return `${diffMins}min ago`;
  }
  if (diffHours < 24) {
    return `${diffHours}hrs ago`;
  }
  return `${diffDays}d ago`;
}

function truncate(str: string, maxLen: number): string {
  if (str.length <= maxLen) {
    return str;
  }
  return `${str.slice(0, maxLen)}...`;
}

export function JobCard({
  item,
  isSaving,
  columnIndex,
  totalColumns,
  onViewDetails,
  onRemove,
}: JobCardProps) {
  const progress = Math.floor(((columnIndex + 1) / totalColumns) * 100);
  const { application } = item;
  const job = application.jobs;
  const company = job?.companies;

  return (
    <KanbanCard column={item.column} id={item.id} name={item.name}>
      <div className="flex flex-col gap-2">
        {/* Header */}
        <div className="flex items-center justify-between">
          <span className="text-muted-foreground text-xs">
            {formatRelativeTime(application.updated_at)}
          </span>
          {isSaving ? (
            <ArrowPathIcon className="h-4 w-4 animate-spin text-muted-foreground" />
          ) : (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  aria-label="Card actions"
                  className="rounded p-1 hover:bg-muted"
                  type="button"
                >
                  <EllipsisVerticalIcon className="h-4 w-4 text-muted-foreground" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem
                  onClick={() => onViewDetails(application.application_id)}
                >
                  {t("pages.hq.kanban.actions.viewDetails")}
                </DropdownMenuItem>
                <DropdownMenuItem
                  className="text-destructive"
                  onClick={() => onRemove(application.application_id)}
                >
                  {t("pages.hq.kanban.actions.remove")}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>

        {/* Title & Company */}
        <div>
          <p className="line-clamp-2 font-semibold text-sm">
            {job?.title ?? "Unknown"}
          </p>
          <p className="text-muted-foreground text-xs">
            @ {company?.company_name ?? "Unknown"}
          </p>
        </div>

        {/* Progress Bar */}
        <div className="flex items-center gap-2">
          <Progress className="flex-1" value={progress} />
          <span className="text-muted-foreground text-xs">{progress}%</span>
        </div>

        {/* Tags */}
        <div className="flex flex-wrap gap-2 text-muted-foreground text-xs">
          {job?.experience_level ? (
            <span className="flex items-center gap-1">
              <BuildingOfficeIcon className="h-3 w-3" />
              {job.experience_level}
            </span>
          ) : null}
          {job?.job_type ? (
            <span className="flex items-center gap-1">
              <ClockIcon className="h-3 w-3" />
              {job.job_type}
            </span>
          ) : null}
          {job?.location ? (
            <span className="flex items-center gap-1">
              <MapPinIcon className="h-3 w-3" />
              {truncate(job.location, 12)}
            </span>
          ) : null}
        </div>
      </div>
    </KanbanCard>
  );
}
