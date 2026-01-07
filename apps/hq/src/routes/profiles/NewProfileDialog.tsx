import {
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@arcata/components";
import { getSession } from "@arcata/db";
import {
  DocumentPlusIcon,
  DocumentTextIcon,
} from "@heroicons/react/24/outline";
import type { SerializedEditorState } from "lexical";
import { useState } from "react";
import { parseResume } from "../../lib/api";
import { convertToEditorState } from "../../lib/resume-utils";
import { ImportProgress } from "./ImportProgress";
import { ResumeFileUpload } from "./ResumeFileUpload";

/** Regex to match file extensions (e.g., ".pdf", ".docx") for removal from filenames */
const FILE_EXTENSION_REGEX = /\.[^/.]+$/;

/** Progress thresholds for determining which message to display during import */
const PROGRESS_THRESHOLDS = {
  UPLOAD: 30,
  READING: 50,
  ANALYZING: 70,
  EXTRACTING: 85,
} as const;

/**
 * Returns the appropriate progress message based on the current progress value.
 * Messages indicate the current stage of resume processing.
 */
function getProgressMessage(progress: number): string {
  if (progress < PROGRESS_THRESHOLDS.UPLOAD) {
    return "Uploading resume...";
  }
  if (progress < PROGRESS_THRESHOLDS.READING) {
    return "Reading document structure...";
  }
  if (progress < PROGRESS_THRESHOLDS.ANALYZING) {
    return "Analyzing content with AI...";
  }
  if (progress < PROGRESS_THRESHOLDS.EXTRACTING) {
    return "Extracting experience and skills...";
  }
  return "Finalizing profile...";
}

type NewProfileDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (name: string, resumeData?: SerializedEditorState) => Promise<void>;
};

export function NewProfileDialog({
  open,
  onOpenChange,
  onCreate,
}: NewProfileDialogProps) {
  const [mode, setMode] = useState<"select" | "import">("select");
  const [isLoading, setIsLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [progressMessage, setProgressMessage] = useState("");
  const [error, setError] = useState<string | null>(null);

  const reset = () => {
    setMode("select");
    setIsLoading(false);
    setProgress(0);
    setProgressMessage("");
    setError(null);
  };

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      reset();
    }
    onOpenChange(newOpen);
  };

  const handleCreateBlank = async () => {
    setIsLoading(true);
    try {
      await onCreate("Untitled Profile");
      handleOpenChange(false);
    } catch (err) {
      console.error(err);
      setError("Failed to create profile");
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileSelect = async (file: File) => {
    setIsLoading(true);
    setError(null);
    setProgress(10);
    setProgressMessage("Uploading resume...");

    try {
      const session = await getSession();

      if (!session) {
        throw new Error("Not authenticated");
      }

      // Simulate progress for upload and processing
      let currentProgress = 0;

      const progressInterval = setInterval(() => {
        // Decaying increment to make it feel continuous but never reach 100% until done
        // Move 5% of the remaining distance to 95%
        const remaining = 95 - currentProgress;
        const increment = Math.max(0.5, remaining * 0.05);

        setProgress((prev) => {
          const next = Math.min(95, prev + increment);
          currentProgress = next;
          setProgressMessage(getProgressMessage(next));
          return next;
        });
      }, 200);

      const result = await parseResume(file, session.access_token);

      clearInterval(progressInterval);

      if (!result.ok) {
        throw new Error(result.error);
      }

      setProgress(95);
      setProgressMessage("Processing data...");

      const editorState = convertToEditorState(result.data);

      await onCreate(
        file.name.replace(FILE_EXTENSION_REGEX, "") || "Imported Profile",
        editorState
      );

      setProgress(100);
      handleOpenChange(false);
    } catch (err) {
      console.error(err);
      setError(err instanceof Error ? err.message : "Failed to import resume");
      setIsLoading(false);
    }
  };

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Create New Profile</DialogTitle>
          <DialogDescription>
            Start with a blank profile or import an existing resume.
          </DialogDescription>
        </DialogHeader>

        {mode === "select" && (
          <div className="grid grid-cols-2 gap-4 py-4">
            <button
              className="flex flex-col items-center justify-center rounded-lg border-2 border-gray-200 p-6 transition-colors hover:border-blue-500 hover:bg-blue-50"
              disabled={isLoading}
              onClick={handleCreateBlank}
              type="button"
            >
              <DocumentPlusIcon className="mb-2 h-10 w-10 text-gray-400" />
              <span className="font-medium text-gray-900 text-sm">
                Blank Profile
              </span>
            </button>
            <button
              className="flex flex-col items-center justify-center rounded-lg border-2 border-gray-200 p-6 transition-colors hover:border-blue-500 hover:bg-blue-50"
              disabled={isLoading}
              onClick={() => setMode("import")}
              type="button"
            >
              <DocumentTextIcon className="mb-2 h-10 w-10 text-gray-400" />
              <span className="font-medium text-gray-900 text-sm">
                Import Resume
              </span>
            </button>
          </div>
        )}

        {mode === "import" && (
          <div className="py-4">
            {isLoading ? (
              <ImportProgress message={progressMessage} progress={progress} />
            ) : (
              <ResumeFileUpload onFileSelect={handleFileSelect} />
            )}
            {error ? (
              <p className="mt-2 text-center text-red-600 text-sm">{error}</p>
            ) : null}
            {!isLoading && (
              <button
                className="mt-4 w-full text-center text-gray-500 text-sm hover:text-gray-900"
                onClick={() => setMode("select")}
                type="button"
              >
                Back to options
              </button>
            )}
          </div>
        )}

        <DialogFooter>
          <Button
            disabled={isLoading}
            onClick={() => handleOpenChange(false)}
            variant="outline"
          >
            Cancel
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
