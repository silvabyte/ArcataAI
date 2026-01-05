import {
  createDefaultResumeTemplate,
  type ResumeData,
} from "@arcata/components";
import type { SerializedEditorState, SerializedLexicalNode } from "lexical";

/**
 * Converts parsed ResumeData into a Lexical SerializedEditorState.
 * It starts with the default template and populates it with the parsed data.
 */
export function convertToEditorState(data: ResumeData): SerializedEditorState {
  const template = createDefaultResumeTemplate();

  // We need to clone to avoid mutating the original template if it's cached/shared (though likely not)
  const newState = JSON.parse(JSON.stringify(template));
  const children = newState.root.children;

  // Helper to find node by type
  const findNode = (type: string) =>
    children.find((c: SerializedLexicalNode) => c.type === type);

  // Update Contact
  if (data.contact) {
    const node = findNode("contact-section");
    if (node) {
      node.contactData = data.contact;
    }
  }

  // Update Summary
  if (data.summary) {
    const node = findNode("summary-section");
    if (node) {
      node.summaryData = data.summary;
    }
  }

  // Update Experience
  if (data.experience) {
    const node = findNode("experience-section");
    if (node) {
      node.entries = data.experience;
    }
  }

  // Update Education
  if (data.education) {
    const node = findNode("education-section");
    if (node) {
      node.entries = data.education;
    }
  }

  // Update Skills
  if (data.skills) {
    const node = findNode("skills-section");
    if (node) {
      node.skillsData = data.skills;
    }
  }

  // Note: Other sections (Certifications, Projects, etc.) are not part of the
  // default template yet. If we want to support them, we'd need to append
  // new nodes to the children array.

  return newState;
}
