import { PlusIcon, TrashIcon } from "@heroicons/react/20/solid";
import { HeartIcon } from "@heroicons/react/24/outline";
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
import { DECORATOR_NODE_CHANGED_COMMAND } from "../commands";

export type VolunteerEntry = {
  id: string;
  organization: string;
  role: string;
  location: string;
  startDate: string;
  endDate: string;
  current: boolean;
  highlights: string[];
};

export type SerializedVolunteerNode = Spread<
  {
    entries: VolunteerEntry[];
  },
  SerializedLexicalNode
>;

function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

function createEmptyEntry(): VolunteerEntry {
  return {
    id: generateId(),
    organization: "",
    role: "",
    location: "",
    startDate: "",
    endDate: "",
    current: false,
    highlights: [],
  };
}

function VolunteerEntryEditor({
  entry,
  onChange,
  onDelete,
}: {
  entry: VolunteerEntry;
  onChange: (entry: VolunteerEntry) => void;
  onDelete: () => void;
}) {
  const id = useId();
  const [highlightInput, setHighlightInput] = useState("");

  const handleChange = (field: keyof VolunteerEntry, value: unknown) => {
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
          {entry.role || entry.organization || "New Volunteer Experience"}
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
            htmlFor={`${id}-role`}
          >
            Role
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-role`}
            onChange={(e) => handleChange("role", e.target.value)}
            placeholder="Volunteer Coordinator"
            type="text"
            value={entry.role}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-organization`}
          >
            Organization
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-organization`}
            onChange={(e) => handleChange("organization", e.target.value)}
            placeholder="Local Food Bank"
            type="text"
            value={entry.organization}
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
            I currently volunteer here
          </label>
        </div>
        <div className="sm:col-span-2">
          <span className="block font-medium text-gray-700 text-xs">
            Key Contributions
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
                placeholder="Add a contribution..."
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

function VolunteerNodeComponent({
  entries,
  nodeKey,
}: {
  entries: VolunteerEntry[];
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<VolunteerEntry[]>(entries);
  const [isEditing, setIsEditing] = useState(false);

  const updateNode = useCallback(
    (newEntries: VolunteerEntry[]) => {
      editor.update(() => {
        const node = $getVolunteerNodeByKey(nodeKey);
        if (node) {
          node.setEntries(newEntries);
        }
      });
      // Dispatch command to trigger save (DecoratorNode changes aren't detected by update listener)
      editor.dispatchCommand(DECORATOR_NODE_CHANGED_COMMAND, undefined);
    },
    [editor, nodeKey]
  );

  const deleteSection = useCallback(() => {
    editor.update(() => {
      const node = $getVolunteerNodeByKey(nodeKey);
      if (node) {
        node.remove();
      }
    });
    editor.dispatchCommand(DECORATOR_NODE_CHANGED_COMMAND, undefined);
  }, [editor, nodeKey]);

  const handleEntryChange = (index: number, entry: VolunteerEntry) => {
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
            Volunteer Experience
          </h3>
          <div className="flex items-center gap-2">
            <button
              className="rounded-lg border border-red-200 bg-white px-3 py-1.5 font-medium text-red-600 text-sm shadow-sm hover:bg-red-50"
              onClick={deleteSection}
              title="Delete section"
              type="button"
            >
              <TrashIcon className="size-4" />
            </button>
            <button
              className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 font-medium text-gray-700 text-sm shadow-sm hover:bg-gray-50"
              onClick={() => setIsEditing(false)}
              type="button"
            >
              Done
            </button>
          </div>
        </div>
        <div className="space-y-4">
          {data.map((entry, index) => (
            <VolunteerEntryEditor
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
            Add Volunteer Experience
          </button>
        </div>
      </div>
    );
  }

  // Display mode
  return (
    <button
      className="group w-full cursor-pointer rounded-lg p-4 text-left transition-all hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      <h3 className="mb-6 border-gray-200 border-b pb-2 font-bold text-gray-900 text-sm uppercase tracking-wider">
        Volunteer Experience
      </h3>
      {data.length > 0 ? (
        <div className="space-y-6">
          {data.map((entry) => (
            <div className="relative pl-4" key={entry.id}>
              {/* Timeline line */}
              <div className="absolute top-0 bottom-0 left-0 w-px bg-gray-200 group-hover:bg-gray-300" />

              <div className="flex flex-col gap-1 sm:flex-row sm:justify-between sm:gap-4">
                <div>
                  <h4 className="font-bold text-base text-gray-900">
                    {entry.role || "Untitled Role"}
                  </h4>
                  <div className="font-medium text-gray-700">
                    {entry.organization}
                    {entry.location ? ` • ${entry.location}` : ""}
                  </div>
                </div>
                <div className="shrink-0 font-medium text-gray-500 text-sm tabular-nums">
                  {formatDateRange(
                    entry.startDate,
                    entry.endDate,
                    entry.current
                  )}
                </div>
              </div>

              {entry.highlights.length > 0 && (
                <ul className="mt-3 list-disc space-y-1.5 pl-4 text-gray-600 text-sm leading-relaxed">
                  {entry.highlights.map((highlight) => (
                    <li key={highlight}>{highlight}</li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="flex w-full items-center justify-center rounded-lg border-2 border-gray-200 border-dashed py-8 text-gray-400 group-hover:border-gray-300 group-hover:bg-gray-50">
          <div className="text-center">
            <HeartIcon className="mx-auto size-10 text-gray-300" />
            <p className="mt-2 font-medium text-sm">Add Volunteer Experience</p>
          </div>
        </div>
      )}
    </button>
  );
}

function $getVolunteerNodeByKey(key: NodeKey): VolunteerNode | null {
  const node = $getNodeByKey(key);
  if ($isVolunteerNode(node)) {
    return node;
  }
  return null;
}

export class VolunteerNode extends DecoratorNode<JSX.Element> {
  __entries: VolunteerEntry[];

  static getType(): string {
    return "volunteer-section";
  }

  static clone(node: VolunteerNode): VolunteerNode {
    return new VolunteerNode([...node.__entries], node.__key);
  }

  static importJSON(serializedNode: SerializedVolunteerNode): VolunteerNode {
    return $createVolunteerNode(serializedNode.entries);
  }

  constructor(entries?: VolunteerEntry[], key?: NodeKey) {
    super(key);
    this.__entries = entries ?? [];
  }

  exportJSON(): SerializedVolunteerNode {
    return {
      ...super.exportJSON(),
      entries: this.__entries,
      type: "volunteer-section",
      version: 1,
    };
  }

  createDOM(_config: EditorConfig): HTMLElement {
    const div = document.createElement("div");
    div.className = "resume-section";
    // Set explicit line-height so Lexical's DraggableBlockPlugin
    // can properly center the drag handle on the first line
    div.style.lineHeight = "32px";
    return div;
  }

  updateDOM(): false {
    return false;
  }

  isInline(): boolean {
    return false;
  }

  getEntries(): VolunteerEntry[] {
    return this.__entries;
  }

  setEntries(entries: VolunteerEntry[]): void {
    const writable = this.getWritable();
    writable.__entries = entries;
  }

  decorate(): JSX.Element {
    return (
      <VolunteerNodeComponent entries={this.__entries} nodeKey={this.__key} />
    );
  }
}

export function $createVolunteerNode(
  entries?: VolunteerEntry[]
): VolunteerNode {
  return $applyNodeReplacement(new VolunteerNode(entries));
}

export function $isVolunteerNode(
  node: LexicalNode | null | undefined
): node is VolunteerNode {
  return node instanceof VolunteerNode;
}
