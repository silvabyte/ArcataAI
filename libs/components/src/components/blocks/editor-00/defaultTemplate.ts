import type { SerializedEditorState, SerializedLexicalNode } from "lexical";
import type { SerializedContactNode } from "./nodes/ContactNode";
import type { SerializedEducationNode } from "./nodes/EducationNode";
import type { SerializedExperienceNode } from "./nodes/ExperienceNode";
import type { SerializedSkillsNode } from "./nodes/SkillsNode";
import type { SerializedSummaryNode } from "./nodes/SummaryNode";

/**
 * Creates a default resume template with empty sections
 * for Contact, Summary, Experience, Education, and Skills
 */
export function createDefaultResumeTemplate(): SerializedEditorState {
  const contactNode: SerializedContactNode = {
    type: "contact-section",
    version: 1,
    contactData: {
      name: "",
      email: "",
      phone: "",
      location: "",
      linkedIn: "",
      portfolio: "",
      github: "",
    },
  };

  const summaryNode: SerializedSummaryNode = {
    type: "summary-section",
    version: 1,
    summaryData: {
      headline: "",
      summary: "",
    },
  };

  const experienceNode: SerializedExperienceNode = {
    type: "experience-section",
    version: 1,
    entries: [],
  };

  const educationNode: SerializedEducationNode = {
    type: "education-section",
    version: 1,
    entries: [],
  };

  const skillsNode: SerializedSkillsNode = {
    type: "skills-section",
    version: 1,
    skillsData: {
      categories: [],
    },
  };

  return {
    root: {
      children: [
        contactNode as SerializedLexicalNode,
        summaryNode as SerializedLexicalNode,
        experienceNode as SerializedLexicalNode,
        educationNode as SerializedLexicalNode,
        skillsNode as SerializedLexicalNode,
      ],
      direction: "ltr",
      format: "",
      indent: 0,
      type: "root",
      version: 1,
    },
  };
}
