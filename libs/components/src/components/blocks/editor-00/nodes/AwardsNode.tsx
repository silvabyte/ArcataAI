import { PlusIcon, TrashIcon } from "@heroicons/react/20/solid";
import { TrophyIcon } from "@heroicons/react/24/outline";
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
      // Dispatch command to trigger save (DecoratorNode changes aren't detected by update listener)
      editor.dispatchCommand(DECORATOR_NODE_CHANGED_COMMAND, undefined);
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
      className="group w-full cursor-pointer rounded-lg p-4 text-left transition-all hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      <h3 className="mb-6 border-gray-200 border-b pb-2 font-bold text-gray-900 text-sm uppercase tracking-wider">
        Awards & Honors
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
                    {entry.title || "Untitled Award"}
                  </h4>
                  <div className="font-medium text-gray-700">
                    {entry.issuer}
                  </div>
                </div>
                <div className="shrink-0 font-medium text-gray-500 text-sm tabular-nums">
                  {formatDate(entry.date)}
                </div>
              </div>

              {entry.description && (
                <p className="mt-2 text-gray-600 text-sm leading-relaxed">
                  {entry.description}
                </p>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="flex w-full items-center justify-center rounded-lg border-2 border-gray-200 border-dashed py-8 text-gray-400 group-hover:border-gray-300 group-hover:bg-gray-50">
          <div className="text-center">
            <TrophyIcon className="mx-auto size-10 text-gray-300" />
            <p className="mt-2 font-medium text-sm">Add Awards & Honors</p>
          </div>
        </div>
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
