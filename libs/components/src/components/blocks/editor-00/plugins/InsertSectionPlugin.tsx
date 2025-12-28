import {
  AcademicCapIcon,
  BriefcaseIcon,
  DocumentTextIcon,
  FolderIcon,
  HeartIcon,
  LanguageIcon,
  PlusIcon,
  SparklesIcon,
  StarIcon,
  TrophyIcon,
  UserIcon,
  WrenchScrewdriverIcon,
} from "@heroicons/react/20/solid";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import type { LexicalNode } from "lexical";
import { $insertNodes } from "lexical";
import { useCallback, useState } from "react";
import { $createAwardsNode } from "../nodes/AwardsNode";
import { $createCertificationsNode } from "../nodes/CertificationsNode";
import { $createContactNode } from "../nodes/ContactNode";
import { $createCustomSectionNode } from "../nodes/CustomSectionNode";
import { $createEducationNode } from "../nodes/EducationNode";
import { $createExperienceNode } from "../nodes/ExperienceNode";
import { $createLanguagesNode } from "../nodes/LanguagesNode";
import { $createProjectsNode } from "../nodes/ProjectsNode";
import { $createSkillsNode } from "../nodes/SkillsNode";
import { $createSummaryNode } from "../nodes/SummaryNode";
import { $createVolunteerNode } from "../nodes/VolunteerNode";

type SectionType =
  | "contact"
  | "summary"
  | "experience"
  | "education"
  | "skills"
  | "certifications"
  | "projects"
  | "awards"
  | "languages"
  | "volunteer"
  | "custom";

type SectionOption = {
  type: SectionType;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
};

const sectionOptions: SectionOption[] = [
  {
    type: "contact",
    label: "Contact Info",
    description: "Name, email, phone, links",
    icon: UserIcon,
  },
  {
    type: "summary",
    label: "Summary",
    description: "Professional headline and summary",
    icon: DocumentTextIcon,
  },
  {
    type: "experience",
    label: "Work Experience",
    description: "Job history and achievements",
    icon: BriefcaseIcon,
  },
  {
    type: "education",
    label: "Education",
    description: "Degrees and academic history",
    icon: AcademicCapIcon,
  },
  {
    type: "skills",
    label: "Skills",
    description: "Technical and soft skills",
    icon: WrenchScrewdriverIcon,
  },
  {
    type: "certifications",
    label: "Certifications",
    description: "Professional certifications",
    icon: StarIcon,
  },
  {
    type: "projects",
    label: "Projects",
    description: "Personal or professional projects",
    icon: FolderIcon,
  },
  {
    type: "awards",
    label: "Awards & Honors",
    description: "Recognition and achievements",
    icon: TrophyIcon,
  },
  {
    type: "languages",
    label: "Languages",
    description: "Language proficiencies",
    icon: LanguageIcon,
  },
  {
    type: "volunteer",
    label: "Volunteer Work",
    description: "Community involvement",
    icon: HeartIcon,
  },
  {
    type: "custom",
    label: "Custom Section",
    description: "Add your own section",
    icon: SparklesIcon,
  },
];

const sectionNodeFactories: Record<SectionType, () => LexicalNode> = {
  contact: $createContactNode,
  summary: $createSummaryNode,
  experience: $createExperienceNode,
  education: $createEducationNode,
  skills: $createSkillsNode,
  certifications: $createCertificationsNode,
  projects: $createProjectsNode,
  awards: $createAwardsNode,
  languages: $createLanguagesNode,
  volunteer: $createVolunteerNode,
  custom: $createCustomSectionNode,
};

export function InsertSectionPlugin() {
  const [editor] = useLexicalComposerContext();
  const [isOpen, setIsOpen] = useState(false);

  const insertSection = useCallback(
    (type: SectionType) => {
      editor.update(() => {
        const node: LexicalNode = sectionNodeFactories[type]();
        $insertNodes([node]);
      });
      setIsOpen(false);
    },
    [editor]
  );

  return (
    <div className="relative">
      <button
        aria-expanded={isOpen}
        aria-haspopup="true"
        className="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2 font-medium text-gray-700 text-sm shadow-sm hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-gray-400 focus-visible:outline-offset-2"
        onClick={() => setIsOpen(!isOpen)}
        type="button"
      >
        <PlusIcon className="size-5" />
        Add Section
      </button>

      {isOpen ? (
        <>
          <button
            aria-label="Close menu"
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
            type="button"
          />
          <div className="absolute left-0 z-20 mt-2 w-72 origin-top-left rounded-xl border border-gray-200 bg-white p-2 shadow-lg">
            {sectionOptions.map((option) => (
              <button
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-gray-100"
                key={option.type}
                onClick={() => insertSection(option.type)}
                type="button"
              >
                <option.icon className="size-5 text-gray-400" />
                <div>
                  <div className="font-medium text-gray-900 text-sm">
                    {option.label}
                  </div>
                  <div className="text-gray-500 text-xs">
                    {option.description}
                  </div>
                </div>
              </button>
            ))}
          </div>
        </>
      ) : null}
    </div>
  );
}
