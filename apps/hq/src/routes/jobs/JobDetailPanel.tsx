import {
  FiyaIcon,
  Skeleton,
  Spinner,
  useNotification,
} from "@arcata/components";
import { t } from "@arcata/translate";
import { Dialog, Transition } from "@headlessui/react";
import {
  BriefcaseIcon,
  LinkIcon,
  MapPinIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { Fragment, useState } from "react";
import type { JobDetail } from "./types";
import { useJobDetail } from "./useJobDetail";
import { useJobStreamEntry } from "./useJobStreamEntry";
import { useTrackJob } from "./useTrackJob";

export type JobDetailData = {
  title: string;
  company: string;
  url: string;
};

export type JobDetailPanelProps = {
  isOpen: boolean;
  onClose: () => void;
  /** Stream ID - used when viewing from job stream (enables tracking) */
  streamId?: number | null;
  /** Job ID - used when viewing from kanban (already tracked, no track button) */
  jobId?: number | null;
  onTrackSuccess?: () => void;
};

function JobDetailSkeleton() {
  return (
    <div className="animate-pulse">
      {/* Header skeleton */}
      <div className="space-y-2">
        <Skeleton height={14} variant="text" width={120} />
        <Skeleton height={24} variant="text" width="80%" />
      </div>

      {/* Metadata row skeleton */}
      <div className="mt-4 flex gap-4">
        <Skeleton height={16} variant="text" width={100} />
        <Skeleton height={16} variant="text" width={100} />
        <Skeleton height={16} variant="text" width={80} />
      </div>

      {/* Content skeleton */}
      <div className="mt-8 space-y-6">
        <div>
          <Skeleton height={16} variant="text" width={100} />
          <div className="mt-2 space-y-2">
            <Skeleton height={14} variant="text" width="100%" />
            <Skeleton height={14} variant="text" width="90%" />
            <Skeleton height={14} variant="text" width="95%" />
          </div>
        </div>
        <div>
          <Skeleton height={16} variant="text" width={120} />
          <div className="mt-2 space-y-2">
            <Skeleton height={14} variant="text" width="70%" />
            <Skeleton height={14} variant="text" width="65%" />
            <Skeleton height={14} variant="text" width="75%" />
          </div>
        </div>
      </div>
    </div>
  );
}

function ExpandableText({
  text,
  maxLength = 300,
}: {
  text: string;
  maxLength?: number;
}) {
  const [expanded, setExpanded] = useState(false);
  const shouldTruncate = text.length > maxLength;

  if (!shouldTruncate) {
    return (
      <p className="whitespace-pre-wrap text-gray-300 leading-relaxed">
        {text}
      </p>
    );
  }

  const displayText = expanded ? text : `${text.slice(0, maxLength)}...`;
  const buttonText = expanded
    ? t("pages.hq.jobDetail.showLess")
    : t("pages.hq.jobDetail.showMore");

  return (
    <div>
      <p className="whitespace-pre-wrap text-gray-300 leading-relaxed">
        {displayText}
      </p>
      <button
        className="mt-2 font-medium text-white underline decoration-gray-500 underline-offset-2 transition-colors hover:decoration-white"
        onClick={() => setExpanded(!expanded)}
        type="button"
      >
        {buttonText}
      </button>
    </div>
  );
}

function BulletList({ items }: { items: string[] }) {
  return (
    <ul className="mt-3 list-disc space-y-2 pl-5 marker:text-gray-500">
      {items.map((item) => (
        <li className="text-gray-300 leading-relaxed" key={item}>
          {item}
        </li>
      ))}
    </ul>
  );
}

function JobDetailContent({ job }: { job: JobDetail }) {
  const hasDescription = Boolean(job.description);
  const hasQualifications = job.qualifications && job.qualifications.length > 0;
  const hasResponsibilities =
    job.responsibilities && job.responsibilities.length > 0;

  return (
    <div className="space-y-8">
      {/* Description */}
      {hasDescription && job.description ? (
        <section>
          <h3 className="mb-3 font-semibold text-base text-white">
            {t("pages.hq.jobDetail.description")}
          </h3>
          <ExpandableText text={job.description} />
        </section>
      ) : null}

      {/* Qualifications */}
      {hasQualifications && job.qualifications ? (
        <section>
          <h3 className="font-semibold text-base text-white">
            {t("pages.hq.jobDetail.qualifications")}
          </h3>
          <BulletList items={job.qualifications} />
        </section>
      ) : null}

      {/* Responsibilities */}
      {hasResponsibilities && job.responsibilities ? (
        <section>
          <h3 className="font-semibold text-base text-white">
            {t("pages.hq.jobDetail.responsibilities")}
          </h3>
          <BulletList items={job.responsibilities} />
        </section>
      ) : null}
    </div>
  );
}

function LocationDisplay({
  location,
  isRemote,
}: {
  location: string | null;
  isRemote: boolean | null;
}) {
  if (location && isRemote) {
    return (
      <div className="flex items-center gap-1">
        <MapPinIcon className="size-4" />
        <span>{location} (Remote)</span>
      </div>
    );
  }
  if (location) {
    return (
      <div className="flex items-center gap-1">
        <MapPinIcon className="size-4" />
        <span>{location}</span>
      </div>
    );
  }
  if (isRemote) {
    return (
      <div className="flex items-center gap-1">
        <MapPinIcon className="size-4" />
        <span>Remote</span>
      </div>
    );
  }
  return null;
}

function TrackButton({
  isTracked,
  isTracking,
  canTrack,
  onClick,
}: {
  isTracked: boolean;
  isTracking: boolean;
  canTrack: boolean;
  onClick: () => void;
}) {
  const isDisabled = isTracking || isTracked || !canTrack;
  const buttonText = isTracked
    ? t("pages.hq.jobDetail.tracked")
    : t("pages.hq.jobDetail.track");

  const buttonClasses = isDisabled
    ? "cursor-not-allowed border border-gray-600 bg-transparent text-gray-400"
    : "border border-white bg-transparent text-white hover:bg-white/10";

  return (
    <button
      className={`inline-flex items-center gap-2 rounded-lg px-4 py-2 font-semibold text-sm transition-colors ${buttonClasses}`}
      disabled={isDisabled}
      onClick={onClick}
      type="button"
    >
      {isTracking ? <Spinner size="sm" /> : <FiyaIcon className="size-4" />}
      {buttonText}
    </button>
  );
}

function PanelContent({
  isLoading,
  error,
  job,
}: {
  isLoading: boolean;
  error: Error | null;
  job: JobDetail | null;
}) {
  if (isLoading) {
    return <JobDetailSkeleton />;
  }
  if (error) {
    return (
      <div className="text-center">
        <p className="text-gray-400">{t("pages.hq.jobDetail.error")}</p>
      </div>
    );
  }
  if (job) {
    return <JobDetailContent job={job} />;
  }
  return null;
}

export function JobDetailPanel({
  isOpen,
  onClose,
  streamId,
  jobId: directJobId,
  onTrackSuccess,
}: JobDetailPanelProps) {
  console.log("[JobDetailPanel] Render", {
    isOpen,
    streamId,
    directJobId,
  });

  const { data: streamEntry, refetch: refetchStream } = useJobStreamEntry(
    streamId ?? null
  );
  // Use direct jobId if provided, otherwise get from stream entry
  const jobId = directJobId ?? streamEntry?.jobId ?? null;
  // Job is tracked if direct jobId provided (from kanban) or stream entry has application
  const isTracked =
    directJobId !== undefined || Boolean(streamEntry?.applicationId);

  console.log("[JobDetailPanel] State", {
    jobId,
    isTracked,
    streamEntry,
  });

  const { data: job, isLoading, error } = useJobDetail(jobId);
  const { trackJob, isTracking } = useTrackJob();
  const { notify } = useNotification();

  // Can only track if we have a streamId (not direct jobId) and not already tracked
  const canTrack = streamId !== null && streamId !== undefined && !isTracked;

  async function handleTrack() {
    if (!(jobId && streamId)) {
      return;
    }

    try {
      await trackJob(jobId, streamId);
      // Refetch stream entry to get updated application_id
      await refetchStream();
      notify(
        t("pages.hq.jobDetail.toast.trackSuccess"),
        "success",
        undefined,
        3000
      );
      onTrackSuccess?.();
      onClose();
    } catch (err) {
      const message =
        err instanceof Error ? err.message : t("common.errors.generic");
      notify(t("pages.hq.jobDetail.toast.trackError"), "error", message, 5000);
    }
  }

  return (
    <Transition.Root as={Fragment} show={isOpen}>
      <Dialog as="div" className="relative z-50" onClose={onClose}>
        <Transition.Child
          as={Fragment}
          enter="ease-in-out duration-500"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in-out duration-500"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-black/60 transition-opacity" />
        </Transition.Child>

        <div className="fixed inset-0 overflow-hidden">
          <div className="absolute inset-0 overflow-hidden">
            <div className="pointer-events-none fixed inset-y-0 right-0 flex max-w-full pl-10 sm:pl-16">
              <Transition.Child
                as={Fragment}
                enter="transform transition ease-in-out duration-500 sm:duration-700"
                enterFrom="translate-x-full"
                enterTo="translate-x-0"
                leave="transform transition ease-in-out duration-500 sm:duration-700"
                leaveFrom="translate-x-0"
                leaveTo="translate-x-full"
              >
                <Dialog.Panel className="pointer-events-auto w-screen max-w-2xl">
                  <div className="flex h-full flex-col overflow-y-auto bg-[#111826] shadow-xl">
                    {/* Header with Navy Gradient */}
                    <div
                      className="px-6 py-6"
                      style={{ background: "var(--Navy-Gradient)" }}
                    >
                      <div className="flex items-start justify-between">
                        <div className="min-w-0 flex-1">
                          {isLoading ? (
                            <div className="space-y-2">
                              <Skeleton
                                height={14}
                                variant="text"
                                width={120}
                              />
                              <Skeleton
                                height={24}
                                variant="text"
                                width="60%"
                              />
                            </div>
                          ) : null}
                          {!isLoading && job ? (
                            <>
                              <p className="text-gray-400 text-sm">
                                {job.companyName}
                              </p>
                              <Dialog.Title className="mt-1 font-semibold text-white text-xl">
                                {job.title}
                              </Dialog.Title>
                            </>
                          ) : null}
                        </div>
                        <div className="ml-4 flex items-center gap-2">
                          <TrackButton
                            canTrack={canTrack}
                            isTracked={isTracked}
                            isTracking={isTracking}
                            onClick={handleTrack}
                          />
                          <button
                            className="rounded-md p-1 text-gray-400 transition-colors hover:text-white focus:outline-none"
                            onClick={onClose}
                            type="button"
                          >
                            <span className="sr-only">
                              {t("pages.hq.jobDetail.close")}
                            </span>
                            <XMarkIcon aria-hidden="true" className="size-6" />
                          </button>
                        </div>
                      </div>

                      {/* Metadata row */}
                      {!isLoading && job ? (
                        <div className="mt-4 flex flex-wrap items-center gap-4 text-gray-300 text-sm">
                          {job.category ? (
                            <div className="flex items-center gap-1.5">
                              <BriefcaseIcon className="size-4" />
                              <span>{job.category}</span>
                            </div>
                          ) : null}
                          <LocationDisplay
                            isRemote={job.isRemote}
                            location={job.location}
                          />
                          {job.sourceUrl ? (
                            <a
                              className="flex items-center gap-1.5 text-white underline decoration-gray-500 underline-offset-2 transition-colors hover:decoration-white"
                              href={job.sourceUrl}
                              rel="noopener noreferrer"
                              target="_blank"
                            >
                              <LinkIcon className="size-4" />
                              <span>{t("pages.hq.jobDetail.jobUrl")}</span>
                            </a>
                          ) : null}
                        </div>
                      ) : null}
                    </div>

                    {/* Content */}
                    <div className="flex-1 px-6 py-8">
                      <PanelContent
                        error={error}
                        isLoading={isLoading}
                        job={job}
                      />
                    </div>
                  </div>
                </Dialog.Panel>
              </Transition.Child>
            </div>
          </div>
        </div>
      </Dialog>
    </Transition.Root>
  );
}
