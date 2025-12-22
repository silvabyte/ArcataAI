import type { SVGProps } from "react";

/** Kanban board icon - 3-column layout representing a tracker board */
export function BoardIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      aria-hidden="true"
      data-slot="icon"
      fill="none"
      viewBox="0 0 20 16"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <path
        d="M7 13V3m0 10a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2m0 10a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M7 3a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2m0 10V3m0 10a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2V3a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
      />
    </svg>
  );
}
