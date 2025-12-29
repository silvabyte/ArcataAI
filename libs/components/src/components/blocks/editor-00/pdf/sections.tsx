import { Text, View } from "@react-pdf/renderer";
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
import { styles } from "./styles";

function formatDateRange(
  startDate: string | undefined,
  endDate: string | undefined,
  current: boolean
): string {
  if (!startDate) {
    return "";
  }
  const end = current ? "Present" : endDate || "";
  return `${startDate}${end ? ` - ${end}` : ""}`;
}

function ContactInfoRow({ data }: { data: ContactData }) {
  const items = [data.email, data.phone, data.location].filter(Boolean);
  if (items.length === 0) {
    return null;
  }
  return (
    <View style={styles.contactInfo}>
      {items.map((item, i) => (
        <Text key={`contact-${i}`}>{item}</Text>
      ))}
    </View>
  );
}

function ContactLinksRow({ data }: { data: ContactData }) {
  const links = [data.linkedIn, data.portfolio, data.github].filter(Boolean);
  if (links.length === 0) {
    return null;
  }
  return (
    <View style={styles.contactLinks}>
      {links.map((link, i) => (
        <Text key={`link-${i}`}>{link}</Text>
      ))}
    </View>
  );
}

export function ContactPDF({ data }: { data: ContactData }) {
  const hasContactInfo = Boolean(data.email || data.phone || data.location);
  const hasLinks = Boolean(data.linkedIn || data.portfolio || data.github);
  const hasAnyContent = Boolean(data.name) || hasContactInfo || hasLinks;

  if (!hasAnyContent) {
    return null;
  }

  return (
    <View style={styles.contactSection}>
      {data.name ? <Text style={styles.name}>{data.name}</Text> : null}
      <ContactInfoRow data={data} />
      <ContactLinksRow data={data} />
    </View>
  );
}

export function SummaryPDF({ data }: { data: SummaryData }) {
  const hasContent = Boolean(data.headline) || Boolean(data.summary);
  if (!hasContent) {
    return null;
  }

  return (
    <View style={styles.summarySection}>
      {data.headline ? (
        <Text style={styles.headline}>{data.headline}</Text>
      ) : null}
      {data.summary ? (
        <Text style={styles.summaryText}>{data.summary}</Text>
      ) : null}
    </View>
  );
}

export function ExperiencePDF({ entries }: { entries: ExperienceEntry[] }) {
  if (entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Work Experience</Text>
      {entries.map((entry) => (
        <View key={entry.id} style={styles.entry}>
          <View style={styles.entryHeader}>
            <View>
              <Text style={styles.entryTitle}>{entry.title}</Text>
              <Text style={styles.entrySubtitle}>{entry.company}</Text>
            </View>
            <View>
              <Text style={styles.entryDate}>
                {formatDateRange(entry.startDate, entry.endDate, entry.current)}
              </Text>
              {entry.location ? (
                <Text style={styles.entryLocation}>{entry.location}</Text>
              ) : null}
            </View>
          </View>
          {entry.highlights.length > 0 ? (
            <View style={styles.highlightsList}>
              {entry.highlights.map((highlight, i) => (
                <Text key={`${entry.id}-h-${i}`} style={styles.highlightItem}>
                  • {highlight}
                </Text>
              ))}
            </View>
          ) : null}
        </View>
      ))}
    </View>
  );
}

export function EducationPDF({ entries }: { entries: EducationEntry[] }) {
  if (entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Education</Text>
      {entries.map((entry) => (
        <View key={entry.id} style={styles.entry}>
          <View style={styles.entryHeader}>
            <View>
              <Text style={styles.entryTitle}>
                {entry.degree}
                {entry.field ? ` in ${entry.field}` : ""}
              </Text>
              <Text style={styles.entrySubtitle}>{entry.institution}</Text>
            </View>
            <View>
              <Text style={styles.entryDate}>
                {formatDateRange(entry.startDate, entry.endDate, entry.current)}
              </Text>
              {entry.location ? (
                <Text style={styles.entryLocation}>{entry.location}</Text>
              ) : null}
            </View>
          </View>
          {entry.gpa || entry.honors ? (
            <Text style={styles.highlightItem}>
              {entry.gpa ? `GPA: ${entry.gpa}` : ""}
              {entry.gpa && entry.honors ? " | " : ""}
              {entry.honors || ""}
            </Text>
          ) : null}
        </View>
      ))}
    </View>
  );
}

export function SkillsPDF({ data }: { data: SkillsData }) {
  if (data.categories.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Skills</Text>
      <View style={styles.skillsSection}>
        {data.categories.map((category) => (
          <View key={category.id} style={styles.skillCategory}>
            <Text style={styles.skillCategoryName}>{category.name}:</Text>
            <Text style={styles.skillsList}>{category.skills.join(", ")}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

export function CertificationsPDF({
  entries,
}: {
  entries: CertificationEntry[];
}) {
  if (entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Certifications</Text>
      {entries.map((entry) => (
        <View key={entry.id} style={styles.entry}>
          <View style={styles.entryHeader}>
            <View>
              <Text style={styles.entryTitle}>{entry.name}</Text>
              <Text style={styles.entrySubtitle}>{entry.issuer}</Text>
            </View>
            <Text style={styles.entryDate}>{entry.issueDate}</Text>
          </View>
        </View>
      ))}
    </View>
  );
}

export function ProjectsPDF({ entries }: { entries: ProjectEntry[] }) {
  if (entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Projects</Text>
      {entries.map((entry) => (
        <View key={entry.id} style={styles.entry}>
          <View style={styles.entryHeader}>
            <Text style={styles.entryTitle}>{entry.name}</Text>
            <Text style={styles.entryDate}>
              {formatDateRange(entry.startDate, entry.endDate, entry.current)}
            </Text>
          </View>
          {entry.description ? (
            <Text style={styles.highlightItem}>{entry.description}</Text>
          ) : null}
          {entry.technologies.length > 0 ? (
            <Text style={styles.highlightItem}>
              Technologies: {entry.technologies.join(", ")}
            </Text>
          ) : null}
        </View>
      ))}
    </View>
  );
}

export function AwardsPDF({ entries }: { entries: AwardEntry[] }) {
  if (entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Awards & Honors</Text>
      {entries.map((entry) => (
        <View key={entry.id} style={styles.entry}>
          <View style={styles.entryHeader}>
            <View>
              <Text style={styles.entryTitle}>{entry.title}</Text>
              <Text style={styles.entrySubtitle}>{entry.issuer}</Text>
            </View>
            <Text style={styles.entryDate}>{entry.date}</Text>
          </View>
          {entry.description ? (
            <Text style={styles.highlightItem}>{entry.description}</Text>
          ) : null}
        </View>
      ))}
    </View>
  );
}

export function LanguagesPDF({ data }: { data: LanguagesData }) {
  if (data.entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Languages</Text>
      <Text style={styles.skillsList}>
        {data.entries
          .map((lang) => `${lang.language} (${lang.proficiency})`)
          .join(", ")}
      </Text>
    </View>
  );
}

export function VolunteerPDF({ entries }: { entries: VolunteerEntry[] }) {
  if (entries.length === 0) {
    return null;
  }

  return (
    <View>
      <Text style={styles.sectionHeader}>Volunteer Experience</Text>
      {entries.map((entry) => (
        <View key={entry.id} style={styles.entry}>
          <View style={styles.entryHeader}>
            <View>
              <Text style={styles.entryTitle}>{entry.role}</Text>
              <Text style={styles.entrySubtitle}>{entry.organization}</Text>
            </View>
            <View>
              <Text style={styles.entryDate}>
                {formatDateRange(entry.startDate, entry.endDate, entry.current)}
              </Text>
              {entry.location ? (
                <Text style={styles.entryLocation}>{entry.location}</Text>
              ) : null}
            </View>
          </View>
          {entry.highlights.length > 0 ? (
            <View style={styles.highlightsList}>
              {entry.highlights.map((highlight, i) => (
                <Text key={`${entry.id}-h-${i}`} style={styles.highlightItem}>
                  • {highlight}
                </Text>
              ))}
            </View>
          ) : null}
        </View>
      ))}
    </View>
  );
}

export function CustomSectionPDF({ data }: { data: CustomSectionData }) {
  const hasContent = Boolean(data.title) || Boolean(data.content);
  if (!hasContent) {
    return null;
  }

  return (
    <View>
      {data.title ? (
        <Text style={styles.sectionHeader}>{data.title}</Text>
      ) : null}
      {data.content ? (
        <Text style={styles.summaryText}>{data.content}</Text>
      ) : null}
    </View>
  );
}
