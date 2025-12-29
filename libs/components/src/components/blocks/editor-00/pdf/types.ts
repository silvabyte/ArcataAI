import type { AwardEntry } from "../nodes/AwardsNode";
import type { CertificationEntry } from "../nodes/CertificationsNode";
import type { ContactData } from "../nodes/ContactNode";
import type { CustomSectionData } from "../nodes/CustomSectionNode";
import type { EducationEntry } from "../nodes/EducationNode";
import type { ExperienceEntry } from "../nodes/ExperienceNode";
import type { LanguagesData } from "../nodes/LanguagesNode";
import type { ProjectEntry } from "../nodes/ProjectsNode";
import type { SkillsData } from "../nodes/SkillsNode";
import type { SummaryData } from "../nodes/SummaryNode";
import type { VolunteerEntry } from "../nodes/VolunteerNode";

export type ResumeData = {
  contact?: ContactData;
  summary?: SummaryData;
  experience?: ExperienceEntry[];
  education?: EducationEntry[];
  skills?: SkillsData;
  certifications?: CertificationEntry[];
  projects?: ProjectEntry[];
  awards?: AwardEntry[];
  languages?: LanguagesData;
  volunteer?: VolunteerEntry[];
  customSections?: CustomSectionData[];
};
