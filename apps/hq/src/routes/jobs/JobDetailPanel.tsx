import { t } from "@arcata/translate";
import { Dialog, Transition } from "@headlessui/react";
import {
  ArrowTopRightOnSquareIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { Fragment } from "react";

export type JobDetailData = {
  title: string;
  company: string;
  url: string;
};

export type JobDetailPanelProps = {
  isOpen: boolean;
  onClose: () => void;
  jobId: number | null;
  jobData?: JobDetailData;
};

export function JobDetailPanel({
  isOpen,
  onClose,
  jobId,
  jobData,
}: JobDetailPanelProps) {
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
          <div className="fixed inset-0 bg-gray-500/75 transition-opacity" />
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
                  <div className="flex h-full flex-col overflow-y-auto bg-white shadow-xl">
                    {/* Header */}
                    <div className="bg-gray-50 px-4 py-6 sm:px-6">
                      <div className="flex items-start justify-between space-x-3">
                        <div className="space-y-1">
                          <Dialog.Title className="font-semibold text-gray-900 text-lg">
                            {jobData?.title || t("pages.hq.jobDetail.title")}
                          </Dialog.Title>
                          {jobData?.company ? (
                            <p className="text-gray-500 text-sm">
                              {jobData.company}
                            </p>
                          ) : null}
                        </div>
                        <div className="flex h-7 items-center">
                          <button
                            className="relative rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                            onClick={onClose}
                            type="button"
                          >
                            <span className="sr-only">Close panel</span>
                            <XMarkIcon aria-hidden="true" className="size-6" />
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* Content */}
                    <div className="flex-1 px-4 py-6 sm:px-6">
                      {/* Original posting link */}
                      {jobData?.url ? (
                        <div className="mb-6">
                          <a
                            className="inline-flex items-center gap-2 font-medium text-indigo-600 text-sm hover:text-indigo-500"
                            href={jobData.url}
                            rel="noopener noreferrer"
                            target="_blank"
                          >
                            <ArrowTopRightOnSquareIcon className="size-4" />
                            {t("pages.hq.jobDetail.viewPosting")}
                          </a>
                        </div>
                      ) : null}

                      {/* Placeholder sections */}
                      <div className="space-y-6">
                        <section>
                          <h3 className="font-medium text-gray-900 text-sm">
                            Job Description
                          </h3>
                          <p className="mt-2 text-gray-500 text-sm">
                            {t("pages.hq.jobDetail.placeholder.description")}
                          </p>
                        </section>

                        <section>
                          <h3 className="font-medium text-gray-900 text-sm">
                            Requirements
                          </h3>
                          <p className="mt-2 text-gray-500 text-sm">
                            {t("pages.hq.jobDetail.placeholder.requirements")}
                          </p>
                        </section>

                        <section>
                          <h3 className="font-medium text-gray-900 text-sm">
                            Match Score
                          </h3>
                          <p className="mt-2 text-gray-500 text-sm">
                            {t("pages.hq.jobDetail.placeholder.matchScore")}
                          </p>
                        </section>

                        <section>
                          <h3 className="font-medium text-gray-900 text-sm">
                            Notes
                          </h3>
                          <p className="mt-2 text-gray-500 text-sm">
                            {t("pages.hq.jobDetail.placeholder.notes")}
                          </p>
                        </section>
                      </div>

                      {/* Debug info */}
                      {jobId ? (
                        <div className="mt-8 rounded-md bg-gray-100 p-4">
                          <p className="text-gray-500 text-xs">
                            Job ID: {jobId}
                          </p>
                        </div>
                      ) : null}
                    </div>

                    {/* Footer */}
                    <div className="shrink-0 border-gray-200 border-t px-4 py-5 sm:px-6">
                      <div className="flex justify-end space-x-3">
                        <button
                          className="rounded-md bg-white px-3 py-2 font-semibold text-gray-900 text-sm shadow-sm ring-1 ring-gray-300 ring-inset hover:bg-gray-50"
                          onClick={onClose}
                          type="button"
                        >
                          Close
                        </button>
                        <button
                          className="rounded-md bg-indigo-600 px-3 py-2 font-semibold text-sm text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600 focus-visible:outline-offset-2"
                          disabled
                          type="button"
                        >
                          Track Job
                        </button>
                      </div>
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
