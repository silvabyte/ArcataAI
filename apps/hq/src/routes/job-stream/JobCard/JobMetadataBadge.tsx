import { t } from "@arcata/translate";
import {
  BuildingOfficeIcon,
  ClockIcon,
  EyeIcon,
  HomeIcon,
  MapPinIcon,
} from "@heroicons/react/24/outline";

export type JobMetadataBadgeProps = {
  type: "status" | "level" | "jobType" | "location";
  value: string;
};

const iconMap = {
  status: EyeIcon,
  level: BuildingOfficeIcon,
  jobType: ClockIcon,
  location: MapPinIcon,
};

export function JobMetadataBadge({ type, value }: JobMetadataBadgeProps) {
  const Icon =
    type === "location" && value === "Remote" ? HomeIcon : iconMap[type];

  // Get translated label for known values
  const getLabel = () => {
    if (type === "status") {
      return t(`pages.jobStream.badge.${value}`);
    }
    if (type === "level") {
      return t(`pages.jobStream.badge.${value}`);
    }
    if (type === "jobType") {
      const keyMap: Record<string, string> = {
        "full-time": "fullTime",
        "part-time": "partTime",
        contract: "contract",
        temporary: "temporary",
        intern: "intern",
      };
      const key = keyMap[value] || value;
      return t(`pages.jobStream.badge.${key}`);
    }
    // Location is passed through as-is
    return value;
  };

  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-gray-600 text-xs">
      <Icon className="h-3.5 w-3.5" />
      <span>{getLabel()}</span>
    </span>
  );
}
