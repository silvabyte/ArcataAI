import { StyleSheet } from "@react-pdf/renderer";

export const styles = StyleSheet.create({
  page: {
    padding: 40,
    fontFamily: "Helvetica",
    fontSize: 10,
    lineHeight: 1.4,
  },
  // Contact section
  contactSection: {
    marginBottom: 16,
  },
  name: {
    fontSize: 22,
    fontFamily: "Helvetica-Bold",
    marginBottom: 4,
  },
  contactInfo: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    fontSize: 9,
    color: "#4b5563",
  },
  contactItem: {
    flexDirection: "row",
    alignItems: "center",
  },
  contactLinks: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    fontSize: 9,
    color: "#6b7280",
    marginTop: 2,
  },
  // Summary section
  summarySection: {
    marginBottom: 16,
  },
  headline: {
    fontSize: 12,
    fontFamily: "Helvetica-Bold",
    marginBottom: 4,
  },
  summaryText: {
    fontSize: 10,
    color: "#374151",
  },
  // Section headers
  sectionHeader: {
    fontSize: 12,
    fontFamily: "Helvetica-Bold",
    borderBottomWidth: 1,
    borderBottomColor: "#e5e7eb",
    paddingBottom: 4,
    marginBottom: 8,
    marginTop: 12,
  },
  // Experience/Education entries
  entry: {
    marginBottom: 10,
  },
  entryHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 2,
  },
  entryTitle: {
    fontSize: 11,
    fontFamily: "Helvetica-Bold",
  },
  entrySubtitle: {
    fontSize: 10,
    color: "#4b5563",
  },
  entryDate: {
    fontSize: 9,
    color: "#6b7280",
  },
  entryLocation: {
    fontSize: 9,
    color: "#6b7280",
  },
  // Highlights/bullets
  highlightsList: {
    marginTop: 4,
    paddingLeft: 12,
  },
  highlightItem: {
    fontSize: 9,
    marginBottom: 2,
  },
  bullet: {
    width: 10,
  },
  // Skills section
  skillsSection: {
    marginBottom: 8,
  },
  skillCategory: {
    flexDirection: "row",
    marginBottom: 4,
  },
  skillCategoryName: {
    fontSize: 10,
    fontFamily: "Helvetica-Bold",
    marginRight: 8,
    minWidth: 80,
  },
  skillsList: {
    fontSize: 9,
    flex: 1,
  },
  // Generic list item
  listItem: {
    marginBottom: 4,
  },
});
