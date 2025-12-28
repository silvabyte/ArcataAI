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

export type SummaryData = {
  headline: string;
  summary: string;
};

export type SerializedSummaryNode = Spread<
  {
    summaryData: SummaryData;
  },
  SerializedLexicalNode
>;

function SummaryNodeComponent({
  nodeKey,
  summaryData,
}: {
  nodeKey: NodeKey;
  summaryData: SummaryData;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<SummaryData>(summaryData);
  const [isEditing, setIsEditing] = useState(false);
  const id = useId();

  const updateNode = useCallback(
    (newData: SummaryData) => {
      editor.update(() => {
        const node = $getSummaryNodeByKey(nodeKey);
        if (node) {
          node.setSummaryData(newData);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleChange = (field: keyof SummaryData, value: string) => {
    const newData = { ...data, [field]: value };
    setData(newData);
    updateNode(newData);
  };

  if (isEditing) {
    return (
      <div className="border-gray-100 border-t py-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-lg">
            Professional Summary
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
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-headline`}
            >
              Headline
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-headline`}
              onChange={(e) => handleChange("headline", e.target.value)}
              placeholder="Senior Software Engineer"
              type="text"
              value={data.headline}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-summary`}
            >
              Summary
            </label>
            <textarea
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-summary`}
              onChange={(e) => handleChange("summary", e.target.value)}
              placeholder="Experienced software engineer with expertise in..."
              rows={4}
              value={data.summary}
            />
          </div>
        </div>
      </div>
    );
  }

  // Display mode
  const hasContent = Boolean(data.headline || data.summary);

  return (
    <button
      className="w-full cursor-pointer border-gray-100 border-t py-4 text-left transition-colors hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      {hasContent ? (
        <div>
          {data.headline !== "" && (
            <h3 className="font-semibold text-gray-900 text-lg">
              {data.headline}
            </h3>
          )}
          {data.summary !== "" && (
            <p className="mt-2 whitespace-pre-wrap text-gray-600">
              {data.summary}
            </p>
          )}
        </div>
      ) : (
        <div className="text-center text-gray-500">
          <p>Click to add professional summary</p>
        </div>
      )}
    </button>
  );
}

function $getSummaryNodeByKey(key: NodeKey): SummaryNode | null {
  const node = $getNodeByKey(key);
  if ($isSummaryNode(node)) {
    return node;
  }
  return null;
}

export class SummaryNode extends DecoratorNode<JSX.Element> {
  __summaryData: SummaryData;

  static getType(): string {
    return "summary-section";
  }

  static clone(node: SummaryNode): SummaryNode {
    return new SummaryNode(node.__summaryData, node.__key);
  }

  static importJSON(serializedNode: SerializedSummaryNode): SummaryNode {
    return $createSummaryNode(serializedNode.summaryData);
  }

  constructor(summaryData?: SummaryData, key?: NodeKey) {
    super(key);
    this.__summaryData = summaryData ?? {
      headline: "",
      summary: "",
    };
  }

  exportJSON(): SerializedSummaryNode {
    return {
      ...super.exportJSON(),
      summaryData: this.__summaryData,
      type: "summary-section",
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

  getSummaryData(): SummaryData {
    return this.__summaryData;
  }

  setSummaryData(summaryData: SummaryData): void {
    const writable = this.getWritable();
    writable.__summaryData = summaryData;
  }

  decorate(): JSX.Element {
    return (
      <SummaryNodeComponent
        nodeKey={this.__key}
        summaryData={this.__summaryData}
      />
    );
  }
}

export function $createSummaryNode(summaryData?: SummaryData): SummaryNode {
  return $applyNodeReplacement(new SummaryNode(summaryData));
}

export function $isSummaryNode(
  node: LexicalNode | null | undefined
): node is SummaryNode {
  return node instanceof SummaryNode;
}
