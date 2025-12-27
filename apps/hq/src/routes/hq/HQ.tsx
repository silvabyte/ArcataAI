import {
  EditableText,
  KanbanBoard,
  KanbanCards,
  KanbanHeader,
  KanbanProvider,
  useNotification,
} from "@arcata/components";
import {
  type ApplicationStatus,
  db,
  getCurrentUser,
  getSession,
  type JobApplicationWithJob,
  listApplicationsWithJobs,
} from "@arcata/db";
import { t } from "@arcata/translate";
import type { User } from "@supabase/supabase-js";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLoaderData, useRevalidator } from "react-router-dom";
import { ingestJobWithProgress, type ProgressUpdate } from "../../lib/api";
import { AddJobPopover, IngestionProgress, JobDetailPanel } from "../jobs";
import { JobCard } from "./kanban/JobCard";
import { calculateNewOrder, findChangedItem } from "./kanban/orderUtils";
import {
  type KanbanApplication,
  type KanbanStatus,
  toKanbanApplication,
  toKanbanStatus,
} from "./kanban/types";

type IngestionState = {
  status: "idle" | "processing" | "success" | "error";
  currentStep: number;
  totalSteps: number;
  stepName: string;
  jobId?: number;
  error?: string;
};

const INITIAL_STATE: IngestionState = {
  status: "idle",
  currentStep: 0,
  totalSteps: 6,
  stepName: "",
};

export const loader = async () => {
  try {
    const user = await getCurrentUser();
    const [applications, statuses] = await Promise.all([
      listApplicationsWithJobs(user?.id ?? ""),
      db.application_statuses.list<ApplicationStatus>({
        eq: { key: "profile_id", value: user?.id },
      }),
    ]);

    if (!Array.isArray(statuses) || statuses.length === 0) {
      throw new Error("Failed to load application statuses");
    }

    return { user, applications, statuses };
  } catch (error) {
    throw new Response(JSON.stringify({ error: (error as Error).message }), {
      status: 500,
    });
  }
};

export default function HQ() {
  const data = useLoaderData() as {
    user?: User | null;
    applications: JobApplicationWithJob[] | null;
    statuses: ApplicationStatus[] | null;
  } | null;

  const revalidator = useRevalidator();

  const applications = useMemo(
    () => data?.applications ?? [],
    [data?.applications]
  );

  const [localStatuses, setLocalStatuses] = useState<ApplicationStatus[]>([]);

  // Ingestion state
  const [ingestionState, setIngestionState] =
    useState<IngestionState>(INITIAL_STATE);

  // Job detail panel state - supports both streamId (from ingestion) and jobId (from kanban)
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelStreamId, setPanelStreamId] = useState<number | null>(null);
  const [panelJobId, setPanelJobId] = useState<number | null>(null);

  const handleProgress = useCallback(
    (update: ProgressUpdate) => {
      if (update.status === "complete") {
        setIngestionState({
          status: "success",
          currentStep: update.totalSteps,
          totalSteps: update.totalSteps,
          stepName: update.message,
          jobId: update.jobId,
        });

        // Revalidate data immediately
        revalidator.revalidate();

        // After 1.5s, open panel and reset
        setTimeout(() => {
          if (update.streamId) {
            setPanelStreamId(update.streamId);
            setPanelOpen(true);
          }
          setIngestionState(INITIAL_STATE);
        }, 1500);
      } else if (update.status === "error") {
        setIngestionState({
          status: "error",
          currentStep: update.step,
          totalSteps: update.totalSteps,
          stepName: update.message,
          error: update.error,
        });
      } else {
        setIngestionState({
          status: "processing",
          currentStep: update.step,
          totalSteps: update.totalSteps,
          stepName: update.message,
        });
      }
    },
    [revalidator]
  );

  const handleSubmitJob = useCallback(
    async (url: string) => {
      setIngestionState({
        status: "processing",
        currentStep: 0,
        totalSteps: 6,
        stepName: "Starting...",
      });

      const session = await getSession();
      if (!session?.access_token) {
        setIngestionState({
          status: "error",
          currentStep: 0,
          totalSteps: 6,
          stepName: "Authentication failed",
          error: "Please log in again",
        });
        return;
      }

      await ingestJobWithProgress(
        { url, source: "manual", createApplication: true },
        session.access_token,
        handleProgress
      );
    },
    [handleProgress]
  );

  const handleDismissError = useCallback(() => {
    setIngestionState(INITIAL_STATE);
  }, []);

  const handleClosePanel = useCallback(() => {
    setPanelOpen(false);
    setPanelStreamId(null);
    setPanelJobId(null);
  }, []);

  const statuses = useMemo(() => {
    const source =
      localStatuses.length > 0 ? localStatuses : data?.statuses || [];
    return [...source].sort((a, b) => a.column_order - b.column_order);
  }, [data?.statuses, localStatuses]);

  // Initialize local statuses from loader data
  useEffect(() => {
    if (data?.statuses && localStatuses.length === 0) {
      setLocalStatuses(data.statuses);
    }
  }, [data?.statuses, localStatuses.length]);

  const handleRenameStatus = useCallback(
    async (statusId: number, newName: string) => {
      // Optimistic update
      setLocalStatuses((prev) =>
        prev.map((s) =>
          s.status_id === statusId ? { ...s, name: newName } : s
        )
      );

      try {
        await db.application_statuses.update(statusId, { name: newName });
      } catch (error) {
        // Revert on error
        setLocalStatuses(data?.statuses || []);
        throw error;
      }
    },
    [data?.statuses]
  );

  // Transform data for KanbanProvider
  const kanbanColumns = useMemo(() => statuses.map(toKanbanStatus), [statuses]);

  const kanbanDataFromServer = useMemo(
    () => applications.map(toKanbanApplication),
    [applications]
  );

  // Local state for optimistic updates
  const [localKanbanData, setLocalKanbanData] = useState<KanbanApplication[]>(
    []
  );

  // Track which cards are currently being saved (passed to JobCard in Task 4)
  const [savingIds, setSavingIds] = useState<Set<string>>(new Set());

  // Notification hook
  const { notify } = useNotification();

  // Sync from server when data changes
  useEffect(() => {
    setLocalKanbanData(kanbanDataFromServer);
  }, [kanbanDataFromServer]);

  // Persist card move to Supabase
  const persistChange = useCallback(
    async (movedItem: KanbanApplication, newData: KanbanApplication[]) => {
      const newStatusId = Number(movedItem.column);

      // Get siblings in destination column (excluding moved item), sorted by order
      const siblings = newData
        .filter((d) => d.column === movedItem.column && d.id !== movedItem.id)
        .sort(
          (a, b) => a.application.status_order - b.application.status_order
        );

      // Find insert position
      const columnCards = newData.filter((d) => d.column === movedItem.column);
      const insertIndex = columnCards.findIndex((d) => d.id === movedItem.id);

      const newOrder = calculateNewOrder(siblings, insertIndex);

      try {
        await db.job_applications.update(movedItem.application.application_id, {
          status_id: newStatusId,
          status_order: newOrder,
        });
      } catch (error) {
        console.error("Failed to persist card move:", error);
        notify(
          t("pages.hq.kanban.errors.moveFailedTitle"),
          "error",
          t("pages.hq.kanban.errors.moveFailedMessage")
        );
        revalidator.revalidate();
      }
    },
    [notify, revalidator]
  );

  const handleDataChange = useCallback(
    (newData: KanbanApplication[]) => {
      const changed = findChangedItem(localKanbanData, newData);

      if (changed) {
        // Mark as saving
        setSavingIds((prev) => new Set(prev).add(changed.id));

        // Optimistic update
        setLocalKanbanData(newData);

        // Persist
        persistChange(changed, newData).finally(() => {
          setSavingIds((prev) => {
            const next = new Set(prev);
            next.delete(changed.id);
            return next;
          });
        });
      } else {
        setLocalKanbanData(newData);
      }
    },
    [localKanbanData, persistChange]
  );

  const empty = applications?.length === 0;
  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex items-center justify-between border-b-2 px-4 py-3 sm:px-6 lg:px-8">
        <div className="min-w-0 flex-1">
          <h2 className="text-2xl text-gray-900 sm:truncate sm:text-3xl">
            {t("pages.hq.title")}
          </h2>
        </div>
        <div className="flex md:mt-0 md:ml-4">
          {ingestionState.status === "idle" ? (
            <AddJobPopover
              actuator={t("pages.hq.actions.addJob")}
              onSubmit={handleSubmitJob}
            />
          ) : (
            <IngestionProgress
              currentStep={ingestionState.currentStep}
              error={ingestionState.error}
              onDismiss={
                ingestionState.status === "error"
                  ? handleDismissError
                  : undefined
              }
              status={
                ingestionState.status as "processing" | "success" | "error"
              }
              stepName={ingestionState.stepName}
              totalSteps={ingestionState.totalSteps}
            />
          )}
        </div>
      </div>
      <div className="min-h-0 flex-1 overflow-auto p-4">
        {empty ? (
          <div className="mt-6 flex flex-col items-start justify-start px-4 sm:px-6 lg:px-8">
            <h2 className="mb-2 text-2xl text-gray-900 sm:truncate sm:text-3xl">
              {t("pages.hq.empty.title")}
            </h2>
            <p className="text-gray-900">{t("pages.hq.empty.description")}</p>
            <Link className="font-semibold text-gray-900" to="/job-stream">
              {t("nav.jobList")} <span aria-hidden="true">&rarr;</span>
            </Link>
          </div>
        ) : (
          <KanbanProvider
            columns={kanbanColumns}
            data={localKanbanData}
            onDataChange={handleDataChange}
          >
            {(column: KanbanStatus) => (
              <KanbanBoard id={column.id} key={column.id}>
                <KanbanHeader className="kb-header mb-3 flex h-12 w-full items-center justify-between rounded-lg px-4">
                  <EditableText
                    className="font-semibold text-2xl text-white"
                    onSave={(newName) =>
                      handleRenameStatus(column.status.status_id, newName)
                    }
                    value={column.name}
                  />
                  <span className="font-extralight text-4xl text-white">
                    {
                      localKanbanData.filter((d) => d.column === column.id)
                        .length
                    }
                  </span>
                </KanbanHeader>
                <KanbanCards id={column.id}>
                  {(item: KanbanApplication) => (
                    <JobCard
                      columnIndex={kanbanColumns.findIndex(
                        (c) => c.id === item.column
                      )}
                      isSaving={savingIds.has(item.id)}
                      item={item}
                      key={item.id}
                      onRemove={async (appId) => {
                        await db.job_applications.remove(appId);
                        revalidator.revalidate();
                      }}
                      onViewDetails={(jobId) => {
                        console.log("[HQ] onViewDetails called", { jobId });
                        setPanelJobId(jobId);
                        setPanelOpen(true);
                        console.log("[HQ] Panel state set", {
                          panelJobId: jobId,
                          panelOpen: true,
                        });
                      }}
                      totalColumns={kanbanColumns.length}
                    />
                  )}
                </KanbanCards>
              </KanbanBoard>
            )}
          </KanbanProvider>
        )}
      </div>
      <JobDetailPanel
        isOpen={panelOpen}
        jobId={panelJobId}
        onClose={handleClosePanel}
        onTrackSuccess={() => revalidator.revalidate()}
        streamId={panelStreamId}
      />
    </div>
  );
}
