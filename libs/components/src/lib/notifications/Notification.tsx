import { Transition } from "@headlessui/react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { Fragment } from "react";

export type NotificationVariant = "loading" | "success" | "error";

export type NotificationProps = {
  show: boolean;
  onClose: () => void;
  title: string;
  message?: string;
  variant: NotificationVariant;
};

function LoadingSpinner() {
  return (
    <svg
      aria-hidden="true"
      className="size-6 animate-spin text-gray-400"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        fill="currentColor"
      />
    </svg>
  );
}

function VariantIcon({ variant }: { variant: NotificationVariant }) {
  switch (variant) {
    case "loading":
      return <LoadingSpinner />;
    case "success":
      return (
        <CheckCircleIcon aria-hidden="true" className="size-6 text-green-400" />
      );
    case "error":
      return (
        <ExclamationCircleIcon
          aria-hidden="true"
          className="size-6 text-red-400"
        />
      );
    default:
      return null;
  }
}

export function Notification({
  show,
  onClose,
  title,
  message,
  variant,
}: NotificationProps) {
  return (
    <div
      aria-live="assertive"
      className="pointer-events-none fixed inset-0 z-50 flex items-end px-4 py-6 sm:items-start sm:p-6"
    >
      <div className="flex w-full flex-col items-center space-y-4 sm:items-end">
        <Transition
          as={Fragment}
          enter="transform ease-out duration-300 transition"
          enterFrom="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
          enterTo="translate-y-0 opacity-100 sm:translate-x-0"
          leave="transition ease-in duration-100"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
          show={show}
        >
          <div className="pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg bg-white shadow-lg ring-1 ring-black/5">
            <div className="p-4">
              <div className="flex items-start">
                <div className="shrink-0">
                  <VariantIcon variant={variant} />
                </div>
                <div className="ml-3 w-0 flex-1 pt-0.5">
                  <p className="font-medium text-gray-900 text-sm">{title}</p>
                  {message ? (
                    <p className="mt-1 text-gray-500 text-sm">{message}</p>
                  ) : null}
                </div>
                {variant !== "loading" ? (
                  <div className="ml-4 flex shrink-0">
                    <button
                      className="inline-flex rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                      onClick={onClose}
                      type="button"
                    >
                      <span className="sr-only">Close</span>
                      <XMarkIcon aria-hidden="true" className="size-5" />
                    </button>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  );
}
