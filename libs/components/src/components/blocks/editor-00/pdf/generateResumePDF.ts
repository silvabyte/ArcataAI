import { pdf } from "@react-pdf/renderer";
import type { SerializedEditorState, SerializedLexicalNode } from "lexical";
import type { CustomSectionData } from "../nodes/CustomSectionNode";
import { ResumePDF } from "./ResumePDF";
import type { ResumeData } from "./types";

type SerializedNodeWithData = SerializedLexicalNode & {
  contactData?: ResumeData["contact"];
  summaryData?: ResumeData["summary"];
  entries?: unknown[];
  skillsData?: ResumeData["skills"];
  languagesData?: ResumeData["languages"];
  customData?: CustomSectionData;
};

export function extractResumeData(
  editorState: SerializedEditorState
): ResumeData {
  const data: ResumeData = {};
  const customSections: NonNullable<ResumeData["customSections"]> = [];

  const nodes = editorState.root.children as SerializedNodeWithData[];

  for (const node of nodes) {
    switch (node.type) {
      case "contact-section":
        data.contact = node.contactData;
        break;
      case "summary-section":
        data.summary = node.summaryData;
        break;
      case "experience-section":
        data.experience = node.entries as ResumeData["experience"];
        break;
      case "education-section":
        data.education = node.entries as ResumeData["education"];
        break;
      case "skills-section":
        data.skills = node.skillsData;
        break;
      case "certifications-section":
        data.certifications = node.entries as ResumeData["certifications"];
        break;
      case "projects-section":
        data.projects = node.entries as ResumeData["projects"];
        break;
      case "awards-section":
        data.awards = node.entries as ResumeData["awards"];
        break;
      case "languages-section":
        data.languages = node.languagesData;
        break;
      case "volunteer-section":
        data.volunteer = node.entries as ResumeData["volunteer"];
        break;
      case "custom-section":
        if (node.customData) {
          customSections.push(node.customData);
        }
        break;
      default:
        break;
    }
  }

  if (customSections.length > 0) {
    data.customSections = customSections;
  }

  return data;
}

export async function generateResumePDF(
  editorState: SerializedEditorState
): Promise<Blob> {
  const resumeData = extractResumeData(editorState);
  const doc = ResumePDF({ data: resumeData });
  const blob = await pdf(doc).toBlob();
  return blob;
}

export async function downloadResumePDF(
  editorState: SerializedEditorState,
  filename: string
): Promise<void> {
  const blob = await generateResumePDF(editorState);
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}
