import { downloadResumePDF, Editor } from "@arcata/components";
import { db, type JobProfile, type JobProfileStatus } from "@arcata/db";
import { ArrowDownTrayIcon, ArrowLeftIcon } from "@heroicons/react/20/solid";
import type { SerializedEditorState } from "lexical";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  Link,
  type LoaderFunctionArgs,
  useLoaderData,
  useNavigate,
} from "react-router-dom";

const DEBOUNCE_MS = 500;

type LoaderData = {
  profile: JobProfile | null;
  error: Error | null;
};

/**
 * Loader function for ProfileBuilderPage
 * Fetches a single job profile by ID
 */
export async function loader({
  params,
}: LoaderFunctionArgs): Promise<LoaderData> {
  const { id } = params;
  if (!id) {
    return { profile: null, error: new Error("Profile ID is required") };
  }

  try {
    const profile = await db.job_profiles.get<JobProfile>({
      id: Number.parseInt(id, 10),
    });
    return { profile, error: null };
  } catch (error) {
    return { profile: null, error: error as Error };
  }
}

function getStatusBadgeClasses(status: JobProfileStatus): string {
  if (status === "live") {
    return "bg-green-50 text-green-700 ring-green-600/20";
  }
  return "bg-yellow-50 text-yellow-700 ring-yellow-600/20";
}

function getStatusLabel(status: JobProfileStatus): string {
  return status === "live" ? "Live" : "Draft";
}

type SaveStatus = "idle" | "saving" | "saved" | "error";
type DownloadStatus = "idle" | "generating" | "error";

type ProfileHeaderProps = {
  profile: JobProfile;
  saveStatus: SaveStatus;
  downloadStatus: DownloadStatus;
  onNameChange: (name: string) => void;
  onStatusToggle: () => void;
  onDownload: () => void;
};

function ProfileHeader({
  profile,
  saveStatus,
  downloadStatus,
  onNameChange,
  onStatusToggle,
  onDownload,
}: ProfileHeaderProps) {
  const [isEditingName, setIsEditingName] = useState(false);
  const [editedName, setEditedName] = useState(profile.name);

  const handleNameSubmit = () => {
    setIsEditingName(false);
    if (editedName.trim() && editedName !== profile.name) {
      onNameChange(editedName.trim());
    } else {
      setEditedName(profile.name);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleNameSubmit();
    } else if (e.key === "Escape") {
      setIsEditingName(false);
      setEditedName(profile.name);
    }
  };

  return (
    <header className="flex items-center justify-between border-gray-200 border-b bg-white px-4 py-3 lg:px-6 lg:py-4">
      {/* Left side: Back + Name */}
      <div className="flex min-w-0 items-center gap-x-4">
        <Link
          className="flex items-center gap-x-2 text-gray-500 text-sm hover:text-gray-700"
          to="/profiles"
        >
          <ArrowLeftIcon className="size-4" />
          <span className="hidden sm:inline">Profiles</span>
        </Link>

        <div className="h-6 w-px bg-gray-200" />

        {isEditingName ? (
          <input
            autoFocus
            className="min-w-0 flex-1 rounded-md border-0 bg-transparent px-2 py-1 font-semibold text-gray-900 text-lg ring-1 ring-gray-300 ring-inset focus:ring-2 focus:ring-gray-400"
            onBlur={handleNameSubmit}
            onChange={(e) => setEditedName(e.target.value)}
            onKeyDown={handleKeyDown}
            type="text"
            value={editedName}
          />
        ) : (
          <button
            className="min-w-0 truncate font-semibold text-gray-900 text-lg hover:text-gray-600"
            onClick={() => setIsEditingName(true)}
            type="button"
          >
            {profile.name}
          </button>
        )}
      </div>

      {/* Right side: Status + Actions */}
      <div className="flex shrink-0 items-center gap-x-3">
        {/* Save status indicator */}
        <span className="text-gray-500 text-sm">
          {saveStatus === "saving" && "Saving..."}
          {saveStatus === "saved" && "Saved"}
          {saveStatus === "error" && "Error saving"}
        </span>

        {/* Status badge (clickable) */}
        <button
          className={`inline-flex shrink-0 cursor-pointer items-center rounded-full px-2 py-1 font-medium text-xs ring-1 ring-inset transition-opacity hover:opacity-80 ${getStatusBadgeClasses(profile.status)}`}
          onClick={onStatusToggle}
          title={`Click to change to ${profile.status === "draft" ? "Live" : "Draft"}`}
          type="button"
        >
          {getStatusLabel(profile.status)}
        </button>

        {/* Download button */}
        <button
          className="inline-flex items-center gap-x-1.5 rounded-md bg-gray-900 px-3 py-2 font-semibold text-sm text-white shadow-sm hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={downloadStatus === "generating"}
          onClick={onDownload}
          type="button"
        >
          <ArrowDownTrayIcon className="-ml-0.5 size-4" />
          <span className="hidden sm:inline">
            {downloadStatus === "generating" ? "Generating..." : "Download"}
          </span>
        </button>
      </div>
    </header>
  );
}

export function ProfileBuilderPage() {
  const { profile, error } = useLoaderData() as LoaderData;
  const navigate = useNavigate();
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const [downloadStatus, setDownloadStatus] = useState<DownloadStatus>("idle");
  const [currentProfile, setCurrentProfile] = useState<JobProfile | null>(
    profile
  );

  const handleNameChange = useCallback(
    async (newName: string) => {
      if (!currentProfile) {
        return;
      }

      setSaveStatus("saving");
      try {
        await db.job_profiles.update(currentProfile.job_profile_id, {
          name: newName,
        });
        setCurrentProfile({ ...currentProfile, name: newName });
        setSaveStatus("saved");
        setTimeout(() => setSaveStatus("idle"), 2000);
      } catch (err) {
        console.error("Failed to update name:", err);
        setSaveStatus("error");
      }
    },
    [currentProfile]
  );

  const handleStatusToggle = useCallback(async () => {
    if (!currentProfile) {
      return;
    }

    const newStatus: JobProfileStatus =
      currentProfile.status === "draft" ? "live" : "draft";

    setSaveStatus("saving");
    try {
      await db.job_profiles.update(currentProfile.job_profile_id, {
        status: newStatus,
      });
      setCurrentProfile({ ...currentProfile, status: newStatus });
      setSaveStatus("saved");
      setTimeout(() => setSaveStatus("idle"), 2000);
    } catch (err) {
      console.error("Failed to update status:", err);
      setSaveStatus("error");
    }
  }, [currentProfile]);

  const handleDownload = useCallback(async () => {
    if (!currentProfile?.resume_data) {
      return;
    }

    setDownloadStatus("generating");
    try {
      const editorState =
        currentProfile.resume_data as unknown as SerializedEditorState;
      const filename = `${currentProfile.name.toLowerCase().replace(/\s+/g, "-")}-resume.pdf`;
      await downloadResumePDF(editorState, filename);
      setDownloadStatus("idle");
    } catch (err) {
      console.error("Failed to generate PDF:", err);
      setDownloadStatus("error");
      setTimeout(() => setDownloadStatus("idle"), 3000);
    }
  }, [currentProfile]);

  // Debounced auto-save for editor content
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingStateRef = useRef<SerializedEditorState | null>(null);

  const saveEditorContent = useCallback(
    async (editorState: SerializedEditorState) => {
      if (!currentProfile) {
        return;
      }

      setSaveStatus("saving");
      try {
        await db.job_profiles.update(currentProfile.job_profile_id, {
          resume_data: editorState as unknown as Record<string, unknown>,
        });
        setSaveStatus("saved");
        setTimeout(() => setSaveStatus("idle"), 2000);
      } catch (err) {
        console.error("Failed to save editor content:", err);
        setSaveStatus("error");
      }
    },
    [currentProfile]
  );

  const handleEditorChange = useCallback(
    (editorState: SerializedEditorState) => {
      // Store the pending state
      pendingStateRef.current = editorState;

      // Clear any existing timeout
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }

      // Show "Saving..." after a brief delay to avoid flicker
      setSaveStatus("saving");

      // Set up new debounced save
      saveTimeoutRef.current = setTimeout(() => {
        if (pendingStateRef.current) {
          saveEditorContent(pendingStateRef.current);
          pendingStateRef.current = null;
        }
      }, DEBOUNCE_MS);
    },
    [saveEditorContent]
  );

  // Save pending changes immediately (used for beforeunload)
  const savePendingChanges = useCallback(() => {
    if (pendingStateRef.current && currentProfile) {
      // Use navigator.sendBeacon for reliable saving on page unload
      const _data = JSON.stringify({
        resume_data: pendingStateRef.current,
      });
      // Fallback to sync XHR if sendBeacon not available
      db.job_profiles.update(currentProfile.job_profile_id, {
        resume_data: pendingStateRef.current as unknown as Record<
          string,
          unknown
        >,
      });
      pendingStateRef.current = null;
    }
  }, [currentProfile]);

  // Handle page unload - save pending changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (pendingStateRef.current) {
        savePendingChanges();
        // Show browser's default "unsaved changes" warning
        e.preventDefault();
        e.returnValue = "";
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [savePendingChanges]);

  // Cleanup timeout on unmount
  useEffect(
    () => () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
        savePendingChanges();
      }
    },
    [savePendingChanges]
  );

  if (error || !currentProfile) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="text-center">
          <h3 className="font-medium text-lg text-red-800">
            {error ? "Error loading profile" : "Profile not found"}
          </h3>
          <p className="mt-2 text-red-600 text-sm">
            {error?.message ?? "The requested profile could not be found."}
          </p>
          <button
            className="mt-4 text-indigo-600 text-sm hover:text-indigo-500"
            onClick={() => navigate("/profiles")}
            type="button"
          >
            Back to Profiles
          </button>
        </div>
      </div>
    );
  }

  // Parse the resume_data as SerializedEditorState if it exists
  const initialEditorState = currentProfile.resume_data as unknown as
    | SerializedEditorState
    | undefined;

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <ProfileHeader
        downloadStatus={downloadStatus}
        onDownload={handleDownload}
        onNameChange={handleNameChange}
        onStatusToggle={handleStatusToggle}
        profile={currentProfile}
        saveStatus={saveStatus}
      />

      {/* Editor Container */}
      <main className="flex-1 overflow-y-auto bg-gray-100 p-4 pb-20 lg:p-6 lg:pb-6">
        <div className="mx-auto max-w-4xl">
          <Editor
            editorSerializedState={initialEditorState}
            onSerializedChange={handleEditorChange}
          />
        </div>
      </main>
    </div>
  );
}
