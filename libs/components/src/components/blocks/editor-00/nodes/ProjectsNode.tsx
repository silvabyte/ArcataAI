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

export type ProjectEntry = {
  id: string;
  name: string;
  description: string;
  url?: string;
  startDate?: string;
  endDate?: string;
  current: boolean;
  technologies: string[];
  highlights: string[];
};

export type SerializedProjectsNode = Spread<
  {
    entries: ProjectEntry[];
  },
  SerializedLexicalNode
>;

function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

function createEmptyEntry(): ProjectEntry {
  return {
    id: generateId(),
    name: "",
    description: "",
    url: "",
    startDate: "",
    endDate: "",
    current: false,
    technologies: [],
    highlights: [],
  };
}

function ProjectEntryEditor({
  entry,
  onChange,
  onDelete,
}: {
  entry: ProjectEntry;
  onChange: (entry: ProjectEntry) => void;
  onDelete: () => void;
}) {
  const id = useId();
  const [technologyInput, setTechnologyInput] = useState("");
  const [highlightInput, setHighlightInput] = useState("");

  const handleChange = (field: keyof ProjectEntry, value: unknown) => {
    onChange({ ...entry, [field]: value });
  };

  const addTechnology = () => {
    if (technologyInput.trim()) {
      onChange({
        ...entry,
        technologies: [...entry.technologies, technologyInput.trim()],
      });
      setTechnologyInput("");
    }
  };

  const removeTechnology = (index: number) => {
    onChange({
      ...entry,
      technologies: entry.technologies.filter((_, i) => i !== index),
    });
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
          {entry.name || "New Project"}
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
            htmlFor={`${id}-name`}
          >
            Project Name
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-name`}
            onChange={(e) => handleChange("name", e.target.value)}
            placeholder="My Awesome Project"
            type="text"
            value={entry.name}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-url`}
          >
            URL (optional)
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-url`}
            onChange={(e) => handleChange("url", e.target.value)}
            placeholder="https://github.com/user/project"
            type="url"
            value={entry.url ?? ""}
          />
        </div>
        <div className="sm:col-span-2">
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-description`}
          >
            Description
          </label>
          <textarea
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-description`}
            onChange={(e) => handleChange("description", e.target.value)}
            placeholder="A brief description of the project..."
            rows={2}
            value={entry.description}
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
              value={entry.startDate ?? ""}
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
              value={entry.endDate ?? ""}
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
            This is an ongoing project
          </label>
        </div>
        <div className="sm:col-span-2">
          <span className="block font-medium text-gray-700 text-xs">
            Technologies
          </span>
          <div className="mt-1 space-y-2">
            <div className="flex flex-wrap gap-2">
              {entry.technologies.map((tech, index) => (
                <span
                  className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-gray-700 text-xs"
                  key={tech}
                >
                  {tech}
                  <button
                    className="text-gray-500 hover:text-gray-700"
                    onClick={() => removeTechnology(index)}
                    type="button"
                  >
                    <TrashIcon className="size-3" />
                  </button>
                </span>
              ))}
            </div>
            <div className="flex gap-2">
              <input
                className="mt-1 block w-full flex-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
                onChange={(e) => setTechnologyInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addTechnology();
                  }
                }}
                placeholder="Add a technology..."
                type="text"
                value={technologyInput}
              />
              <button
                className="rounded-lg bg-gray-100 px-3 py-1.5 text-sm hover:bg-gray-200"
                onClick={addTechnology}
                type="button"
              >
                Add
              </button>
            </div>
          </div>
        </div>
        <div className="sm:col-span-2">
          <span className="block font-medium text-gray-700 text-xs">
            Highlights
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
                className="mt-1 block w-full flex-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
                onChange={(e) => setHighlightInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addHighlight();
                  }
                }}
                placeholder="Add a highlight..."
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
  startDate: string | undefined,
  endDate: string | undefined,
  current: boolean
): string {
  const formatDate = (date: string | undefined) => {
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

function ProjectsNodeComponent({
  entries,
  nodeKey,
}: {
  entries: ProjectEntry[];
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<ProjectEntry[]>(entries);
  const [isEditing, setIsEditing] = useState(false);

  const updateNode = useCallback(
    (newEntries: ProjectEntry[]) => {
      editor.update(() => {
        const node = $getProjectsNodeByKey(nodeKey);
        if (node) {
          node.setEntries(newEntries);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleEntryChange = (index: number, entry: ProjectEntry) => {
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
          <h3 className="font-semibold text-gray-900 text-lg">Projects</h3>
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
            <ProjectEntryEditor
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
            Add Project
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
      <h3 className="mb-4 font-semibold text-gray-900 text-lg">Projects</h3>
      {data.length > 0 ? (
        <div className="space-y-4">
          {data.map((entry) => (
            <div className="border-gray-100 border-l-2 pl-4" key={entry.id}>
              <div className="flex flex-wrap items-baseline justify-between gap-x-2">
                <h4 className="font-medium text-gray-900">
                  {entry.name || "Untitled Project"}
                </h4>
                <span className="text-gray-500 text-sm">
                  {formatDateRange(
                    entry.startDate,
                    entry.endDate,
                    entry.current
                  )}
                </span>
              </div>
              {entry.description ? (
                <p className="text-gray-600 text-sm">{entry.description}</p>
              ) : null}
              {entry.url ? (
                <p className="text-gray-600 text-sm">{entry.url}</p>
              ) : null}
              {entry.technologies.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1">
                  {entry.technologies.map((tech) => (
                    <span
                      className="rounded-full bg-gray-100 px-2 py-0.5 text-gray-600 text-xs"
                      key={tech}
                    >
                      {tech}
                    </span>
                  ))}
                </div>
              )}
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
        <p className="text-center text-gray-500">Click to add projects</p>
      )}
    </button>
  );
}

function $getProjectsNodeByKey(key: NodeKey): ProjectsNode | null {
  const node = $getNodeByKey(key);
  if ($isProjectsNode(node)) {
    return node;
  }
  return null;
}

export class ProjectsNode extends DecoratorNode<JSX.Element> {
  __entries: ProjectEntry[];

  static getType(): string {
    return "projects-section";
  }

  static clone(node: ProjectsNode): ProjectsNode {
    return new ProjectsNode([...node.__entries], node.__key);
  }

  static importJSON(serializedNode: SerializedProjectsNode): ProjectsNode {
    return $createProjectsNode(serializedNode.entries);
  }

  constructor(entries?: ProjectEntry[], key?: NodeKey) {
    super(key);
    this.__entries = entries ?? [];
  }

  exportJSON(): SerializedProjectsNode {
    return {
      ...super.exportJSON(),
      entries: this.__entries,
      type: "projects-section",
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

  getEntries(): ProjectEntry[] {
    return this.__entries;
  }

  setEntries(entries: ProjectEntry[]): void {
    const writable = this.getWritable();
    writable.__entries = entries;
  }

  decorate(): JSX.Element {
    return (
      <ProjectsNodeComponent entries={this.__entries} nodeKey={this.__key} />
    );
  }
}

export function $createProjectsNode(entries?: ProjectEntry[]): ProjectsNode {
  return $applyNodeReplacement(new ProjectsNode(entries));
}

export function $isProjectsNode(
  node: LexicalNode | null | undefined
): node is ProjectsNode {
  return node instanceof ProjectsNode;
}
