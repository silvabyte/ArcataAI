import { PlusIcon, TrashIcon, XMarkIcon } from "@heroicons/react/20/solid";
import { WrenchScrewdriverIcon } from "@heroicons/react/24/outline";
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

export type SkillCategory = {
  id: string;
  name: string;
  skills: string[];
};

export type SkillsData = {
  categories: SkillCategory[];
};

export type SerializedSkillsNode = Spread<
  {
    skillsData: SkillsData;
  },
  SerializedLexicalNode
>;

function SkillsCategoryEditor({
  category,
  categoryIndex,
  id,
  onRemoveCategory,
  onUpdateCategory,
}: {
  category: SkillCategory;
  categoryIndex: number;
  id: string;
  onRemoveCategory: () => void;
  onUpdateCategory: (updated: SkillCategory) => void;
}) {
  const [newSkill, setNewSkill] = useState("");

  const handleNameChange = (name: string) => {
    onUpdateCategory({ ...category, name });
  };

  const handleAddSkill = () => {
    if (newSkill.trim() === "") {
      return;
    }
    onUpdateCategory({
      ...category,
      skills: [...category.skills, newSkill.trim()],
    });
    setNewSkill("");
  };

  const handleRemoveSkill = (skillIndex: number) => {
    onUpdateCategory({
      ...category,
      skills: category.skills.filter((_, i) => i !== skillIndex),
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleAddSkill();
    }
  };

  return (
    <div className="rounded-lg border border-gray-200 p-4">
      <div className="mb-3 flex items-center gap-2">
        <div className="flex-1">
          <label
            className="block font-medium text-gray-700 text-sm"
            htmlFor={`${id}-category-${categoryIndex}-name`}
          >
            Category Name
          </label>
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-category-${categoryIndex}-name`}
            onChange={(e) => handleNameChange(e.target.value)}
            placeholder="e.g., Programming Languages"
            type="text"
            value={category.name}
          />
        </div>
        <button
          className="mt-6 rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-red-500"
          onClick={onRemoveCategory}
          title="Remove category"
          type="button"
        >
          <TrashIcon className="size-5" />
        </button>
      </div>

      <div>
        <span className="block font-medium text-gray-700 text-sm">Skills</span>
        <div className="mt-2 flex flex-wrap gap-2">
          {category.skills.map((skill, skillIndex) => (
            <span
              className="inline-flex items-center gap-1 rounded-full bg-indigo-100 px-3 py-1 font-medium text-indigo-700 text-sm"
              key={`${category.id}-skill-${skillIndex}`}
            >
              {skill}
              <button
                className="rounded-full p-0.5 hover:bg-indigo-200"
                onClick={() => handleRemoveSkill(skillIndex)}
                title="Remove skill"
                type="button"
              >
                <XMarkIcon className="size-3.5" />
              </button>
            </span>
          ))}
        </div>
        <div className="mt-2 flex gap-2">
          <input
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
            id={`${id}-category-${categoryIndex}-new-skill`}
            onChange={(e) => setNewSkill(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Add a skill..."
            type="text"
            value={newSkill}
          />
          <button
            className="rounded-lg bg-gray-100 px-3 py-1.5 text-sm hover:bg-gray-200"
            onClick={handleAddSkill}
            type="button"
          >
            <PlusIcon className="size-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

function SkillsNodeComponent({
  nodeKey,
  skillsData,
}: {
  nodeKey: NodeKey;
  skillsData: SkillsData;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<SkillsData>(skillsData);
  const [isEditing, setIsEditing] = useState(false);
  const id = useId();

  const updateNode = useCallback(
    (newData: SkillsData) => {
      editor.update(() => {
        const node = $getSkillsNodeByKey(nodeKey);
        if (node) {
          node.setSkillsData(newData);
        }
      });
      // Dispatch command to trigger save (DecoratorNode changes aren't detected by update listener)
      editor.dispatchCommand(DECORATOR_NODE_CHANGED_COMMAND, undefined);
    },
    [editor, nodeKey]
  );

  const deleteSection = useCallback(() => {
    editor.update(() => {
      const node = $getSkillsNodeByKey(nodeKey);
      if (node) {
        node.remove();
      }
    });
    editor.dispatchCommand(DECORATOR_NODE_CHANGED_COMMAND, undefined);
  }, [editor, nodeKey]);

  const handleUpdateCategory = (index: number, updated: SkillCategory) => {
    const newCategories = [...data.categories];
    newCategories[index] = updated;
    const newData = { ...data, categories: newCategories };
    setData(newData);
    updateNode(newData);
  };

  const handleRemoveCategory = (index: number) => {
    const newCategories = data.categories.filter((_, i) => i !== index);
    const newData = { ...data, categories: newCategories };
    setData(newData);
    updateNode(newData);
  };

  const handleAddCategory = () => {
    const newCategory: SkillCategory = {
      id: crypto.randomUUID(),
      name: "",
      skills: [],
    };
    const newData = { ...data, categories: [...data.categories, newCategory] };
    setData(newData);
    updateNode(newData);
  };

  if (isEditing) {
    return (
      <div className="border-gray-100 border-t py-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-lg">Skills</h3>
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
          {data.categories.map((category, index) => (
            <SkillsCategoryEditor
              category={category}
              categoryIndex={index}
              id={id}
              key={category.id}
              onRemoveCategory={() => handleRemoveCategory(index)}
              onUpdateCategory={(updated) =>
                handleUpdateCategory(index, updated)
              }
            />
          ))}
          <button
            className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-gray-300 border-dashed py-3 text-gray-500 transition-colors hover:border-gray-400 hover:text-gray-600"
            onClick={handleAddCategory}
            type="button"
          >
            <PlusIcon className="size-5" />
            Add Category
          </button>
        </div>
      </div>
    );
  }

  // Display mode
  const hasContent =
    data?.categories?.some((cat) => cat.name !== "" || cat.skills.length > 0) ??
    false;

  return (
    <button
      className="group w-full cursor-pointer rounded-lg p-4 text-left transition-all hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      <h3 className="mb-6 border-gray-200 border-b pb-2 font-bold text-gray-900 text-sm uppercase tracking-wider">
        Skills
      </h3>
      {hasContent ? (
        <div className="space-y-4">
          {data.categories.map((category) => (
            <div key={category.id}>
              {category.name !== "" && (
                <span className="mb-2 block font-bold text-gray-700 text-sm">
                  {category.name}
                </span>
              )}
              <div className="flex flex-wrap gap-2">
                {category.skills.map((skill, skillIndex) => (
                  <span
                    className="inline-block rounded-md bg-gray-100 px-2.5 py-1 font-medium text-gray-700 text-sm"
                    key={`${category.id}-display-${skillIndex}`}
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="flex w-full items-center justify-center rounded-lg border-2 border-gray-200 border-dashed py-8 text-gray-400 group-hover:border-gray-300 group-hover:bg-gray-50">
          <div className="text-center">
            <WrenchScrewdriverIcon className="mx-auto size-10 text-gray-300" />
            <p className="mt-2 font-medium text-sm">Add Skills</p>
          </div>
        </div>
      )}
    </button>
  );
}

function $getSkillsNodeByKey(key: NodeKey): SkillsNode | null {
  const node = $getNodeByKey(key);
  if ($isSkillsNode(node)) {
    return node;
  }
  return null;
}

export class SkillsNode extends DecoratorNode<JSX.Element> {
  __skillsData: SkillsData;

  static getType(): string {
    return "skills-section";
  }

  static clone(node: SkillsNode): SkillsNode {
    return new SkillsNode(node.__skillsData, node.__key);
  }

  static importJSON(serializedNode: SerializedSkillsNode): SkillsNode {
    return $createSkillsNode(serializedNode.skillsData);
  }

  constructor(skillsData?: SkillsData, key?: NodeKey) {
    super(key);
    this.__skillsData = skillsData ?? {
      categories: [],
    };
  }

  exportJSON(): SerializedSkillsNode {
    return {
      ...super.exportJSON(),
      skillsData: this.__skillsData,
      type: "skills-section",
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

  getSkillsData(): SkillsData {
    return this.__skillsData;
  }

  setSkillsData(skillsData: SkillsData): void {
    const writable = this.getWritable();
    writable.__skillsData = skillsData;
  }

  decorate(): JSX.Element {
    return (
      <SkillsNodeComponent
        nodeKey={this.__key}
        skillsData={this.__skillsData}
      />
    );
  }
}

export function $createSkillsNode(skillsData?: SkillsData): SkillsNode {
  return $applyNodeReplacement(new SkillsNode(skillsData));
}

export function $isSkillsNode(
  node: LexicalNode | null | undefined
): node is SkillsNode {
  return node instanceof SkillsNode;
}
