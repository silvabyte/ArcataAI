import { PlusIcon, TrashIcon } from "@heroicons/react/20/solid";
import { StarIcon } from "@heroicons/react/24/outline";
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

/** Regex to strip http:// or https:// protocol prefix from URLs */
const STRIP_PROTOCOL_REGEX = /^https?:\/\//;

export type CertificationEntry = {
  id: string;
  name: string;
  issuer: string;
  issueDate: string;
  expirationDate: string;
  credentialId: string;
  credentialUrl: string;
  noExpiration: boolean;
};

export type SerializedCertificationsNode = Spread<
  {
    entries: CertificationEntry[];
  },
  SerializedLexicalNode
>;

function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

function createEmptyEntry(): CertificationEntry {
  return {
    id: generateId(),
    name: "",
    issuer: "",
    issueDate: "",
    expirationDate: "",
    credentialId: "",
    credentialUrl: "",
    noExpiration: false,
  };
}

function CertificationEntryEditor({
  entry,
  onChange,
  onDelete,
}: {
  entry: CertificationEntry;
  onChange: (entry: CertificationEntry) => void;
  onDelete: () => void;
}) {
  const id = useId();

  const handleChange = (field: keyof CertificationEntry, value: unknown) => {
    onChange({ ...entry, [field]: value });
  };

  return (
    <div className="rounded-lg border border-gray-200 p-4">
      <div className="mb-3 flex items-start justify-between">
        <h4 className="font-medium text-gray-900">
          {entry.name || entry.issuer || "New Certification"}
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
            Certification Name
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-name`}
            onChange={(e) => handleChange("name", e.target.value)}
            placeholder="AWS Solutions Architect"
            type="text"
            value={entry.name}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-issuer`}
          >
            Issuing Organization
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-issuer`}
            onChange={(e) => handleChange("issuer", e.target.value)}
            placeholder="Amazon Web Services"
            type="text"
            value={entry.issuer}
          />
        </div>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <label
              className="block font-medium text-gray-700 text-xs"
              htmlFor={`${id}-issue`}
            >
              Issue Date
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-issue`}
              onChange={(e) => handleChange("issueDate", e.target.value)}
              type="month"
              value={entry.issueDate}
            />
          </div>
          <div className="flex-1">
            <label
              className="block font-medium text-gray-700 text-xs"
              htmlFor={`${id}-expiration`}
            >
              Expiration Date
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400 disabled:opacity-50"
              disabled={entry.noExpiration}
              id={`${id}-expiration`}
              onChange={(e) => handleChange("expirationDate", e.target.value)}
              type="month"
              value={entry.expirationDate}
            />
          </div>
        </div>
        <div className="flex items-center gap-2 sm:col-span-2">
          <input
            checked={entry.noExpiration}
            className="size-4 rounded border-gray-300 text-gray-600 focus:ring-gray-400"
            id={`${id}-no-expiration`}
            onChange={(e) => handleChange("noExpiration", e.target.checked)}
            type="checkbox"
          />
          <label
            className="font-medium text-gray-700 text-sm"
            htmlFor={`${id}-no-expiration`}
          >
            This credential does not expire
          </label>
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-credential-id`}
          >
            Credential ID
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-credential-id`}
            onChange={(e) => handleChange("credentialId", e.target.value)}
            placeholder="ABC123XYZ"
            type="text"
            value={entry.credentialId}
          />
        </div>
        <div>
          <label
            className="block font-medium text-gray-700 text-xs"
            htmlFor={`${id}-credential-url`}
          >
            Credential URL
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-credential-url`}
            onChange={(e) => handleChange("credentialUrl", e.target.value)}
            placeholder="https://credential.example.com/verify/..."
            type="url"
            value={entry.credentialUrl}
          />
        </div>
      </div>
    </div>
  );
}

function formatDateRange(
  issueDate: string,
  expirationDate: string,
  noExpiration: boolean
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

  const start = formatDate(issueDate);
  const end = noExpiration ? "No Expiration" : formatDate(expirationDate);

  if (!(start || end)) {
    return "";
  }
  if (!start) {
    return end;
  }
  if (!end) {
    return `Issued ${start}`;
  }
  return `Issued ${start} - ${end}`;
}

function CertificationsNodeComponent({
  entries,
  nodeKey,
}: {
  entries: CertificationEntry[];
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<CertificationEntry[]>(entries);
  const [isEditing, setIsEditing] = useState(false);

  const updateNode = useCallback(
    (newEntries: CertificationEntry[]) => {
      editor.update(() => {
        const node = $getCertificationsNodeByKey(nodeKey);
        if (node) {
          node.setEntries(newEntries);
        }
      });
      // Dispatch command to trigger save (DecoratorNode changes aren't detected by update listener)
      editor.dispatchCommand(DECORATOR_NODE_CHANGED_COMMAND, undefined);
    },
    [editor, nodeKey]
  );

  const handleEntryChange = (index: number, entry: CertificationEntry) => {
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
            Certifications
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
            <CertificationEntryEditor
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
            Add Certification
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
        Certifications
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
                    {entry.name || "Untitled Certification"}
                  </h4>
                  <div className="font-medium text-gray-700">
                    {entry.issuer}
                    {entry.credentialId ? ` • ID: ${entry.credentialId}` : ""}
                  </div>
                </div>
                <div className="shrink-0 font-medium text-gray-500 text-sm tabular-nums">
                  {formatDateRange(
                    entry.issueDate,
                    entry.expirationDate,
                    entry.noExpiration
                  )}
                </div>
              </div>

              {entry.credentialUrl ? (
                <a
                  className="mt-1 block truncate text-blue-600 text-sm hover:underline"
                  href={entry.credentialUrl}
                  onClick={(e) => e.stopPropagation()}
                  rel="noopener noreferrer"
                  target="_blank"
                >
                  {entry.credentialUrl.replace(STRIP_PROTOCOL_REGEX, "")}
                </a>
              ) : null}
            </div>
          ))}
        </div>
      ) : (
        <div className="flex w-full items-center justify-center rounded-lg border-2 border-gray-200 border-dashed py-8 text-gray-400 group-hover:border-gray-300 group-hover:bg-gray-50">
          <div className="text-center">
            <StarIcon className="mx-auto size-10 text-gray-300" />
            <p className="mt-2 font-medium text-sm">Add Certifications</p>
          </div>
        </div>
      )}
    </button>
  );
}

function $getCertificationsNodeByKey(key: NodeKey): CertificationsNode | null {
  const node = $getNodeByKey(key);
  if ($isCertificationsNode(node)) {
    return node;
  }
  return null;
}

export class CertificationsNode extends DecoratorNode<JSX.Element> {
  __entries: CertificationEntry[];

  static getType(): string {
    return "certifications-section";
  }

  static clone(node: CertificationsNode): CertificationsNode {
    return new CertificationsNode([...node.__entries], node.__key);
  }

  static importJSON(
    serializedNode: SerializedCertificationsNode
  ): CertificationsNode {
    return $createCertificationsNode(serializedNode.entries);
  }

  constructor(entries?: CertificationEntry[], key?: NodeKey) {
    super(key);
    this.__entries = entries ?? [];
  }

  exportJSON(): SerializedCertificationsNode {
    return {
      ...super.exportJSON(),
      entries: this.__entries,
      type: "certifications-section",
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

  getEntries(): CertificationEntry[] {
    return this.__entries;
  }

  setEntries(entries: CertificationEntry[]): void {
    const writable = this.getWritable();
    writable.__entries = entries;
  }

  decorate(): JSX.Element {
    return (
      <CertificationsNodeComponent
        entries={this.__entries}
        nodeKey={this.__key}
      />
    );
  }
}

export function $createCertificationsNode(
  entries?: CertificationEntry[]
): CertificationsNode {
  return $applyNodeReplacement(new CertificationsNode(entries));
}

export function $isCertificationsNode(
  node: LexicalNode | null | undefined
): node is CertificationsNode {
  return node instanceof CertificationsNode;
}
