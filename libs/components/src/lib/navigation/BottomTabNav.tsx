import type { ComponentType } from "react";
import { cn } from "../utils";

export type NavItem = {
  name: string;
  href: string;
  // biome-ignore lint/suspicious/noExplicitAny: Heroicons use complex ForwardRef types
  icon: ComponentType<any>;
  current?: boolean;
};

export type BottomTabNavProps = {
  items: NavItem[];
  onNavigate?: (href: string) => void;
};

export function BottomTabNav({ items, onNavigate }: BottomTabNavProps) {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 border-gray-200 border-t bg-gray-900 pb-safe">
      <div className="flex h-16 items-center justify-around">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <button
              className={cn(
                "flex flex-1 flex-col items-center justify-center gap-1 py-2",
                item.current ? "text-white" : "text-gray-400"
              )}
              key={item.name}
              onClick={() => onNavigate?.(item.href)}
              type="button"
            >
              <Icon className="h-5 w-5" />
              <span className="text-xs">{item.name}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
