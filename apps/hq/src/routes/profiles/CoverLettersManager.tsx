import { Button, Card, Input } from "@arcata/components";
import {
  type CoverLetter as DBCoverLetter,
  db,
  getCurrentUser,
} from "@arcata/db";
import { PlusIcon, TrashIcon } from "@heroicons/react/24/outline";
import { useCallback, useEffect, useRef, useState } from "react";

const DEBOUNCE_MS = 500;

type CoverLettersManagerProps = {
  jobProfileId: number;
};

export function CoverLettersManager({
  jobProfileId,
}: CoverLettersManagerProps) {
  const [coverLetters, setCoverLetters] = useState<DBCoverLetter[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const selectedLetter = coverLetters.find(
    (cl) => cl.cover_letter_id === selectedId
  );

  // Load cover letters on mount
  useEffect(() => {
    async function loadCoverLetters() {
      setIsLoading(true);
      setError(null);
      try {
        const letters = await db.cover_letters.list<DBCoverLetter>({
          eq: { key: "job_profile_id", value: jobProfileId },
          order: { key: "updated_at", meta: { ascending: false } },
        });
        setCoverLetters(letters as DBCoverLetter[]);
      } catch (err) {
        console.error("Failed to load cover letters:", err);
        setError("Failed to load cover letters");
      } finally {
        setIsLoading(false);
      }
    }

    loadCoverLetters();
  }, [jobProfileId]);

  const handleCreate = useCallback(async () => {
    try {
      const user = await getCurrentUser();
      if (!user) {
        setError("Not authenticated");
        return;
      }

      const result = await db.cover_letters.create<DBCoverLetter[]>({
        profile_id: user.id,
        job_profile_id: jobProfileId,
        name: "New Cover Letter",
        content: "",
      });

      const created = result[0];
      if (created) {
        setCoverLetters((prev) => [created, ...prev]);
        setSelectedId(created.cover_letter_id);
      }
    } catch (err) {
      console.error("Failed to create cover letter:", err);
      setError("Failed to create cover letter");
    }
  }, [jobProfileId]);

  const handleDelete = useCallback(
    async (id: number, e: React.MouseEvent) => {
      e.stopPropagation();
      if (
        // biome-ignore lint/suspicious/noAlert: Simple confirmation for now
        !confirm("Are you sure you want to delete this cover letter?")
      ) {
        return;
      }

      try {
        await db.cover_letters.remove(id);
        setCoverLetters((prev) =>
          prev.filter((cl) => cl.cover_letter_id !== id)
        );
        if (selectedId === id) {
          setSelectedId(null);
        }
      } catch (err) {
        console.error("Failed to delete cover letter:", err);
        setError("Failed to delete cover letter");
      }
    },
    [selectedId]
  );

  const handleUpdate = useCallback(
    (id: number, updates: Partial<Pick<DBCoverLetter, "name" | "content">>) => {
      // Optimistic update
      setCoverLetters((prev) =>
        prev.map((cl) =>
          cl.cover_letter_id === id ? { ...cl, ...updates } : cl
        )
      );

      // Debounced save
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }

      debounceTimerRef.current = setTimeout(async () => {
        setIsSaving(true);
        try {
          await db.cover_letters.update(id, updates);
        } catch (err) {
          console.error("Failed to update cover letter:", err);
          setError("Failed to save changes");
        } finally {
          setIsSaving(false);
        }
      }, DEBOUNCE_MS);
    },
    []
  );

  // Cleanup debounce timer on unmount
  useEffect(() => {
    const cleanup = () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
    return cleanup;
  }, []);

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500">Loading cover letters...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="rounded-lg bg-red-50 p-6 text-center">
          <p className="text-red-600">{error}</p>
          <button
            className="mt-2 text-red-800 text-sm underline"
            onClick={() => {
              setError(null);
            }}
            type="button"
          >
            Dismiss
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto flex h-full max-w-7xl gap-6">
      {/* Sidebar List */}
      <div className="flex w-1/3 min-w-[300px] max-w-sm flex-col gap-4">
        <div className="flex items-center justify-between px-1">
          <h2 className="font-semibold text-gray-900 text-lg">
            Cover Letters
            {isSaving ? (
              <span className="ml-2 font-normal text-gray-400 text-sm">
                Saving...
              </span>
            ) : null}
          </h2>
          <Button onClick={handleCreate} size="sm" variant="default">
            <PlusIcon className="mr-2 h-4 w-4" />
            New
          </Button>
        </div>

        <div className="flex-1 space-y-3 overflow-y-auto pr-2 pb-4">
          {coverLetters.length === 0 ? (
            <div className="rounded-lg border border-gray-300 border-dashed bg-white p-6 py-8 text-center text-gray-500 text-sm">
              No cover letters yet. <br />
              Create one to get started.
            </div>
          ) : (
            coverLetters.map((letter) => (
              <Card
                className={`group relative cursor-pointer transition-all hover:border-gray-400 ${
                  selectedId === letter.cover_letter_id
                    ? "border-gray-900 shadow-md ring-1 ring-gray-900"
                    : "border-gray-200 shadow-sm"
                }`}
                key={letter.cover_letter_id}
                onClick={() => setSelectedId(letter.cover_letter_id)}
              >
                <div className="p-4">
                  <h3 className="truncate pr-6 font-medium text-gray-900">
                    {letter.name}
                  </h3>
                  <p className="mt-1 text-gray-500 text-xs">
                    Modified{" "}
                    {letter.updated_at
                      ? new Date(letter.updated_at).toLocaleDateString()
                      : "N/A"}
                  </p>

                  <button
                    className="absolute top-3 right-3 rounded-md p-1.5 text-gray-400 opacity-0 transition-colors hover:bg-gray-100 hover:text-red-600 group-hover:opacity-100"
                    onClick={(e) => handleDelete(letter.cover_letter_id, e)}
                    title="Delete cover letter"
                    type="button"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                </div>
              </Card>
            ))
          )}
        </div>
      </div>

      {/* Editor Area */}
      <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        {selectedLetter ? (
          <div className="flex h-full flex-col">
            <div className="border-gray-200 border-b bg-gray-50/50 p-4">
              <div className="max-w-md">
                <label
                  className="mb-1.5 block font-medium text-gray-500 text-xs uppercase tracking-wide"
                  htmlFor="cover-letter-name"
                >
                  Name
                </label>
                <Input
                  className="bg-white font-medium"
                  id="cover-letter-name"
                  onChange={(e) =>
                    handleUpdate(selectedLetter.cover_letter_id, {
                      name: e.target.value,
                    })
                  }
                  placeholder="e.g. Software Engineer at Google"
                  value={selectedLetter.name}
                />
              </div>
            </div>
            <div className="relative flex-1 p-0">
              <textarea
                className="h-full w-full resize-none p-8 font-sans text-base text-gray-800 leading-relaxed focus:outline-none"
                onChange={(e) =>
                  handleUpdate(selectedLetter.cover_letter_id, {
                    content: e.target.value,
                  })
                }
                placeholder="Write your cover letter here..."
                spellCheck={false}
                value={selectedLetter.content}
              />
            </div>
          </div>
        ) : (
          <div className="flex flex-1 flex-col items-center justify-center gap-4 bg-gray-50/30 text-gray-400">
            <div className="rounded-full border border-gray-100 bg-white p-6 shadow-sm">
              <PlusIcon className="h-10 w-10 text-gray-300" />
            </div>
            <div className="text-center">
              <h3 className="mb-1 font-medium text-gray-900">
                No cover letter selected
              </h3>
              <p className="text-sm">
                Select a cover letter from the list or create a new one.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
