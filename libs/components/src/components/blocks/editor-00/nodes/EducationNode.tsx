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

export type EducationEntry = {
  id: string;
  institution: string;
  degree: string;
  field: string;
  location: string;
  startDate: string;
  endDate: string;
  current: boolean;
  gpa?: string;
  honors?: string;
  coursework: string[];
};

export type SerializedEducationNode = Spread<
  {
    entries: EducationEntry[];
  },
  SerializedLexicalNode
>;

function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

function createEmptyEntry(): EducationEntry {
  return {
    id: generateId(),
    institution: "",
    degree: "",
    field: "",
    location: "",
    startDate: "",
    endDate: "",
    current: false,
    gpa: undefined,
    honors: undefined,
    coursework: [],
  };
}

function EducationEntryEditor({
  entry,
  onChange,
  onDelete,
}: {
  entry: EducationEntry;
  onChange: (entry: EducationEntry) => void;
  onDelete: () => void;
}) {
  const id = useId();
  const [courseworkInput, setCourseworkInput] = useState("");

  const handleChange = (field: keyof EducationEntry, value: unknown) => {
    onChange({ ...entry, [field]: value });
  };

  const addCoursework = () => {
    if (courseworkInput.trim()) {
      onChange({
        ...entry,
        coursework: [...entry.coursework, courseworkInput.trim()],
      });
      setCourseworkInput("");
    }
  };

  const removeCoursework = (index: number) => {
    onChange({
      ...entry,
      coursework: entry.coursework.filter((_, i) => i !== index),
    });
  };

  return (
    <div className="rounded-lg border border-gray-200 p-4">
      <div className="mb-3 flex items-start justify-between">
        <h4 className="font-medium text-gray-900">
          {entry.degree || entry.institution || "New Education"}
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
            htmlFor={`${id}-institution`}
          >
            Institution
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-institution`}
            onChange={(e) => handleChange("institution", e.target.value)}
            placeholder="University of California"
            type="text"
            value={entry.institution}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-degree`}
          >
            Degree
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-degree`}
            onChange={(e) => handleChange("degree", e.target.value)}
            placeholder="Bachelor of Science"
            type="text"
            value={entry.degree}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-field`}
          >
            Field of Study
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-field`}
            onChange={(e) => handleChange("field", e.target.value)}
            placeholder="Computer Science"
            type="text"
            value={entry.field}
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
            placeholder="Berkeley, CA"
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
            I am currently studying here
          </label>
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-gpa`}
          >
            GPA (Optional)
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-gpa`}
            onChange={(e) => handleChange("gpa", e.target.value || undefined)}
            placeholder="3.8"
            type="text"
            value={entry.gpa ?? ""}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-honors`}
          >
            Honors (Optional)
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-honors`}
            onChange={(e) =>
              handleChange("honors", e.target.value || undefined)
            }
            placeholder="Magna Cum Laude"
            type="text"
            value={entry.honors ?? ""}
          />
        </div>
        <div className="sm:col-span-2">
          <span className="block font-medium text-gray-700 text-xs">
            Coursework
          </span>
          <div className="mt-1 space-y-2">
            {entry.coursework.map((course, index) => (
              <div className="flex items-center gap-2" key={course}>
                <span className="flex-1 text-gray-700 text-sm">{course}</span>
                <button
                  className="text-red-500 hover:text-red-700"
                  onClick={() => removeCoursework(index)}
                  type="button"
                >
                  <TrashIcon className="size-4" />
                </button>
              </div>
            ))}
            <div className="flex gap-2">
              <input
                className="mt-1 block w-full flex-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
                onChange={(e) => setCourseworkInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addCoursework();
                  }
                }}
                placeholder="Add a course..."
                type="text"
                value={courseworkInput}
              />
              <button
                className="rounded-lg bg-gray-100 px-3 py-1.5 text-sm hover:bg-gray-200"
                onClick={addCoursework}
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

function EducationDetails({ entry }: { entry: EducationEntry }) {
  const hasGpaOrHonors = Boolean(entry.gpa || entry.honors);
  const hasCoursework = entry.coursework.length > 0;

  if (!(hasGpaOrHonors || hasCoursework)) {
    return null;
  }

  return (
    <>
      {hasGpaOrHonors ? (
        <p className="mt-1 text-gray-600 text-sm">
          {entry.gpa ? `GPA: ${entry.gpa}` : ""}
          {entry.gpa && entry.honors ? " | " : ""}
          {entry.honors ?? ""}
        </p>
      ) : null}
      {hasCoursework ? (
        <p className="mt-1 text-gray-600 text-sm">
          <span className="font-medium">Relevant Coursework:</span>{" "}
          {entry.coursework.join(", ")}
        </p>
      ) : null}
    </>
  );
}

function EducationNodeComponent({
  entries,
  nodeKey,
}: {
  entries: EducationEntry[];
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<EducationEntry[]>(entries);
  const [isEditing, setIsEditing] = useState(false);

  const updateNode = useCallback(
    (newEntries: EducationEntry[]) => {
      editor.update(() => {
        const node = $getEducationNodeByKey(nodeKey);
        if (node) {
          node.setEntries(newEntries);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleEntryChange = (index: number, entry: EducationEntry) => {
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
          <h3 className="font-semibold text-gray-900 text-lg">Education</h3>
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
            <EducationEntryEditor
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
            Add Education
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
      <h3 className="mb-4 font-semibold text-gray-900 text-lg">Education</h3>
      {data.length > 0 ? (
        <div className="space-y-4">
          {data.map((entry) => (
            <div className="border-gray-100 border-l-2 pl-4" key={entry.id}>
              <div className="flex flex-wrap items-baseline justify-between gap-x-2">
                <h4 className="font-medium text-gray-900">
                  {entry.degree || "Untitled Degree"}
                  {entry.field ? ` in ${entry.field}` : ""}
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
                {entry.institution}
                {entry.location ? ` - ${entry.location}` : ""}
              </p>
              <EducationDetails entry={entry} />
            </div>
          ))}
        </div>
      ) : (
        <p className="text-center text-gray-500">Click to add education</p>
      )}
    </button>
  );
}

function $getEducationNodeByKey(key: NodeKey): EducationNode | null {
  const node = $getNodeByKey(key);
  if ($isEducationNode(node)) {
    return node;
  }
  return null;
}

export class EducationNode extends DecoratorNode<JSX.Element> {
  __entries: EducationEntry[];

  static getType(): string {
    return "education-section";
  }

  static clone(node: EducationNode): EducationNode {
    return new EducationNode([...node.__entries], node.__key);
  }

  static importJSON(serializedNode: SerializedEducationNode): EducationNode {
    return $createEducationNode(serializedNode.entries);
  }

  constructor(entries?: EducationEntry[], key?: NodeKey) {
    super(key);
    this.__entries = entries ?? [];
  }

  exportJSON(): SerializedEducationNode {
    return {
      ...super.exportJSON(),
      entries: this.__entries,
      type: "education-section",
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

  getEntries(): EducationEntry[] {
    return this.__entries;
  }

  setEntries(entries: EducationEntry[]): void {
    const writable = this.getWritable();
    writable.__entries = entries;
  }

  decorate(): JSX.Element {
    return (
      <EducationNodeComponent entries={this.__entries} nodeKey={this.__key} />
    );
  }
}

export function $createEducationNode(
  entries?: EducationEntry[]
): EducationNode {
  return $applyNodeReplacement(new EducationNode(entries));
}

export function $isEducationNode(
  node: LexicalNode | null | undefined
): node is EducationNode {
  return node instanceof EducationNode;
}
