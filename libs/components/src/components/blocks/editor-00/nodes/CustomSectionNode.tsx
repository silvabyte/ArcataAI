import { SparklesIcon } from "@heroicons/react/24/outline";
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

export type CustomSectionData = {
  content: string;
  title: string;
};

export type SerializedCustomSectionNode = Spread<
  {
    customData: CustomSectionData;
  },
  SerializedLexicalNode
>;

function CustomSectionNodeComponent({
  customData,
  nodeKey,
}: {
  customData: CustomSectionData;
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<CustomSectionData>(customData);
  const [isEditing, setIsEditing] = useState(false);
  const id = useId();

  const updateNode = useCallback(
    (newData: CustomSectionData) => {
      editor.update(() => {
        const node = $getCustomSectionNodeByKey(nodeKey);
        if (node) {
          node.setCustomData(newData);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleChange = (field: keyof CustomSectionData, value: string) => {
    const newData = { ...data, [field]: value };
    setData(newData);
    updateNode(newData);
  };

  if (isEditing) {
    return (
      <div className="border-gray-100 border-t py-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-lg">
            Custom Section
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
              htmlFor={`${id}-title`}
            >
              Section Title
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-title`}
              onChange={(e) => handleChange("title", e.target.value)}
              placeholder="Projects, Awards, Publications..."
              type="text"
              value={data.title}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-content`}
            >
              Content
            </label>
            <textarea
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-content`}
              onChange={(e) => handleChange("content", e.target.value)}
              placeholder="Enter your content here..."
              rows={4}
              value={data.content}
            />
          </div>
        </div>
      </div>
    );
  }

  // Display mode
  const hasContent = Boolean(data.title || data.content);

  return (
    <button
      className="group w-full cursor-pointer rounded-lg p-4 text-left transition-all hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      {hasContent ? (
        <div>
          {data.title !== "" && (
            <h3 className="mb-6 border-gray-200 border-b pb-2 font-bold text-gray-900 text-sm uppercase tracking-wider">
              {data.title}
            </h3>
          )}
          {data.content !== "" && (
            <p className="mt-2 whitespace-pre-wrap text-gray-600">
              {data.content}
            </p>
          )}
        </div>
      ) : (
        <div className="flex w-full items-center justify-center rounded-lg border-2 border-gray-200 border-dashed py-8 text-gray-400 group-hover:border-gray-300 group-hover:bg-gray-50">
          <div className="text-center">
            <SparklesIcon className="mx-auto size-10 text-gray-300" />
            <p className="mt-2 font-medium text-sm">Add Custom Section</p>
          </div>
        </div>
      )}
    </button>
  );
}

function $getCustomSectionNodeByKey(key: NodeKey): CustomSectionNode | null {
  const node = $getNodeByKey(key);
  if ($isCustomSectionNode(node)) {
    return node;
  }
  return null;
}

export class CustomSectionNode extends DecoratorNode<JSX.Element> {
  __customData: CustomSectionData;

  static getType(): string {
    return "custom-section";
  }

  static clone(node: CustomSectionNode): CustomSectionNode {
    return new CustomSectionNode(node.__customData, node.__key);
  }

  static importJSON(
    serializedNode: SerializedCustomSectionNode
  ): CustomSectionNode {
    return $createCustomSectionNode(serializedNode.customData);
  }

  constructor(customData?: CustomSectionData, key?: NodeKey) {
    super(key);
    this.__customData = customData ?? {
      content: "",
      title: "",
    };
  }

  exportJSON(): SerializedCustomSectionNode {
    return {
      ...super.exportJSON(),
      customData: this.__customData,
      type: "custom-section",
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

  getCustomData(): CustomSectionData {
    return this.__customData;
  }

  setCustomData(customData: CustomSectionData): void {
    const writable = this.getWritable();
    writable.__customData = customData;
  }

  decorate(): JSX.Element {
    return (
      <CustomSectionNodeComponent
        customData={this.__customData}
        nodeKey={this.__key}
      />
    );
  }
}

export function $createCustomSectionNode(
  customData?: CustomSectionData
): CustomSectionNode {
  return $applyNodeReplacement(new CustomSectionNode(customData));
}

export function $isCustomSectionNode(
  node: LexicalNode | null | undefined
): node is CustomSectionNode {
  return node instanceof CustomSectionNode;
}
