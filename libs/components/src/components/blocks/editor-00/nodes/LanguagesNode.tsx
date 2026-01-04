import { PlusIcon, TrashIcon } from "@heroicons/react/20/solid";
import { LanguageIcon } from "@heroicons/react/24/outline";
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

export type LanguageProficiency =
  | "native"
  | "fluent"
  | "advanced"
  | "intermediate"
  | "beginner";

export type LanguageEntry = {
  id: string;
  language: string;
  proficiency: LanguageProficiency;
};

export type LanguagesData = {
  entries: LanguageEntry[];
};

export type SerializedLanguagesNode = Spread<
  {
    languagesData: LanguagesData;
  },
  SerializedLexicalNode
>;

const PROFICIENCY_LABELS: Record<LanguageProficiency, string> = {
  native: "Native",
  fluent: "Fluent",
  advanced: "Advanced",
  intermediate: "Intermediate",
  beginner: "Beginner",
};

const PROFICIENCY_COLORS: Record<LanguageProficiency, string> = {
  native: "bg-green-100 text-green-700",
  fluent: "bg-blue-100 text-blue-700",
  advanced: "bg-indigo-100 text-indigo-700",
  intermediate: "bg-yellow-100 text-yellow-700",
  beginner: "bg-gray-100 text-gray-700",
};

function LanguageEntryEditor({
  entry,
  entryIndex,
  id,
  onRemoveEntry,
  onUpdateEntry,
}: {
  entry: LanguageEntry;
  entryIndex: number;
  id: string;
  onRemoveEntry: () => void;
  onUpdateEntry: (updated: LanguageEntry) => void;
}) {
  const handleLanguageChange = (language: string) => {
    onUpdateEntry({ ...entry, language });
  };

  const handleProficiencyChange = (proficiency: LanguageProficiency) => {
    onUpdateEntry({ ...entry, proficiency });
  };

  return (
    <div className="flex items-center gap-3 rounded-lg border border-gray-200 p-4">
      <div className="flex-1">
        <label
          className="block font-medium text-gray-700 text-sm"
          htmlFor={`${id}-entry-${entryIndex}-language`}
        >
          Language
        </label>
        <input
          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
          id={`${id}-entry-${entryIndex}-language`}
          onChange={(e) => handleLanguageChange(e.target.value)}
          placeholder="e.g., English"
          type="text"
          value={entry.language}
        />
      </div>
      <div className="flex-1">
        <label
          className="block font-medium text-gray-700 text-sm"
          htmlFor={`${id}-entry-${entryIndex}-proficiency`}
        >
          Proficiency
        </label>
        <select
          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
          id={`${id}-entry-${entryIndex}-proficiency`}
          onChange={(e) =>
            handleProficiencyChange(e.target.value as LanguageProficiency)
          }
          value={entry.proficiency}
        >
          <option value="native">Native</option>
          <option value="fluent">Fluent</option>
          <option value="advanced">Advanced</option>
          <option value="intermediate">Intermediate</option>
          <option value="beginner">Beginner</option>
        </select>
      </div>
      <button
        className="mt-6 rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-red-500"
        onClick={onRemoveEntry}
        title="Remove language"
        type="button"
      >
        <TrashIcon className="size-5" />
      </button>
    </div>
  );
}

function LanguagesNodeComponent({
  languagesData,
  nodeKey,
}: {
  languagesData: LanguagesData;
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<LanguagesData>(languagesData);
  const [isEditing, setIsEditing] = useState(false);
  const id = useId();

  const updateNode = useCallback(
    (newData: LanguagesData) => {
      editor.update(() => {
        const node = $getLanguagesNodeByKey(nodeKey);
        if (node) {
          node.setLanguagesData(newData);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleUpdateEntry = (index: number, updated: LanguageEntry) => {
    const newEntries = [...data.entries];
    newEntries[index] = updated;
    const newData = { ...data, entries: newEntries };
    setData(newData);
    updateNode(newData);
  };

  const handleRemoveEntry = (index: number) => {
    const newEntries = data.entries.filter((_, i) => i !== index);
    const newData = { ...data, entries: newEntries };
    setData(newData);
    updateNode(newData);
  };

  const handleAddEntry = () => {
    const newEntry: LanguageEntry = {
      id: crypto.randomUUID(),
      language: "",
      proficiency: "intermediate",
    };
    const newData = { ...data, entries: [...data.entries, newEntry] };
    setData(newData);
    updateNode(newData);
  };

  if (isEditing) {
    return (
      <div className="border-gray-100 border-t py-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-lg">Languages</h3>
          <button
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 font-medium text-gray-700 text-sm shadow-sm hover:bg-gray-50"
            onClick={() => setIsEditing(false)}
            type="button"
          >
            Done
          </button>
        </div>
        <div className="space-y-4">
          {data.entries.map((entry, index) => (
            <LanguageEntryEditor
              entry={entry}
              entryIndex={index}
              id={id}
              key={entry.id}
              onRemoveEntry={() => handleRemoveEntry(index)}
              onUpdateEntry={(updated) => handleUpdateEntry(index, updated)}
            />
          ))}
          <button
            className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-gray-300 border-dashed py-3 text-gray-500 transition-colors hover:border-gray-400 hover:text-gray-600"
            onClick={handleAddEntry}
            type="button"
          >
            <PlusIcon className="size-5" />
            Add Language
          </button>
        </div>
      </div>
    );
  }

  // Display mode
  const hasContent = data.entries.some((entry) => entry.language !== "");

  return (
    <button
      className="group w-full cursor-pointer rounded-lg p-4 text-left transition-all hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      <h3 className="mb-6 border-gray-200 border-b pb-2 font-bold text-gray-900 text-sm uppercase tracking-wider">
        Languages
      </h3>
      {hasContent ? (
        <div className="flex flex-wrap gap-2">
          {data.entries.map((entry) => (
            <span
              className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm ${PROFICIENCY_COLORS[entry.proficiency]}`}
              key={entry.id}
            >
              <span className="font-medium">{entry.language}</span>
              <span className="opacity-75">
                ({PROFICIENCY_LABELS[entry.proficiency]})
              </span>
            </span>
          ))}
        </div>
      ) : (
        <div className="flex w-full items-center justify-center rounded-lg border-2 border-gray-200 border-dashed py-8 text-gray-400 group-hover:border-gray-300 group-hover:bg-gray-50">
          <div className="text-center">
            <LanguageIcon className="mx-auto size-10 text-gray-300" />
            <p className="mt-2 font-medium text-sm">Add Languages</p>
          </div>
        </div>
      )}
    </button>
  );
}

function $getLanguagesNodeByKey(key: NodeKey): LanguagesNode | null {
  const node = $getNodeByKey(key);
  if ($isLanguagesNode(node)) {
    return node;
  }
  return null;
}

export class LanguagesNode extends DecoratorNode<JSX.Element> {
  __languagesData: LanguagesData;

  static getType(): string {
    return "languages-section";
  }

  static clone(node: LanguagesNode): LanguagesNode {
    return new LanguagesNode(node.__languagesData, node.__key);
  }

  static importJSON(serializedNode: SerializedLanguagesNode): LanguagesNode {
    return $createLanguagesNode(serializedNode.languagesData);
  }

  constructor(languagesData?: LanguagesData, key?: NodeKey) {
    super(key);
    this.__languagesData = languagesData ?? {
      entries: [],
    };
  }

  exportJSON(): SerializedLanguagesNode {
    return {
      ...super.exportJSON(),
      languagesData: this.__languagesData,
      type: "languages-section",
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

  getLanguagesData(): LanguagesData {
    return this.__languagesData;
  }

  setLanguagesData(languagesData: LanguagesData): void {
    const writable = this.getWritable();
    writable.__languagesData = languagesData;
  }

  decorate(): JSX.Element {
    return (
      <LanguagesNodeComponent
        languagesData={this.__languagesData}
        nodeKey={this.__key}
      />
    );
  }
}

export function $createLanguagesNode(
  languagesData?: LanguagesData
): LanguagesNode {
  return $applyNodeReplacement(new LanguagesNode(languagesData));
}

export function $isLanguagesNode(
  node: LexicalNode | null | undefined
): node is LanguagesNode {
  return node instanceof LanguagesNode;
}
