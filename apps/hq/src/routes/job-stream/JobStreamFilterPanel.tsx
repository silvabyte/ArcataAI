import {
  FilterCheckbox,
  FilterCombobox,
  type FilterComboboxOption,
  FilterRadio,
  FilterSection,
} from "@arcata/components";
import type { JobFilterOptions } from "@arcata/db";
import { t } from "@arcata/translate";
import { FilterPanelHeader } from "./FilterPanelHeader";
import type {
  DegreeLevel,
  ExperienceLevel,
  JobStreamFilters,
  JobType,
  SortOption,
} from "./filterTypes";

// Static options for checkbox/radio groups (these are app-defined enums)
const experienceOptions = [
  { id: "intern", labelKey: "pages.jobStream.filters.experience.intern" },
  { id: "early", labelKey: "pages.jobStream.filters.experience.early" },
  { id: "mid", labelKey: "pages.jobStream.filters.experience.mid" },
  { id: "senior", labelKey: "pages.jobStream.filters.experience.senior" },
  { id: "advanced", labelKey: "pages.jobStream.filters.experience.advanced" },
  { id: "director", labelKey: "pages.jobStream.filters.experience.director" },
  { id: "principal", labelKey: "pages.jobStream.filters.experience.principal" },
] as const;

const degreeOptions = [
  { id: "none", labelKey: "pages.jobStream.filters.degree.none" },
  { id: "pursuing", labelKey: "pages.jobStream.filters.degree.pursuing" },
  { id: "bootcamp", labelKey: "pages.jobStream.filters.degree.bootcamp" },
  {
    id: "certification",
    labelKey: "pages.jobStream.filters.degree.certification",
  },
  { id: "associate", labelKey: "pages.jobStream.filters.degree.associate" },
  { id: "bachelors", labelKey: "pages.jobStream.filters.degree.bachelors" },
  { id: "masters", labelKey: "pages.jobStream.filters.degree.masters" },
  { id: "phd", labelKey: "pages.jobStream.filters.degree.phd" },
] as const;

const jobTypeOptions = [
  { id: "full-time", labelKey: "pages.jobStream.filters.jobTypes.fullTime" },
  { id: "part-time", labelKey: "pages.jobStream.filters.jobTypes.partTime" },
  { id: "contract", labelKey: "pages.jobStream.filters.jobTypes.contract" },
  { id: "temporary", labelKey: "pages.jobStream.filters.jobTypes.temporary" },
  { id: "intern", labelKey: "pages.jobStream.filters.jobTypes.intern" },
  { id: "freelance", labelKey: "pages.jobStream.filters.jobTypes.freelance" },
] as const;

const sortOptions = [
  { id: "relevance", labelKey: "pages.jobStream.filters.sort.relevance" },
  { id: "date", labelKey: "pages.jobStream.filters.sort.date" },
] as const;

export type JobStreamFilterPanelProps = {
  isMobile?: boolean;
  filters: JobStreamFilters;
  filterOptions: JobFilterOptions;
  onFiltersChange: (filters: JobStreamFilters) => void;
  jobCount: number;
  onClearFilters: () => void;
  hasActiveFilters: boolean;
};

export function JobStreamFilterPanel({
  isMobile = false,
  filters,
  filterOptions,
  onFiltersChange,
  jobCount,
  onClearFilters,
  hasActiveFilters,
}: JobStreamFilterPanelProps) {
  // Mobile version doesn't need fixed width or border - it's inside a bottom sheet
  const containerClass = isMobile
    ? "flex flex-col"
    : "flex h-full w-72 flex-col border-gray-200 border-r bg-gray-50";

  const contentClass = isMobile ? "" : "flex-1 overflow-y-auto p-4";

  // Handlers for locations combobox
  const selectedLocations: FilterComboboxOption[] = filters.locations.map(
    (loc) => ({
      id: loc,
      label:
        filterOptions.locations.find((l) => l.id === loc)?.label ?? String(loc),
    })
  );

  const handleLocationSelect = (option: FilterComboboxOption) => {
    onFiltersChange({
      ...filters,
      locations: [...filters.locations, String(option.id)],
    });
  };

  const handleLocationRemove = (option: FilterComboboxOption) => {
    onFiltersChange({
      ...filters,
      locations: filters.locations.filter((loc) => loc !== option.id),
    });
  };

  // Handlers for skills combobox (skills are user-entered, no predefined options)
  const selectedSkills: FilterComboboxOption[] = filters.skills.map(
    (skill) => ({
      id: skill,
      label: skill,
    })
  );

  const handleSkillSelect = (option: FilterComboboxOption) => {
    onFiltersChange({
      ...filters,
      skills: [...filters.skills, String(option.id)],
    });
  };

  const handleSkillRemove = (option: FilterComboboxOption) => {
    onFiltersChange({
      ...filters,
      skills: filters.skills.filter((skill) => skill !== option.id),
    });
  };

  // Handlers for organizations combobox
  const selectedOrganizations: FilterComboboxOption[] =
    filters.organizationIds.map((id) => ({
      id,
      label:
        filterOptions.companies.find((c) => c.id === id)?.label ?? String(id),
    }));

  const handleOrganizationSelect = (option: FilterComboboxOption) => {
    onFiltersChange({
      ...filters,
      organizationIds: [...filters.organizationIds, Number(option.id)],
    });
  };

  const handleOrganizationRemove = (option: FilterComboboxOption) => {
    onFiltersChange({
      ...filters,
      organizationIds: filters.organizationIds.filter((id) => id !== option.id),
    });
  };

  // Handler for experience checkboxes
  const handleExperienceChange = (level: ExperienceLevel, checked: boolean) => {
    if (checked) {
      onFiltersChange({
        ...filters,
        experience: [...filters.experience, level],
      });
    } else {
      onFiltersChange({
        ...filters,
        experience: filters.experience.filter((exp) => exp !== level),
      });
    }
  };

  // Handler for degree checkboxes
  const handleDegreeChange = (level: DegreeLevel, checked: boolean) => {
    if (checked) {
      onFiltersChange({
        ...filters,
        degree: [...filters.degree, level],
      });
    } else {
      onFiltersChange({
        ...filters,
        degree: filters.degree.filter((deg) => deg !== level),
      });
    }
  };

  // Handler for job type checkboxes
  const handleJobTypeChange = (type: JobType, checked: boolean) => {
    if (checked) {
      onFiltersChange({
        ...filters,
        jobTypes: [...filters.jobTypes, type],
      });
    } else {
      onFiltersChange({
        ...filters,
        jobTypes: filters.jobTypes.filter((jt) => jt !== type),
      });
    }
  };

  // Handler for sort option
  const handleSortChange = (option: SortOption) => {
    onFiltersChange({
      ...filters,
      sortBy: option,
    });
  };

  return (
    <div className={containerClass}>
      <div className={contentClass}>
        {/* Header with job count and clear filters */}
        <FilterPanelHeader
          hasActiveFilters={hasActiveFilters}
          jobCount={jobCount}
          onClearFilters={onClearFilters}
        />

        {/* Job Title Input */}
        <div className="mb-4">
          <label
            className="mb-2 block font-medium text-gray-900 text-sm"
            htmlFor="jobTitle"
          >
            {t("pages.jobStream.filters.jobTitle")}
          </label>
          <input
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-500"
            id="jobTitle"
            onChange={(e) =>
              onFiltersChange({ ...filters, search: e.target.value })
            }
            placeholder={t("pages.jobStream.filters.jobTitlePlaceholder")}
            type="text"
            value={filters.search}
          />
        </div>

        {/* Locations */}
        <FilterSection title={t("pages.jobStream.filters.locations")}>
          <FilterCombobox
            onRemove={handleLocationRemove}
            onSelect={handleLocationSelect}
            options={filterOptions.locations}
            placeholder={t("pages.jobStream.filters.locationsPlaceholder")}
            selected={selectedLocations}
          />
          {/* Remote Work Toggle */}
          <div className="mt-3">
            <FilterCheckbox
              checked={filters.remote}
              id="remote-work"
              label={t("pages.jobStream.filters.remoteOnly")}
              onChange={(checked) =>
                onFiltersChange({ ...filters, remote: checked })
              }
            />
          </div>
        </FilterSection>

        {/* Experience */}
        <FilterSection title={t("pages.jobStream.filters.experience.title")}>
          <div className="grid grid-cols-2 gap-2">
            {experienceOptions.map((option) => (
              <FilterCheckbox
                checked={filters.experience.includes(
                  option.id as ExperienceLevel
                )}
                id={`experience-${option.id}`}
                key={option.id}
                label={t(option.labelKey)}
                onChange={(checked) =>
                  handleExperienceChange(option.id as ExperienceLevel, checked)
                }
              />
            ))}
          </div>
        </FilterSection>

        {/* Skills & Qualifications */}
        <FilterSection title={t("pages.jobStream.filters.skills")}>
          <FilterCombobox
            allowCustom
            onRemove={handleSkillRemove}
            onSelect={handleSkillSelect}
            options={filterOptions.skills}
            placeholder={t("pages.jobStream.filters.skillsPlaceholder")}
            selected={selectedSkills}
          />
        </FilterSection>

        {/* Degree */}
        <FilterSection title={t("pages.jobStream.filters.degree.title")}>
          <div className="grid grid-cols-2 gap-2">
            {degreeOptions.map((option) => (
              <FilterCheckbox
                checked={filters.degree.includes(option.id as DegreeLevel)}
                id={`degree-${option.id}`}
                key={option.id}
                label={t(option.labelKey)}
                onChange={(checked) =>
                  handleDegreeChange(option.id as DegreeLevel, checked)
                }
              />
            ))}
          </div>
        </FilterSection>

        {/* Job Types */}
        <FilterSection title={t("pages.jobStream.filters.jobTypes.title")}>
          <div className="grid grid-cols-2 gap-2">
            {jobTypeOptions.map((option) => (
              <FilterCheckbox
                checked={filters.jobTypes.includes(option.id as JobType)}
                id={`jobtype-${option.id}`}
                key={option.id}
                label={t(option.labelKey)}
                onChange={(checked) =>
                  handleJobTypeChange(option.id as JobType, checked)
                }
              />
            ))}
          </div>
        </FilterSection>

        {/* Organizations */}
        <FilterSection title={t("pages.jobStream.filters.organizations")}>
          <FilterCombobox
            onRemove={handleOrganizationRemove}
            onSelect={handleOrganizationSelect}
            options={filterOptions.companies}
            placeholder={t("pages.jobStream.filters.organizationsPlaceholder")}
            selected={selectedOrganizations}
          />
        </FilterSection>

        {/* Sort By */}
        <FilterSection title={t("pages.jobStream.filters.sortBy")}>
          <div className="space-y-2">
            {sortOptions.map((option) => (
              <FilterRadio
                checked={filters.sortBy === option.id}
                id={`sort-${option.id}`}
                key={option.id}
                label={t(option.labelKey)}
                name="sortBy"
                onChange={() => handleSortChange(option.id as SortOption)}
                value={option.id}
              />
            ))}
          </div>
        </FilterSection>
      </div>
    </div>
  );
}
