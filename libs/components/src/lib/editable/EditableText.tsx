import { useEffect, useRef, useState } from "react";
import { cn } from "../utils";

type EditableTextProps = {
  value: string;
  onSave: (newValue: string) => Promise<void> | void;
  className?: string;
  inputClassName?: string;
  placeholder?: string;
  onEditingChange?: (isEditing: boolean) => void;
};

export function EditableText({
  value,
  onSave,
  className,
  inputClassName,
  placeholder = "Enter text...",
  onEditingChange,
}: EditableTextProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(value);
  const [isSaving, setIsSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const updateEditing = (editing: boolean) => {
    setIsEditing(editing);
    onEditingChange?.(editing);
  };

  useEffect(() => {
    setEditValue(value);
  }, [value]);

  useEffect(() => {
    if (isEditing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [isEditing]);

  const handleSave = async () => {
    const trimmedValue = editValue.trim();
    if (trimmedValue === "" || trimmedValue === value) {
      setEditValue(value);
      updateEditing(false);
      return;
    }

    setIsSaving(true);
    try {
      await onSave(trimmedValue);
      updateEditing(false);
    } catch {
      setEditValue(value);
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = () => {
    setEditValue(value);
    updateEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleSave();
    } else if (e.key === "Escape") {
      e.preventDefault();
      handleCancel();
    }
  };

  const handleBlur = () => {
    if (!isSaving) {
      handleSave();
    }
  };

  if (isEditing) {
    return (
      <div className="flex items-center gap-2">
        <input
          className={cn(
            "w-auto min-w-[3ch] border-none bg-transparent outline-none",
            isSaving && "opacity-50",
            inputClassName
          )}
          disabled={isSaving}
          onBlur={handleBlur}
          onChange={(e) => setEditValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          ref={inputRef}
          size={Math.max(editValue.length, 1)}
          type="text"
          value={editValue}
        />
        <button
          className="flex h-5 w-5 items-center justify-center rounded text-white/70 transition-colors hover:bg-white/20 hover:text-white"
          onClick={(e) => {
            e.preventDefault();
            handleSave();
          }}
          title="Save"
          type="button"
        >
          <svg
            aria-hidden="true"
            className="h-3.5 w-3.5"
            fill="none"
            stroke="currentColor"
            strokeWidth={2.5}
            viewBox="0 0 24 24"
          >
            <path
              d="M5 13l4 4L19 7"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
        <button
          className="flex h-5 w-5 items-center justify-center rounded text-white/70 transition-colors hover:bg-white/20 hover:text-white"
          onClick={(e) => {
            e.preventDefault();
            handleCancel();
          }}
          title="Cancel"
          type="button"
        >
          <svg
            aria-hidden="true"
            className="h-3.5 w-3.5"
            fill="none"
            stroke="currentColor"
            strokeWidth={2.5}
            viewBox="0 0 24 24"
          >
            <path
              d="M6 18L18 6M6 6l12 12"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      </div>
    );
  }

  return (
    <button
      className={cn(
        "m-0 cursor-pointer border-none bg-transparent p-0 font-inherit text-inherit",
        "transition-opacity hover:opacity-70",
        "focus:opacity-70 focus:outline-none",
        className
      )}
      onClick={() => updateEditing(true)}
      title="Click to edit"
      type="button"
    >
      {value}
    </button>
  );
}
