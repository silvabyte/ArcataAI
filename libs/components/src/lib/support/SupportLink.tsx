import { t } from "@arcata/translate";
import type React from "react";
import { classNames } from "../css";

type SupportLinkProps = {
  href?: string;
  classes?: string;
};

export const SupportLink: React.FC<SupportLinkProps> = ({
  href = "mailto:info@arcata.ai",
  classes = "",
}) => {
  const cx = classNames("text-sm font-semibold text-gray-900", classes);
  return (
    <a className={cx} href={href}>
      {t("common.support.contact")} <span aria-hidden="true">&rarr;</span>
    </a>
  );
};
