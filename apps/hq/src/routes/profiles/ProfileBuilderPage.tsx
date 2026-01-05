import { downloadResumePDF, Editor } from "@arcata/components";
import { db, type JobProfile, type JobProfileStatus } from "@arcata/db";
import { Menu, Switch, Transition } from "@headlessui/react";
import {
  ArrowDownTrayIcon,
  ArrowLeftIcon,
  CheckIcon,
  EllipsisVerticalIcon,
  PencilIcon,
  TrashIcon,
  XMarkIcon,
} from "@heroicons/react/20/solid";
import type { SerializedEditorState } from "lexical";
import { Fragment, useCallback, useEffect, useRef, useState } from "react";
import {
  Link,
  type LoaderFunctionArgs,
  useLoaderData,
  useNavigate,
} from "react-router-dom";
import { CoverLettersManager } from "./CoverLettersManager";

function classNames(...classes: string[]) {
  return classes.filter(Boolean).join(" ");
}

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

// Remove unused helper functions since we now render status inline
// function getStatusBadgeClasses(status: JobProfileStatus): string { ... }
// function getStatusLabel(status: JobProfileStatus): string { ... }

// Status types
type SaveStatus = "idle" | "saving" | "saved" | "error";
type DownloadStatus = "idle" | "generating" | "error";

type ProfileHeaderProps = {
  profile: JobProfile;
  saveStatus: SaveStatus;
  downloadStatus: DownloadStatus;
  onNameChange: (name: string) => void;
  onStatusToggle: () => void;
  onDownload: () => void;
  onDelete: () => void;
};

function ProfileHeader({
  profile,
  saveStatus,
  downloadStatus,
  onNameChange,
  onStatusToggle,
  onDownload,
  onDelete,
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
      {/* Left side: Back + Name + Status */}
      <div className="flex min-w-0 flex-1 items-center gap-x-4">
        <Link
          className="group flex flex-col items-center justify-center rounded-md border border-gray-200 bg-white p-2 hover:bg-gray-50 hover:text-gray-900"
          title="Back to Profiles"
          to="/profiles"
        >
          <ArrowLeftIcon className="size-4 text-gray-500 group-hover:text-gray-900" />
        </Link>

        <div className="flex min-w-0 flex-col">
          <div className="flex items-center gap-x-2">
            {isEditingName ? (
              <div className="flex items-center gap-x-1">
                <input
                  autoFocus
                  className="block w-full min-w-[200px] rounded-lg border-0 bg-transparent px-2 py-1 font-bold text-gray-900 text-xl ring-1 ring-gray-900/5 transition-all duration-200 ease-in-out placeholder:text-gray-400 focus:bg-white focus:shadow-md focus:ring-1 focus:ring-gray-900/10 sm:leading-6"
                  onBlur={handleNameSubmit}
                  onChange={(e) => setEditedName(e.target.value)}
                  onKeyDown={handleKeyDown}
                  type="text"
                  value={editedName}
                />
                <button
                  className="rounded-full p-1 text-green-600 hover:bg-gray-100"
                  onMouseDown={(e) => {
                    e.preventDefault(); // Prevent blur
                    handleNameSubmit();
                  }}
                  type="button"
                >
                  <CheckIcon className="size-5" />
                </button>
                <button
                  className="rounded-full p-1 text-red-500 hover:bg-gray-100"
                  onClick={() => {
                    setIsEditingName(false);
                    setEditedName(profile.name);
                  }}
                  type="button"
                >
                  <XMarkIcon className="size-5" />
                </button>
              </div>
            ) : (
              <div className="group flex items-center gap-x-2">
                <button
                  className="cursor-pointer truncate rounded-lg border border-transparent px-2 py-1 text-left font-bold text-gray-900 text-xl transition-colors duration-200 hover:bg-gray-50"
                  onClick={() => setIsEditingName(true)}
                  title="Click to edit"
                  type="button"
                >
                  {profile.name}
                </button>
                <button
                  className="hidden rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 group-hover:block"
                  onClick={() => setIsEditingName(true)}
                  title="Edit name"
                  type="button"
                >
                  <PencilIcon className="size-4" />
                </button>
              </div>
            )}
          </div>
          <div className="flex items-center gap-x-2 text-xs">
            <span className="text-gray-500">
              {saveStatus === "saving" && "Saving..."}
              {saveStatus === "saved" && "Saved"}
              {saveStatus === "error" && "Error saving"}
              {saveStatus === "idle" && "All changes saved"}
            </span>
          </div>
        </div>
      </div>

      {/* Right side: Actions */}
      <div className="flex shrink-0 items-center gap-x-4">
        {/* Status Switch */}
        <div className="flex items-center gap-x-2">
          <span
            className={`text-sm ${profile.status === "draft" ? "font-medium text-gray-900" : "text-gray-500"}`}
          >
            Draft
          </span>
          <Switch
            checked={profile.status === "live"}
            className={`${
              profile.status === "live" ? "bg-green-600" : "bg-gray-200"
            } relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2`}
            onChange={onStatusToggle}
          >
            <span className="sr-only">Use setting</span>
            <span
              aria-hidden="true"
              className={`${
                profile.status === "live" ? "translate-x-5" : "translate-x-0"
              } pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out`}
            />
          </Switch>
          <span
            className={`text-sm ${profile.status === "live" ? "font-medium text-gray-900" : "text-gray-500"}`}
          >
            Live
          </span>
        </div>

        <div className="h-6 w-px bg-gray-200" />

        {/* Download button */}
        <button
          className="inline-flex items-center gap-x-1.5 rounded-md bg-gray-900 px-3 py-2 font-semibold text-sm text-white shadow-sm hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={downloadStatus === "generating"}
          onClick={onDownload}
          type="button"
        >
          <ArrowDownTrayIcon className="-ml-0.5 size-4" />
          <span className="hidden sm:inline">
            {downloadStatus === "generating" ? "Generating..." : "Download PDF"}
          </span>
        </button>

        {/* More Menu */}
        <Menu as="div" className="relative inline-block text-left">
          <Menu.Button className="flex items-center rounded-full p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-gray-100">
            <span className="sr-only">Open options</span>
            <EllipsisVerticalIcon aria-hidden="true" className="size-5" />
          </Menu.Button>

          <Transition
            as={Fragment}
            enter="transition ease-out duration-100"
            enterFrom="transform opacity-0 scale-95"
            enterTo="transform opacity-100 scale-100"
            leave="transition ease-in duration-75"
            leaveFrom="transform opacity-100 scale-100"
            leaveTo="transform opacity-0 scale-95"
          >
            <Menu.Items className="absolute right-0 z-10 mt-2 w-56 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
              <div className="py-1">
                <Menu.Item>
                  {({ active }) => (
                    <button
                      className={`${
                        active ? "bg-red-50 text-red-700" : "text-red-600"
                      } group flex w-full items-center px-4 py-2 text-sm`}
                      onClick={onDelete}
                      type="button"
                    >
                      <TrashIcon
                        aria-hidden="true"
                        className="mr-3 size-5 text-red-500 group-hover:text-red-600"
                      />
                      Delete Profile
                    </button>
                  )}
                </Menu.Item>
              </div>
            </Menu.Items>
          </Transition>
        </Menu>
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
  const [currentTab, setCurrentTab] = useState<"resume" | "cover-letters">(
    "resume"
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

  const handleDelete = useCallback(async () => {
    if (!currentProfile) {
      return;
    }

    if (
      // biome-ignore lint/suspicious/noAlert: Simple confirmation for now
      window.confirm(
        "Are you sure you want to delete this profile? This action cannot be undone."
      )
    ) {
      try {
        await db.job_profiles.remove(currentProfile.job_profile_id);
        navigate("/profiles");
      } catch (err) {
        console.error("Failed to delete profile:", err);
        // biome-ignore lint/suspicious/noAlert: Simple error handling
        alert("Failed to delete profile");
      }
    }
  }, [currentProfile, navigate]);

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
        onDelete={handleDelete}
        onDownload={handleDownload}
        onNameChange={handleNameChange}
        onStatusToggle={handleStatusToggle}
        profile={currentProfile}
        saveStatus={saveStatus}
      />

      <div className="flex flex-1 flex-col overflow-hidden">
        <div className="shrink-0 border-gray-200 border-b bg-white px-4 lg:px-6">
          <div className="-mb-px flex space-x-8">
            <button
              className={classNames(
                currentTab === "resume"
                  ? "border-gray-900 text-gray-900"
                  : "border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700",
                "whitespace-nowrap border-b-2 px-1 py-4 font-medium text-sm focus:outline-none"
              )}
              onClick={() => setCurrentTab("resume")}
              type="button"
            >
              Resume
            </button>
            <button
              className={classNames(
                currentTab === "cover-letters"
                  ? "border-gray-900 text-gray-900"
                  : "border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700",
                "whitespace-nowrap border-b-2 px-1 py-4 font-medium text-sm focus:outline-none"
              )}
              onClick={() => setCurrentTab("cover-letters")}
              type="button"
            >
              Cover Letters
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-hidden bg-gray-100">
          {currentTab === "resume" ? (
            <div className="h-full overflow-y-auto p-4 pb-20 lg:p-6 lg:pb-6">
              <div className="mx-auto max-w-4xl">
                <Editor
                  editorSerializedState={initialEditorState}
                  onSerializedChange={handleEditorChange}
                />
              </div>
            </div>
          ) : (
            <div className="h-full p-4 lg:p-6">
              <CoverLettersManager
                jobProfileId={currentProfile.job_profile_id}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
