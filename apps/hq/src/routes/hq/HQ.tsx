import {
  type ApplicationStatus,
  db,
  getCurrentUser,
  getSession,
  type JobApplication,
} from "@arcata/db";
import { t } from "@arcata/translate";
import type { User } from "@supabase/supabase-js";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLoaderData, useRevalidator } from "react-router-dom";
import { ingestJobWithProgress, type ProgressUpdate } from "../../lib/api";
import { AddJobPopover, IngestionProgress, JobDetailPanel } from "../jobs";
import KanbanLane from "./kanban/KanbanLane";

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
      db.job_applications.list<JobApplication>({
        eq: { key: "profile_id", value: user?.id },
      }),
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
    applications: JobApplication[] | null;
    statuses: ApplicationStatus[] | null;
  } | null;

  const revalidator = useRevalidator();

  const applications = useMemo(
    () => (data?.applications as JobApplication[]) || [],
    [data?.applications]
  );

  const [localStatuses, setLocalStatuses] = useState<ApplicationStatus[]>([]);

  // Ingestion state
  const [ingestionState, setIngestionState] =
    useState<IngestionState>(INITIAL_STATE);

  // Job detail panel state - only need streamId, panel fetches the rest
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelStreamId, setPanelStreamId] = useState<number | null>(null);

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

  const boardData = useMemo(
    () =>
      statuses.map((status) => ({
        status,
        apps: applications.filter((a) => a.status_id === status.status_id),
      })),
    [statuses, applications]
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
      <div className="h-full flex-1 overflow-auto p-4">
        <div className="flex h-full gap-3">
          {boardData.map(({ status, apps }) => (
            <KanbanLane
              count={apps.length}
              key={status.status_id}
              onRename={handleRenameStatus}
              statusId={status.status_id}
              title={status.name}
            >
              {apps.map((a) => (
                <div key={a.application_id}>{a.job_id}</div>
              ))}
            </KanbanLane>
          ))}
        </div>
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
        ) : null}
      </div>
      <JobDetailPanel
        isOpen={panelOpen}
        onClose={handleClosePanel}
        onTrackSuccess={() => revalidator.revalidate()}
        streamId={panelStreamId}
      />
    </div>
  );
}
