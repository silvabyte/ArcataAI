import {
  EnvelopeIcon,
  GlobeAltIcon,
  MapPinIcon,
  PhoneIcon,
} from "@heroicons/react/20/solid";
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

export type ContactData = {
  name: string;
  email: string;
  phone: string;
  location: string;
  linkedIn: string;
  portfolio: string;
  github: string;
};

export type SerializedContactNode = Spread<
  {
    contactData: ContactData;
  },
  SerializedLexicalNode
>;

function ContactNodeComponent({
  contactData,
  nodeKey,
}: {
  contactData: ContactData;
  nodeKey: NodeKey;
}) {
  const [editor] = useLexicalComposerContext();
  const [data, setData] = useState<ContactData>(contactData);
  const [isEditing, setIsEditing] = useState(false);
  const id = useId();

  const updateNode = useCallback(
    (newData: ContactData) => {
      editor.update(() => {
        const node = $getContactNodeByKey(nodeKey);
        if (node) {
          node.setContactData(newData);
        }
      });
    },
    [editor, nodeKey]
  );

  const handleChange = (field: keyof ContactData, value: string) => {
    const newData = { ...data, [field]: value };
    setData(newData);
    updateNode(newData);
  };

  if (isEditing) {
    return (
      <div className="py-4">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-lg">
            Contact Information
          </h3>
          <button
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 font-medium text-gray-700 text-sm shadow-sm hover:bg-gray-50"
            onClick={() => setIsEditing(false)}
            type="button"
          >
            Done
          </button>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-name`}
            >
              Full Name
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-name`}
              onChange={(e) => handleChange("name", e.target.value)}
              placeholder="John Doe"
              type="text"
              value={data.name}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-email`}
            >
              Email
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-email`}
              onChange={(e) => handleChange("email", e.target.value)}
              placeholder="john@example.com"
              type="email"
              value={data.email}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-phone`}
            >
              Phone
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-phone`}
              onChange={(e) => handleChange("phone", e.target.value)}
              placeholder="+1 (555) 123-4567"
              type="tel"
              value={data.phone}
            />
          </div>
          <div className="sm:col-span-2">
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-location`}
            >
              Location
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-location`}
              onChange={(e) => handleChange("location", e.target.value)}
              placeholder="San Francisco, CA"
              type="text"
              value={data.location}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-linkedin`}
            >
              LinkedIn
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-linkedin`}
              onChange={(e) => handleChange("linkedIn", e.target.value)}
              placeholder="linkedin.com/in/johndoe"
              type="url"
              value={data.linkedIn}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-portfolio`}
            >
              Portfolio
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-portfolio`}
              onChange={(e) => handleChange("portfolio", e.target.value)}
              placeholder="johndoe.com"
              type="url"
              value={data.portfolio}
            />
          </div>
          <div>
            <label
              className="block font-medium text-gray-700 text-sm"
              htmlFor={`${id}-github`}
            >
              GitHub
            </label>
            <input
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-gray-400 focus:ring-1 focus:ring-gray-400"
              id={`${id}-github`}
              onChange={(e) => handleChange("github", e.target.value)}
              placeholder="github.com/johndoe"
              type="url"
              value={data.github}
            />
          </div>
        </div>
      </div>
    );
  }

  // Display mode
  const hasContent = Boolean(
    data.name || data.email || data.phone || data.location
  );
  const hasLinks = Boolean(data.linkedIn || data.portfolio || data.github);
  const contactItems = [
    data.email !== "" && (
      <span className="flex items-center gap-x-1" key="email">
        <EnvelopeIcon className="size-4" />
        {data.email}
      </span>
    ),
    data.phone !== "" && (
      <span className="flex items-center gap-x-1" key="phone">
        <PhoneIcon className="size-4" />
        {data.phone}
      </span>
    ),
    data.location !== "" && (
      <span className="flex items-center gap-x-1" key="location">
        <MapPinIcon className="size-4" />
        {data.location}
      </span>
    ),
  ].filter(Boolean);

  const linkItems = [
    data.linkedIn !== "" && (
      <span className="flex items-center gap-x-1" key="linkedin">
        <GlobeAltIcon className="size-4" />
        {data.linkedIn}
      </span>
    ),
    data.portfolio !== "" && (
      <span className="flex items-center gap-x-1" key="portfolio">
        <GlobeAltIcon className="size-4" />
        {data.portfolio}
      </span>
    ),
    data.github !== "" && (
      <span className="flex items-center gap-x-1" key="github">
        <GlobeAltIcon className="size-4" />
        {data.github}
      </span>
    ),
  ].filter(Boolean);

  return (
    <button
      className="w-full cursor-pointer py-2 text-left transition-colors hover:bg-gray-50"
      onClick={() => setIsEditing(true)}
      type="button"
    >
      {hasContent ? (
        <div>
          {data.name !== "" && (
            <h2 className="font-bold text-2xl text-gray-900">{data.name}</h2>
          )}
          {contactItems.length > 0 && (
            <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-gray-600 text-sm">
              {contactItems}
            </div>
          )}
          {hasLinks && linkItems.length > 0 && (
            <div className="mt-1 flex flex-wrap items-center gap-x-4 text-gray-500 text-sm">
              {linkItems}
            </div>
          )}
        </div>
      ) : (
        <div className="text-gray-500">
          <p>Click to add contact information</p>
        </div>
      )}
    </button>
  );
}

function $getContactNodeByKey(key: NodeKey): ContactNode | null {
  const node = $getNodeByKey(key);
  if ($isContactNode(node)) {
    return node;
  }
  return null;
}

export class ContactNode extends DecoratorNode<JSX.Element> {
  __contactData: ContactData;

  static getType(): string {
    return "contact-section";
  }

  static clone(node: ContactNode): ContactNode {
    return new ContactNode(node.__contactData, node.__key);
  }

  static importJSON(serializedNode: SerializedContactNode): ContactNode {
    return $createContactNode(serializedNode.contactData);
  }

  constructor(contactData?: ContactData, key?: NodeKey) {
    super(key);
    this.__contactData = contactData ?? {
      name: "",
      email: "",
      phone: "",
      location: "",
      linkedIn: "",
      portfolio: "",
      github: "",
    };
  }

  exportJSON(): SerializedContactNode {
    return {
      ...super.exportJSON(),
      contactData: this.__contactData,
      type: "contact-section",
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

  getContactData(): ContactData {
    return this.__contactData;
  }

  setContactData(contactData: ContactData): void {
    const writable = this.getWritable();
    writable.__contactData = contactData;
  }

  decorate(): JSX.Element {
    return (
      <ContactNodeComponent
        contactData={this.__contactData}
        nodeKey={this.__key}
      />
    );
  }
}

export function $createContactNode(contactData?: ContactData): ContactNode {
  return $applyNodeReplacement(new ContactNode(contactData));
}

export function $isContactNode(
  node: LexicalNode | null | undefined
): node is ContactNode {
  return node instanceof ContactNode;
}
