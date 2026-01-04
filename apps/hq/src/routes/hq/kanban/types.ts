import type { KanbanColumnProps, KanbanItemProps } from "@arcata/components";
import type { ApplicationStatus, JobApplicationWithJob } from "@arcata/db";

// === Card Type ===
// Satisfies Shadcn's required shape + carries full application record
export type KanbanApplication = KanbanItemProps & {
  application: JobApplicationWithJob;
};

// === Column Type ===
// Satisfies Shadcn's required shape + carries full status record
export type KanbanStatus = KanbanColumnProps & {
  status: ApplicationStatus;
};

// === Mappers ===

export function toKanbanApplication(
  app: JobApplicationWithJob
): KanbanApplication {
  return {
    // Required by Shadcn KanbanItemProps
    id: `card-${app.application_id}`,
    name: app.jobs?.title ?? "Unknown",
    column: `col-${app.status_id}`,
    // Full domain data
    application: app,
  };
}

export function toKanbanStatus(status: ApplicationStatus): KanbanStatus {
  return {
    // Required by Shadcn KanbanColumnProps
    id: `col-${status.status_id}`,
    name: status.name,
    // Full domain data
    status,
  };
}
