import type React from "react";
import { type ReactNode, useMemo } from "react";
import { classNames } from "../css";

type AlertProps = {
  type: "danger" | "info" | "success" | "warning";
  title: string;
  children?: ReactNode;
};

export const Alert: React.FC<AlertProps> = ({
  type,
  title,
  children,
}: AlertProps) => {
  const alertClasses = useMemo(() => {
    const typeClasses = {
      danger: "bg-red-50 text-red-800",
      info: "bg-blue-50 text-blue-800",
      success: "bg-green-50 text-green-800",
      warning: "bg-yellow-50 text-yellow-800",
    };

    return classNames("rounded-md p-4 mb-4", typeClasses[type]);
  }, [type]);

  return (
    <div className={alertClasses}>
      <h3 className="font-medium text-sm">{title}</h3>
      {children}
    </div>
  );
};
