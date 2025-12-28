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

export type AwardEntry = {
  id: string;
  title: string;
  issuer: string;
  date: string;
  description?: string;
};

export type SerializedAwardsNode = Spread<
  {
    entries: AwardEntry[];
  },
  SerializedLexicalNode
>;

function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

function createEmptyEntry(): AwardEntry {
  return {
    id: generateId(),
    title: "",
    issuer: "",
    date: "",
    description: "",
  };
}

function AwardEntryEditor({
  entry,
  onChange,
  onDelete,
}: {
  entry: AwardEntry;
  onChange: (entry: AwardEntry) => void;
  onDelete: () => void;
}) {
  const id = useId();

  const handleChange = (field: keyof AwardEntry, value: unknown) => {
    onChange({ ...entry, [field]: value });
  };

  return (
    <div className="rounded-lg border border-gray-200 p-4">
      <div className="mb-3 flex items-start justify-between">
        <h4 className="font-medium text-gray-900">
          {entry.title || entry.issuer || "New Award"}
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
            Award Title
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-title`}
            onChange={(e) => handleChange("title", e.target.value)}
            placeholder="Best Innovation Award"
            type="text"
            value={entry.title}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-issuer`}
          >
            Issuer
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-issuer`}
            onChange={(e) => handleChange("issuer", e.target.value)}
            placeholder="Acme Corp"
            type="text"
            value={entry.issuer}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-date`}
          >
            Date
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-date`}
            onChange={(e) => handleChange("date", e.target.value)}
            type="month"
            value={entry.date}
          />
        </div>
        <div className="sm:col-span-2">
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-description`}
          >
            Description (optional)
          </label>
          <textarea
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-description`}
            onChange={(e) => handleChange("description", e.target.value)}
            placeholder="Describe the award and its significance..."
            rows={2}
            value={entry.description ?? ""}
          />
        </div>
      </div>
    </div>
  );
}

function formatDate(date: string): string {
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
}

function AwardsNodeComponent({
  entries,
  nodeKey,
}: {
  entries: AwardEntry[];
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<AwardEntry[]>(entries);
  const [isEditing, setIsEditing] = useState(false);

  const updateNode = useCallback(
    (newEntries: AwardEntry[]) => {
      editor.update(() => {
        const node = $getAwardsNodeByKey(nodeKey);
        if (node) {
          node.setEntries(newEntries);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleEntryChange = (index: number, entry: AwardEntry) => {
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
            Awards & Honors
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
            <AwardEntryEditor
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
            Add Award
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
        Awards & Honors
      </h3>
      {data.length > 0 ? (
        <div className="space-y-4">
          {data.map((entry) => (
            <div className="border-gray-100 border-l-2 pl-4" key={entry.id}>
              <div className="flex flex-wrap items-baseline justify-between gap-x-2">
                <h4 className="font-medium text-gray-900">
                  {entry.title || "Untitled Award"}
                </h4>
                <span className="text-gray-500 text-sm">
                  {formatDate(entry.date)}
                </span>
              </div>
              <p className="text-gray-600 text-sm">{entry.issuer}</p>
              {entry.description ? (
                <p className="mt-1 text-gray-500 text-sm">
                  {entry.description}
                </p>
              ) : null}
            </div>
          ))}
        </div>
      ) : (
        <p className="text-center text-gray-500">
          Click to add awards and honors
        </p>
      )}
    </button>
  );
}

function $getAwardsNodeByKey(key: NodeKey): AwardsNode | null {
  const node = $getNodeByKey(key);
  if ($isAwardsNode(node)) {
    return node;
  }
  return null;
}

export class AwardsNode extends DecoratorNode<JSX.Element> {
  __entries: AwardEntry[];

  static getType(): string {
    return "awards-section";
  }

  static clone(node: AwardsNode): AwardsNode {
    return new AwardsNode([...node.__entries], node.__key);
  }

  static importJSON(serializedNode: SerializedAwardsNode): AwardsNode {
    return $createAwardsNode(serializedNode.entries);
  }

  constructor(entries?: AwardEntry[], key?: NodeKey) {
    super(key);
    this.__entries = entries ?? [];
  }

  exportJSON(): SerializedAwardsNode {
    return {
      ...super.exportJSON(),
      entries: this.__entries,
      type: "awards-section",
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

  getEntries(): AwardEntry[] {
    return this.__entries;
  }

  setEntries(entries: AwardEntry[]): void {
    const writable = this.getWritable();
    writable.__entries = entries;
  }

  decorate(): JSX.Element {
    return (
      <AwardsNodeComponent entries={this.__entries} nodeKey={this.__key} />
    );
  }
}

export function $createAwardsNode(entries?: AwardEntry[]): AwardsNode {
  return $applyNodeReplacement(new AwardsNode(entries));
}

export function $isAwardsNode(
  node: LexicalNode | null | undefined
): node is AwardsNode {
  return node instanceof AwardsNode;
}
