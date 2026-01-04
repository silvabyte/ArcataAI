import {
  AcademicCapIcon,
  BriefcaseIcon,
  DocumentTextIcon,
  FolderIcon,
  HeartIcon,
  LanguageIcon,
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
import { LogoIcon } from "../../../../lib/brand/Logo";
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
    <div className="fixed right-8 bottom-8 z-50">
      <div className="relative">
        <button
          aria-expanded={isOpen}
          aria-haspopup="true"
          className="flex size-14 items-center justify-center rounded-full bg-gray-900 text-white shadow-lg transition-transform hover:scale-105 hover:bg-black focus:outline-none focus:ring-2 focus:ring-gray-900 focus:ring-offset-2"
          onClick={() => setIsOpen(!isOpen)}
          type="button"
        >
          <LogoIcon className="size-6" />
          <span className="sr-only">Add Section</span>
        </button>

        {isOpen ? (
          <>
            <button
              aria-label="Close menu"
              className="fixed inset-0 z-10 bg-black/20 backdrop-blur-sm"
              onClick={() => setIsOpen(false)}
              type="button"
            />
            <div className="absolute right-0 bottom-full z-20 mb-4 max-h-[80vh] w-72 origin-bottom-right overflow-y-auto rounded-xl border border-gray-200 bg-white p-2 shadow-xl ring-1 ring-black/5">
              {sectionOptions.map((option) => (
                <button
                  className="group flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-gray-50"
                  key={option.type}
                  onClick={() => insertSection(option.type)}
                  type="button"
                >
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-gray-100 group-hover:bg-white group-hover:shadow-sm">
                    <option.icon className="size-5 text-gray-500 group-hover:text-gray-900" />
                  </div>
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
    </div>
  );
}
