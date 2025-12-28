import { PlusIcon, TrashIcon } from "@heroicons/react/20/solid";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import {
  $applyNodeReplacement,
  $getNodeByKey,
  DecoratorNode,
  type EditorConfig,
  type LexicalNode,
  type NodeKey,
  type SerializedLexicalNode,
  type Spread,
} from "lexical";
import { useCallback, useId, useState } from "react";

export type ExperienceEntry = {
  id: string;
  company: string;
  title: string;
  location: string;
  startDate: string;
  endDate: string;
  current: boolean;
  highlights: string[];
};

export type SerializedExperienceNode = Spread<
  {
    entries: ExperienceEntry[];
  },
  SerializedLexicalNode
>;

function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

function createEmptyEntry(): ExperienceEntry {
  return {
    id: generateId(),
    company: "",
    title: "",
    location: "",
    startDate: "",
    endDate: "",
    current: false,
    highlights: [],
  };
}

function ExperienceEntryEditor({
  entry,
  onChange,
  onDelete,
}: {
  entry: ExperienceEntry;
  onChange: (entry: ExperienceEntry) => void;
  onDelete: () => void;
}) {
  const id = useId();
  const [highlightInput, setHighlightInput] = useState("");

  const handleChange = (field: keyof ExperienceEntry, value: unknown) => {
    onChange({ ...entry, [field]: value });
  };

  const addHighlight = () => {
    if (highlightInput.trim()) {
      onChange({
        ...entry,
        highlights: [...entry.highlights, highlightInput.trim()],
      });
      setHighlightInput("");
    }
  };

  const removeHighlight = (index: number) => {
    onChange({
      ...entry,
      highlights: entry.highlights.filter((_, i) => i !== index),
    });
  };

  return (
    <div className="rounded-lg border border-gray-200 p-4">
      <div className="mb-3 flex items-start justify-between">
        <h4 className="font-medium text-gray-900">
          {entry.title || entry.company || "New Experience"}
        </h4>
        <button
          className="text-red-500 hover:text-red-700"
          onClick={onDelete}
          type="button"
        >
          <TrashIcon className="size-5" />
        </button>
      </div>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-title`}
          >
            Job Title
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-title`}
            onChange={(e) => handleChange("title", e.target.value)}
            placeholder="Software Engineer"
            type="text"
            value={entry.title}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-company`}
          >
            Company
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-company`}
            onChange={(e) => handleChange("company", e.target.value)}
            placeholder="Acme Corp"
            type="text"
            value={entry.company}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-location`}
          >
            Location
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-location`}
            onChange={(e) => handleChange("location", e.target.value)}
            placeholder="San Francisco, CA"
            type="text"
            value={entry.location}
          />
        </div>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <label
              className="block font-medium text-gray-700 text-xs"
              htmlFor={`${id}-start`}
            >
              Start Date
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-start`}
              onChange={(e) => handleChange("startDate", e.target.value)}
              type="month"
              value={entry.startDate}
            />
          </div>
          <div className="flex-1">
            <label
              className="block font-medium text-gray-700 text-xs"
              htmlFor={`${id}-end`}
            >
              End Date
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400 disabled:opacity-50"
              disabled={entry.current}
              id={`${id}-end`}
              onChange={(e) => handleChange("endDate", e.target.value)}
              type="month"
              value={entry.endDate}
            />
          </div>
        </div>
        <div className="flex items-center gap-2 sm:col-span-2">
          <input
            checked={entry.current}
            className="size-4 rounded border-gray-300 text-gray-600 focus:ring-gray-400"
            id={`${id}-current`}
            onChange={(e) => handleChange("current", e.target.checked)}
            type="checkbox"
          />
          <label
            className="font-medium text-gray-700 text-sm"
            htmlFor={`${id}-current`}
          >
            I currently work here
          </label>
        </div>
        <div className="sm:col-span-2">
          <span className="block font-medium text-gray-700 text-xs">
            Key Achievements
          </span>
          <div className="mt-1 space-y-2">
            {entry.highlights.map((highlight, index) => (
              <div className="flex items-center gap-2" key={highlight}>
                <span className="flex-1 text-gray-700 text-sm">
                  {highlight}
                </span>
                <button
                  className="text-red-500 hover:text-red-700"
                  onClick={() => removeHighlight(index)}
                  type="button"
                >
                  <TrashIcon className="size-4" />
                </button>
              </div>
            ))}
            <div className="flex gap-2">
              <input
                className="block flex-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
                onChange={(e) => setHighlightInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addHighlight();
                  }
                }}
                placeholder="Add an achievement..."
                type="text"
                value={highlightInput}
              />
              <button
                className="rounded-lg bg-gray-100 px-3 py-1.5 text-sm hover:bg-gray-200"
                onClick={addHighlight}
                type="button"
              >
                Add
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function formatDateRange(
  startDate: string,
  endDate: string,
  current: boolean
): string {
  const formatDate = (date: string) => {
    if (!date) {
      return "";
    }
    const [year, month] = date.split("-");
    const monthNames = [
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec",
    ];
    return `${monthNames[Number.parseInt(month, 10) - 1]} ${year}`;
  };

  const start = formatDate(startDate);
  const end = current ? "Present" : formatDate(endDate);

  if (!(start || end)) {
    return "";
  }
  if (!start) {
    return end;
  }
  if (!end) {
    return start;
  }
  return `${start} - ${end}`;
}

function ExperienceNodeComponent({
  entries,
  nodeKey,
}: {
  entries: ExperienceEntry[];
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<ExperienceEntry[]>(entries);
  const [isEditing, setIsEditing] = useState(false);

  const updateNode = useCallback(
    (newEntries: ExperienceEntry[]) => {
      editor.update(() => {
        const node = $getExperienceNodeByKey(nodeKey);
        if (node) {
          node.setEntries(newEntries);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleEntryChange = (index: number, entry: ExperienceEntry) => {
    const newData = [...data];
    newData[index] = entry;
    setData(newData);
    updateNode(newData);
  };

  const handleAddEntry = () => {
    const newData = [...data, createEmptyEntry()];
    setData(newData);
    updateNode(newData);
  };

  const handleDeleteEntry = (index: number) => {
    const newData = data.filter((_, i) => i !== index);
    setData(newData);
    updateNode(newData);
  };

  if (isEditing) {
    return (
      <div className="border-gray-100 border-t py-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-lg">
            Work Experience
          </h3>
          <button
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 font-medium text-gray-700 text-sm shadow-sm hover:bg-gray-50"
            onClick={() => setIsEditing(false)}
            type="button"
          >
            Done
          </button>
        </div>
        <div className="space-y-4">
          {data.map((entry, index) => (
            <ExperienceEntryEditor
              entry={entry}
              key={entry.id}
              onChange={(e) => handleEntryChange(index, e)}
              onDelete={() => handleDeleteEntry(index)}
            />
          ))}
          <button
            className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-gray-300 border-dashed py-3 text-gray-500 transition-colors hover:border-gray-400 hover:text-gray-600"
            onClick={handleAddEntry}
            type="button"
          >
            <PlusIcon className="size-5" />
            Add Experience
          </button>
        </div>
      </div>
    );
  }

  // Display mode
  return (
    <button
      className="w-full cursor-pointer border-gray-100 border-t py-4 text-left transition-colors hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      <h3 className="mb-4 font-semibold text-gray-900 text-lg">
        Work Experience
      </h3>
      {data.length > 0 ? (
        <div className="space-y-4">
          {data.map((entry) => (
            <div className="border-gray-100 border-l-2 pl-4" key={entry.id}>
              <div className="flex flex-wrap items-baseline justify-between gap-x-2">
                <h4 className="font-medium text-gray-900">
                  {entry.title || "Untitled Position"}
                </h4>
                <span className="text-gray-500 text-sm">
                  {formatDateRange(
                    entry.startDate,
                    entry.endDate,
                    entry.current
                  )}
                </span>
              </div>
              <p className="text-gray-600 text-sm">
                {entry.company}
                {entry.location ? ` - ${entry.location}` : ""}
              </p>
              {entry.highlights.length > 0 && (
                <ul className="mt-2 list-disc space-y-1 pl-4 text-gray-600 text-sm">
                  {entry.highlights.map((highlight) => (
                    <li key={highlight}>{highlight}</li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      ) : (
        <p className="text-center text-gray-500">
          Click to add work experience
        </p>
      )}
    </button>
  );
}

function $getExperienceNodeByKey(key: NodeKey): ExperienceNode | null {
  const node = $getNodeByKey(key);
  if ($isExperienceNode(node)) {
    return node;
  }
  return null;
}

export class ExperienceNode extends DecoratorNode<JSX.Element> {
  __entries: ExperienceEntry[];

  static getType(): string {
    return "experience-section";
  }

  static clone(node: ExperienceNode): ExperienceNode {
    return new ExperienceNode([...node.__entries], node.__key);
  }

  static importJSON(serializedNode: SerializedExperienceNode): ExperienceNode {
    return $createExperienceNode(serializedNode.entries);
  }

  constructor(entries?: ExperienceEntry[], key?: NodeKey) {
    super(key);
    this.__entries = entries ?? [];
  }

  exportJSON(): SerializedExperienceNode {
    return {
      ...super.exportJSON(),
      entries: this.__entries,
      type: "experience-section",
      version: 1,
    };
  }

  createDOM(_config: EditorConfig): HTMLElement {
    const div = document.createElement("div");
    div.className = "resume-section";
    return div;
  }

  updateDOM(): false {
    return false;
  }

  getEntries(): ExperienceEntry[] {
    return this.__entries;
  }

  setEntries(entries: ExperienceEntry[]): void {
    const writable = this.getWritable();
    writable.__entries = entries;
  }

  decorate(): JSX.Element {
    return (
      <ExperienceNodeComponent entries={this.__entries} nodeKey={this.__key} />
    );
  }
}

export function $createExperienceNode(
  entries?: ExperienceEntry[]
): ExperienceNode {
  return $applyNodeReplacement(new ExperienceNode(entries));
}

export function $isExperienceNode(
  node: LexicalNode | null | undefined
): node is ExperienceNode {
  return node instanceof ExperienceNode;
}
