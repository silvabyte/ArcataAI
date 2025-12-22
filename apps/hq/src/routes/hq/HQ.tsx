import {
  type ApplicationStatus,
  db,
  getCurrentUser,
  type JobApplication,
} from "@arcata/db";
import { t } from "@arcata/translate";
import type { User } from "@supabase/supabase-js";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLoaderData } from "react-router-dom";
import { AddJobPopover } from "../jobs/AddJobByUrl";
import KanbanLane from "./kanban/KanbanLane";

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

  const applications = useMemo(
    () => (data?.applications as JobApplication[]) || [],
    [data?.applications]
  );

  const [localStatuses, setLocalStatuses] = useState<ApplicationStatus[]>([]);

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
          <AddJobPopover actuator={t("pages.hq.actions.addJob")} />
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
    </div>
  );
}
