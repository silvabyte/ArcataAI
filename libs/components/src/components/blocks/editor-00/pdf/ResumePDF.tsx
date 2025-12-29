import { Document, Page } from "@react-pdf/renderer";
import {
  AwardsPDF,
  CertificationsPDF,
  ContactPDF,
  CustomSectionPDF,
  EducationPDF,
  ExperiencePDF,
  LanguagesPDF,
  ProjectsPDF,
  SkillsPDF,
  SummaryPDF,
  VolunteerPDF,
} from "./sections";
import { styles } from "./styles";
import type { ResumeData } from "./types";

type ResumePDFProps = {
  data: ResumeData;
};

export function ResumePDF({ data }: ResumePDFProps) {
  return (
    <Document>
      <Page size="LETTER" style={styles.page}>
        {data.contact ? <ContactPDF data={data.contact} /> : null}
        {data.summary ? <SummaryPDF data={data.summary} /> : null}
        {data.experience ? <ExperiencePDF entries={data.experience} /> : null}
        {data.education ? <EducationPDF entries={data.education} /> : null}
        {data.skills ? <SkillsPDF data={data.skills} /> : null}
        {data.certifications ? (
          <CertificationsPDF entries={data.certifications} />
        ) : null}
        {data.projects ? <ProjectsPDF entries={data.projects} /> : null}
        {data.awards ? <AwardsPDF entries={data.awards} /> : null}
        {data.languages ? <LanguagesPDF data={data.languages} /> : null}
        {data.volunteer ? <VolunteerPDF entries={data.volunteer} /> : null}
        {data.customSections?.map((section, i) => (
          <CustomSectionPDF data={section} key={`custom-${i}`} />
        ))}
      </Page>
    </Document>
  );
}
