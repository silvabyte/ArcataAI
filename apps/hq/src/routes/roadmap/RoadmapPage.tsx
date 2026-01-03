import { CheckIcon } from "@heroicons/react/20/solid";
import { SparklesIcon } from "@heroicons/react/24/outline";

type Step = {
  name: string;
  description: string;
  status: "complete" | "current" | "upcoming";
};

const alphaSteps: Step[] = [
  {
    name: "Core Platform",
    description:
      "Job tracking, kanban board, and profile builder - the foundation is live.",
    status: "complete",
  },
  {
    name: "Resume Import",
    description:
      "Upload your resume (PDF, Word, etc.) and let AI build your profile automatically.",
    status: "upcoming",
  },
  {
    name: "Chrome Extension",
    description:
      "Track jobs directly from any job posting page with one click.",
    status: "upcoming",
  },
  {
    name: "PDF/Markdown Export",
    description: "Download your profile as a formatted resume.",
    status: "upcoming",
  },
  {
    name: "Job Status Checker",
    description: "Automatically verify if tracked jobs are still open.",
    status: "upcoming",
  },
  {
    name: "Job Discovery",
    description: "Surface new opportunities from across the web.",
    status: "upcoming",
  },
];

const betaSteps: Step[] = [
  {
    name: "Smart Job Matching",
    description:
      "AI-powered recommendations based on your profile and preferences.",
    status: "upcoming",
  },
  {
    name: "Conversation Tracking",
    description: "Centralize emails, calls, and notes for each application.",
    status: "upcoming",
  },
  {
    name: "Auto-Apply Wizard",
    description: "AI-assisted form filling for faster applications.",
    status: "upcoming",
  },
  {
    name: "AI Cover Letters",
    description: "Generate personalized cover letters for each job.",
    status: "upcoming",
  },
];

function classNames(...classes: (string | boolean | undefined)[]) {
  return classes.filter(Boolean).join(" ");
}

function SectionHeader({ title, label }: { title: string; label: string }) {
  return (
    <div className="mb-8 border-gray-200 border-b pb-5">
      <div className="-mt-2 -ml-2 flex flex-wrap items-baseline">
        <h3 className="mt-2 ml-2 font-semibold text-base text-gray-900">
          {title}
        </h3>
        <p className="mt-1 ml-2 truncate text-gray-500 text-sm">{label}</p>
      </div>
    </div>
  );
}

function StepConnector({
  show,
}: {
  show: boolean;
  variant?: "brand" | "gray";
}) {
  if (!show) {
    return null;
  }
  return (
    <div
      aria-hidden="true"
      className="absolute top-4 left-4 mt-0.5 -ml-px h-full w-0.5 bg-gray-300"
    />
  );
}

function StepConnectorBrand({ show }: { show: boolean }) {
  if (!show) {
    return null;
  }
  return (
    <div
      aria-hidden="true"
      className="absolute top-4 left-4 mt-0.5 -ml-px h-full w-0.5"
      style={{ backgroundColor: "#273655" }}
    />
  );
}

function CompleteStep({
  step,
  showConnector,
}: {
  step: Step;
  showConnector: boolean;
}) {
  return (
    <>
      <StepConnectorBrand show={showConnector} />
      <div className="group relative flex items-start">
        <span className="flex h-9 items-center">
          <span
            className="relative z-10 flex size-8 items-center justify-center rounded-full"
            style={{ backgroundColor: "#273655" }}
          >
            <CheckIcon aria-hidden="true" className="size-5 text-white" />
          </span>
        </span>
        <span className="ml-4 flex min-w-0 flex-col">
          <span className="font-medium text-gray-900 text-sm">{step.name}</span>
          <span className="text-gray-500 text-sm">{step.description}</span>
        </span>
      </div>
    </>
  );
}

function CurrentStep({
  step,
  showConnector,
}: {
  step: Step;
  showConnector: boolean;
}) {
  return (
    <>
      <StepConnector show={showConnector} />
      <div aria-current="step" className="group relative flex items-start">
        <span aria-hidden="true" className="flex h-9 items-center">
          <span
            className="relative z-10 flex size-8 items-center justify-center rounded-full border-2 bg-white"
            style={{ borderColor: "#273655" }}
          >
            <span
              className="size-2.5 rounded-full"
              style={{ backgroundColor: "#273655" }}
            />
          </span>
        </span>
        <span className="ml-4 flex min-w-0 flex-col">
          <span className="font-medium text-sm" style={{ color: "#273655" }}>
            {step.name}
          </span>
          <span className="text-gray-500 text-sm">{step.description}</span>
        </span>
      </div>
    </>
  );
}

function UpcomingStep({
  step,
  showConnector,
}: {
  step: Step;
  showConnector: boolean;
}) {
  return (
    <>
      <StepConnector show={showConnector} />
      <div className="group relative flex items-start">
        <span aria-hidden="true" className="flex h-9 items-center">
          <span className="relative z-10 flex size-8 items-center justify-center rounded-full border-2 border-gray-300 bg-white group-hover:border-gray-400">
            <span className="size-2.5 rounded-full bg-transparent group-hover:bg-gray-300" />
          </span>
        </span>
        <span className="ml-4 flex min-w-0 flex-col">
          <span className="font-medium text-gray-500 text-sm">{step.name}</span>
          <span className="text-gray-500 text-sm">{step.description}</span>
        </span>
      </div>
    </>
  );
}

function StepItem({
  step,
  showConnector,
}: {
  step: Step;
  showConnector: boolean;
}) {
  if (step.status === "complete") {
    return <CompleteStep showConnector={showConnector} step={step} />;
  }
  if (step.status === "current") {
    return <CurrentStep showConnector={showConnector} step={step} />;
  }
  return <UpcomingStep showConnector={showConnector} step={step} />;
}

function StepsList({ steps }: { steps: Step[] }) {
  return (
    <ol className="overflow-hidden">
      {steps.map((step, stepIdx) => (
        <li
          className={classNames(
            stepIdx !== steps.length - 1 ? "pb-10" : "",
            "relative"
          )}
          key={step.name}
        >
          <StepItem showConnector={stepIdx !== steps.length - 1} step={step} />
        </li>
      ))}
    </ol>
  );
}

export default function RoadmapPage() {
  return (
    <div className="min-h-full bg-gray-50 px-6 py-10 lg:px-8">
      <div className="mx-auto max-w-2xl">
        <div className="mb-12">
          <h1 className="font-bold text-3xl text-gray-900 tracking-tight">
            Product Roadmap
          </h1>
          <p className="mt-2 text-gray-600">
            Follow our journey as we build the future of job hunting. Your
            feedback shapes what comes next.
          </p>
        </div>

        <nav aria-label="Progress">
          {/* Alpha Section */}
          <section className="mb-12">
            <SectionHeader label="Current Phase" title="Alpha" />
            <StepsList steps={alphaSteps} />
          </section>

          {/* Beta Section */}
          <section className="mb-12">
            <SectionHeader label="Coming Soon" title="Beta" />
            <StepsList steps={betaSteps} />
          </section>

          {/* GA v1.0 Section */}
          <section>
            <div className="mb-8 border-gray-200 border-b pb-5">
              <div className="-mt-2 -ml-2 flex flex-wrap items-center gap-2">
                <SparklesIcon className="mt-2 ml-2 h-6 w-6 text-yellow-500" />
                <h3 className="mt-2 font-semibold text-base text-gray-900">
                  GA v1.0
                </h3>
              </div>
            </div>
            <p className="text-gray-600">
              All features stable, battle-tested, and refined by community
              feedback. We're building this together.
            </p>
          </section>
        </nav>
      </div>
    </div>
  );
}
