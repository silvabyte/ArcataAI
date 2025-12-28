import { createDefaultResumeTemplate } from "@arcata/components";
import { db, type JobProfile } from "@arcata/db";
import { EyeIcon, PencilIcon, PlusIcon } from "@heroicons/react/20/solid";
import { DocumentPlusIcon } from "@heroicons/react/24/outline";
import { useCallback, useState } from "react";
import {
  type LoaderFunctionArgs,
  useLoaderData,
  useNavigate,
} from "react-router-dom";

type LoaderData = {
  profiles: JobProfile[];
  error: Error | null;
};

/**
 * Loader function for ProfilesPage
 * Fetches all job profiles for the current user
 */
export async function loader(_args: LoaderFunctionArgs): Promise<LoaderData> {
  try {
    const profiles = await db.job_profiles.list<JobProfile>({
      order: { key: "updated_at", meta: { ascending: false } },
    });
    return { profiles: profiles as JobProfile[], error: null };
  } catch (error) {
    return { profiles: [], error: error as Error };
  }
}

function formatDate(dateString: string | null): string {
  if (!dateString) {
    return "";
  }
  const date = new Date(dateString);
  return date.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function getStatusBadgeClasses(status: JobProfile["status"]): string {
  if (status === "live") {
    return "bg-green-50 text-green-700 ring-green-600/20 dark:bg-green-500/10 dark:text-green-500 dark:ring-green-500/30";
  }
  return "bg-yellow-50 text-yellow-700 ring-yellow-600/20 dark:bg-yellow-500/10 dark:text-yellow-500 dark:ring-yellow-500/30";
}

function getStatusLabel(status: JobProfile["status"]): string {
  return status === "live" ? "Live" : "Draft";
}

type EmptyStateProps = {
  onCreateProfile: () => void;
  isCreating: boolean;
};

function EmptyState({ onCreateProfile, isCreating }: EmptyStateProps) {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="text-center">
        <DocumentPlusIcon
          aria-hidden="true"
          className="mx-auto size-12 text-gray-400 dark:text-gray-500"
        />
        <h3 className="mt-2 font-semibold text-gray-900 text-sm dark:text-white">
          No profiles yet
        </h3>
        <p className="mt-1 text-gray-500 text-sm dark:text-gray-400">
          Get started by creating a new profile.
        </p>
        <div className="mt-6">
          <button
            className="inline-flex items-center rounded-md bg-indigo-600 px-3 py-2 font-semibold text-sm text-white shadow-xs hover:bg-indigo-500 focus-visible:outline-2 focus-visible:outline-indigo-600 focus-visible:outline-offset-2 disabled:opacity-50 dark:bg-indigo-500 dark:shadow-none dark:focus-visible:outline-indigo-500 dark:hover:bg-indigo-400"
            disabled={isCreating}
            onClick={onCreateProfile}
            type="button"
          >
            <PlusIcon aria-hidden="true" className="mr-1.5 -ml-0.5 size-5" />
            {isCreating ? "Creating..." : "New Profile"}
          </button>
        </div>
      </div>
    </div>
  );
}

type ProfileCardProps = {
  profile: JobProfile;
  onView: (id: number) => void;
  onEdit: (id: number) => void;
};

function ProfileCard({ profile, onView, onEdit }: ProfileCardProps) {
  return (
    <li className="col-span-1 divide-y divide-gray-200 rounded-lg bg-white shadow-sm dark:divide-white/10 dark:bg-gray-800/50 dark:shadow-none dark:outline dark:outline-white/10 dark:-outline-offset-1">
      <div className="flex w-full items-center justify-between space-x-6 p-6">
        <div className="flex-1 truncate">
          <div className="flex items-center space-x-3">
            <h3 className="truncate font-medium text-gray-900 text-sm dark:text-white">
              {profile.name}
            </h3>
            <span
              className={`inline-flex shrink-0 items-center rounded-full px-1.5 py-0.5 font-medium text-xs ring-1 ring-inset ${getStatusBadgeClasses(profile.status)}`}
            >
              {getStatusLabel(profile.status)}
            </span>
          </div>
          <p className="mt-1 truncate text-gray-500 text-sm dark:text-gray-400">
            Updated {formatDate(profile.updated_at)}
          </p>
        </div>
      </div>
      <div>
        <div className="-mt-px flex divide-x divide-gray-200 dark:divide-white/10">
          <div className="flex w-0 flex-1">
            <button
              className="relative -mr-px inline-flex w-0 flex-1 items-center justify-center gap-x-3 rounded-bl-lg border border-transparent py-4 font-semibold text-gray-900 text-sm dark:text-white"
              onClick={() => onView(profile.job_profile_id)}
              type="button"
            >
              <EyeIcon
                aria-hidden="true"
                className="size-5 text-gray-400 dark:text-gray-500"
              />
              View
            </button>
          </div>
          <div className="-ml-px flex w-0 flex-1">
            <button
              className="relative inline-flex w-0 flex-1 items-center justify-center gap-x-3 rounded-br-lg border border-transparent py-4 font-semibold text-gray-900 text-sm dark:text-white"
              onClick={() => onEdit(profile.job_profile_id)}
              type="button"
            >
              <PencilIcon
                aria-hidden="true"
                className="size-5 text-gray-400 dark:text-gray-500"
              />
              Edit
            </button>
          </div>
        </div>
      </div>
    </li>
  );
}

type ProfileGridProps = {
  profiles: JobProfile[];
  onView: (id: number) => void;
  onEdit: (id: number) => void;
  onCreateProfile: () => void;
  isCreating: boolean;
};

function ProfileGrid({
  profiles,
  onView,
  onEdit,
  onCreateProfile,
  isCreating,
}: ProfileGridProps) {
  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <div className="sm:flex sm:items-center">
        <div className="sm:flex-auto">
          <h1 className="font-semibold text-base text-gray-900 leading-6 dark:text-white">
            Profiles
          </h1>
          <p className="mt-2 text-gray-700 text-sm dark:text-gray-400">
            Manage your job profiles and resumes.
          </p>
        </div>
        <div className="mt-4 sm:mt-0 sm:ml-16 sm:flex-none">
          <button
            className="inline-flex items-center rounded-md bg-indigo-600 px-3 py-2 font-semibold text-sm text-white shadow-xs hover:bg-indigo-500 focus-visible:outline-2 focus-visible:outline-indigo-600 focus-visible:outline-offset-2 disabled:opacity-50 dark:bg-indigo-500 dark:shadow-none dark:focus-visible:outline-indigo-500 dark:hover:bg-indigo-400"
            disabled={isCreating}
            onClick={onCreateProfile}
            type="button"
          >
            <PlusIcon aria-hidden="true" className="mr-1.5 -ml-0.5 size-5" />
            {isCreating ? "Creating..." : "New Profile"}
          </button>
        </div>
      </div>
      <ul className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {profiles.map((profile) => (
          <ProfileCard
            key={profile.job_profile_id}
            onEdit={onEdit}
            onView={onView}
            profile={profile}
          />
        ))}
      </ul>
    </div>
  );
}

export function ProfilesPage() {
  const { profiles, error } = useLoaderData() as LoaderData;
  const navigate = useNavigate();
  const [isCreating, setIsCreating] = useState(false);

  const handleCreateProfile = useCallback(async () => {
    setIsCreating(true);
    try {
      // Get current user's profile_id from the first profile or fetch it
      // For now, we'll use the supabase auth to get the user id
      // TODO: current user should be loaded from loader on page load...
      const { getCurrentUser } = await import("@arcata/db");
      const user = await getCurrentUser();
      if (!user) {
        throw new Error("Not authenticated");
      }

      const result = await db.job_profiles.create<JobProfile[]>({
        profile_id: user.id,
        name: "Untitled Profile",
        status: "draft",
        resume_data: createDefaultResumeTemplate(),
      });

      const created = result[0];
      if (created) {
        navigate(`/profiles/${created.job_profile_id}`);
      }
    } catch (err) {
      console.error("Failed to create profile:", err);
      setIsCreating(false);
    }
  }, [navigate]);

  const handleView = useCallback(
    (id: number) => {
      // For now, view just navigates to edit
      navigate(`/profiles/${id}`);
    },
    [navigate]
  );

  const handleEdit = useCallback(
    (id: number) => {
      navigate(`/profiles/${id}`);
    },
    [navigate]
  );

  if (error) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="rounded-lg bg-red-50 p-6 dark:bg-red-900/20">
          <h3 className="font-medium text-lg text-red-800 dark:text-red-400">
            Error loading profiles
          </h3>
          <p className="mt-2 text-red-600 text-sm dark:text-red-300">
            {error.message}
          </p>
        </div>
      </div>
    );
  }

  if (profiles.length === 0) {
    return (
      <EmptyState
        isCreating={isCreating}
        onCreateProfile={handleCreateProfile}
      />
    );
  }

  return (
    <ProfileGrid
      isCreating={isCreating}
      onCreateProfile={handleCreateProfile}
      onEdit={handleEdit}
      onView={handleView}
      profiles={profiles}
    />
  );
}
