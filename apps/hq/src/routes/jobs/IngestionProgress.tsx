import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  XMarkIcon,
} from "@heroicons/react/24/solid";

export type IngestionProgressProps = {
  status: "processing" | "success" | "error";
  currentStep: number;
  totalSteps: number;
  stepName: string;
  error?: string;
  onDismiss?: () => void;
};

type StepDotsProps = {
  currentStep: number;
  totalSteps: number;
  status: IngestionProgressProps["status"];
};

function getDotClassName(
  index: number,
  currentStep: number,
  status: IngestionProgressProps["status"]
): string {
  const baseClasses = "size-2 rounded-full transition-all duration-300";
  const isError = status === "error" && index === currentStep;
  const isComplete = status === "success" || index < currentStep;
  const isCurrent = status === "processing" && index === currentStep;

  if (isError) {
    return `${baseClasses} bg-red-500`;
  }
  if (isComplete) {
    return `${baseClasses} bg-indigo-600`;
  }
  if (isCurrent) {
    return `${baseClasses} animate-pulse bg-indigo-600 ring-2 ring-indigo-300`;
  }
  return `${baseClasses} bg-gray-300`;
}

function StepDots({ currentStep, totalSteps, status }: StepDotsProps) {
  return (
    <div className="flex items-center gap-1.5">
      {Array.from({ length: totalSteps }, (_, index) => (
        <div
          className={getDotClassName(index, currentStep, status)}
          key={index}
        />
      ))}
    </div>
  );
}

type StepLabelProps = {
  stepName: string;
  currentStep: number;
  totalSteps: number;
  status: IngestionProgressProps["status"];
  error?: string;
  onDismiss?: () => void;
};

function StepLabel({
  stepName,
  currentStep,
  totalSteps,
  status,
  error,
  onDismiss,
}: StepLabelProps) {
  if (status === "success") {
    return (
      <div className="flex items-center gap-1.5 text-green-600">
        <CheckCircleIcon className="size-4" />
        <span className="font-medium text-sm">Job added!</span>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="flex flex-col items-center gap-1">
        <div className="flex items-center gap-1.5 text-red-600">
          <ExclamationCircleIcon className="size-4" />
          <span className="font-medium text-sm">{stepName}</span>
          {onDismiss ? (
            <button
              className="ml-1 rounded-full p-0.5 text-red-500 hover:bg-red-100 hover:text-red-700"
              onClick={onDismiss}
              type="button"
            >
              <XMarkIcon className="size-3.5" />
              <span className="sr-only">Dismiss</span>
            </button>
          ) : null}
        </div>
        {error ? (
          <span className="max-w-xs text-center text-red-500 text-xs">
            {error}
          </span>
        ) : null}
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-0.5">
      <span className="text-gray-700 text-sm">
        {stepName}
        <span className="inline-flex w-4">
          <span className="animate-[bounce_1s_infinite_0ms]">.</span>
          <span className="animate-[bounce_1s_infinite_150ms]">.</span>
          <span className="animate-[bounce_1s_infinite_300ms]">.</span>
        </span>
      </span>
      <span className="text-gray-500 text-xs">
        Step {currentStep + 1} of {totalSteps}
      </span>
    </div>
  );
}

export function IngestionProgress({
  status,
  currentStep,
  totalSteps,
  stepName,
  error,
  onDismiss,
}: IngestionProgressProps) {
  return (
    <div className="flex flex-col items-center gap-2">
      <StepDots
        currentStep={currentStep}
        status={status}
        totalSteps={totalSteps}
      />
      <StepLabel
        currentStep={currentStep}
        error={error}
        onDismiss={onDismiss}
        status={status}
        stepName={stepName}
        totalSteps={totalSteps}
      />
    </div>
  );
}
