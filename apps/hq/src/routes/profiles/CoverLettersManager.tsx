import { Button, Card, Input } from "@arcata/components";
import { PlusIcon, TrashIcon } from "@heroicons/react/24/outline";
import { useState } from "react";

// Mock Data Type
export type CoverLetter = {
  id: string;
  name: string;
  content: string;
  lastModified: string; // ISO date string
};

const INITIAL_COVER_LETTERS: CoverLetter[] = [
  {
    id: "1",
    name: "Software Engineer @ Google",
    content:
      "Dear Hiring Manager,\n\nI am writing to express my interest in the Software Engineer position at Google. With my background in distributed systems and React, I believe I would be a great fit for your team.\n\nSincerely,\n[Name]",
    lastModified: new Date().toISOString(),
  },
  {
    id: "2",
    name: "Frontend Developer @ Airbnb",
    content:
      "To whom it may concern,\n\nI've always admired Airbnb's design system and commitment to high-quality user experiences. I have 5 years of experience building scalable frontend applications.\n\nBest,\n[Name]",
    lastModified: new Date(Date.now() - 86_400_000).toISOString(), // 1 day ago
  },
];

export function CoverLettersManager() {
  const [coverLetters, setCoverLetters] = useState<CoverLetter[]>(
    INITIAL_COVER_LETTERS
  );
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const selectedLetter = coverLetters.find((cl) => cl.id === selectedId);

  const handleCreate = () => {
    const newLetter: CoverLetter = {
      id: crypto.randomUUID(),
      name: "New Cover Letter",
      content: "",
      lastModified: new Date().toISOString(),
    };
    setCoverLetters([newLetter, ...coverLetters]);
    setSelectedId(newLetter.id);
  };

  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (
      // biome-ignore lint/suspicious/noAlert: Simple confirmation for now
      confirm("Are you sure you want to delete this cover letter?")
    ) {
      setCoverLetters(coverLetters.filter((cl) => cl.id !== id));
      if (selectedId === id) {
        setSelectedId(null);
      }
    }
  };

  const handleUpdate = (id: string, updates: Partial<CoverLetter>) => {
    setCoverLetters(
      coverLetters.map((cl) =>
        cl.id === id
          ? { ...cl, ...updates, lastModified: new Date().toISOString() }
          : cl
      )
    );
  };

  return (
    <div className="mx-auto flex h-full max-w-7xl gap-6">
      {/* Sidebar List */}
      <div className="flex w-1/3 min-w-[300px] max-w-sm flex-col gap-4">
        <div className="flex items-center justify-between px-1">
          <h2 className="font-semibold text-gray-900 text-lg">Cover Letters</h2>
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
                  selectedId === letter.id
                    ? "border-gray-900 shadow-md ring-1 ring-gray-900"
                    : "border-gray-200 shadow-sm"
                }`}
                key={letter.id}
                onClick={() => setSelectedId(letter.id)}
              >
                <div className="p-4">
                  <h3 className="truncate pr-6 font-medium text-gray-900">
                    {letter.name}
                  </h3>
                  <p className="mt-1 text-gray-500 text-xs">
                    Modified{" "}
                    {new Date(letter.lastModified).toLocaleDateString()}
                  </p>

                  <button
                    className="absolute top-3 right-3 rounded-md p-1.5 text-gray-400 opacity-0 transition-colors hover:bg-gray-100 hover:text-red-600 group-hover:opacity-100"
                    onClick={(e) => handleDelete(letter.id, e)}
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
                    handleUpdate(selectedLetter.id, { name: e.target.value })
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
                  handleUpdate(selectedLetter.id, { content: e.target.value })
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
