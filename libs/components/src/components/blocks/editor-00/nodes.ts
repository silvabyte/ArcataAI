import { HeadingNode, QuoteNode } from "@lexical/rich-text";
import type { Klass, LexicalNode, LexicalNodeReplacement } from "lexical";
import { AwardsNode } from "./nodes/AwardsNode";
import { CertificationsNode } from "./nodes/CertificationsNode";
import { ContactNode } from "./nodes/ContactNode";
import { CustomSectionNode } from "./nodes/CustomSectionNode";
import { EducationNode } from "./nodes/EducationNode";
import { ExperienceNode } from "./nodes/ExperienceNode";
import { LanguagesNode } from "./nodes/LanguagesNode";
import { ProjectsNode } from "./nodes/ProjectsNode";
import { SkillsNode } from "./nodes/SkillsNode";
import { SummaryNode } from "./nodes/SummaryNode";
import { VolunteerNode } from "./nodes/VolunteerNode";

export const nodes: ReadonlyArray<Klass<LexicalNode> | LexicalNodeReplacement> =
  [
    HeadingNode,
    QuoteNode,
    ContactNode,
    SummaryNode,
    ExperienceNode,
    EducationNode,
    SkillsNode,
    CertificationsNode,
    ProjectsNode,
    AwardsNode,
    LanguagesNode,
    VolunteerNode,
    CustomSectionNode,
  ];
