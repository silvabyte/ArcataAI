import { ArrowUpTrayIcon } from "@heroicons/react/24/outline";
import { useCallback, useRef, useState } from "react";

type ResumeFileUploadProps = {
  onFileSelect: (file: File) => void;
  isLoading?: boolean;
};

export function ResumeFileUpload({
  onFileSelect,
  isLoading,
}: ResumeFileUploadProps) {
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  }, []);

  const validateFile = (file: File): boolean => {
    const validTypes = [
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/msword",
    ];
    // Check extension as backup for mime type
    const validExtensions = [".pdf", ".docx", ".doc"];
    const extension = `.${file.name.split(".").pop()?.toLowerCase()}`;

    if (
      !(validTypes.includes(file.type) || validExtensions.includes(extension))
    ) {
      setError("Please upload a PDF or DOCX file.");
      return false;
    }

    if (file.size > 10 * 1024 * 1024) {
      setError("File size must be less than 10MB.");
      return false;
    }

    return true;
  };

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setDragActive(false);
      setError(null);

      if (e.dataTransfer.files?.[0]) {
        const file = e.dataTransfer.files[0];
        if (validateFile(file)) {
          onFileSelect(file);
        }
      }
    },
    [onFileSelect, validateFile]
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      e.preventDefault();
      setError(null);
      if (e.target.files?.[0]) {
        const file = e.target.files[0];
        // Browser file input handles accept attribute, but we double check size
        if (file.size > 10 * 1024 * 1024) {
          setError("File size must be less than 10MB.");
          return;
        }
        onFileSelect(file);
      }
    },
    [onFileSelect]
  );

  const onZoneClick = () => {
    if (!isLoading) {
      inputRef.current?.click();
    }
  };

  return (
    <div className="space-y-2">
      <div
        aria-label="Upload resume file"
        className={`relative flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-10 transition-colors ${
          dragActive
            ? "border-blue-500 bg-blue-50"
            : "border-gray-200 hover:border-gray-300 hover:bg-gray-50"
        } ${isLoading ? "pointer-events-none opacity-50" : ""}`}
        onClick={onZoneClick}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            onZoneClick();
          }
        }}
        role="button"
        tabIndex={0}
      >
        <input
          accept=".pdf,.docx,.doc"
          className="hidden"
          disabled={isLoading}
          onChange={handleChange}
          ref={inputRef}
          type="file"
        />

        <div className="mb-4 rounded-full bg-white p-4 shadow-sm ring-1 ring-gray-900/5">
          <ArrowUpTrayIcon className="h-8 w-8 text-gray-400" />
        </div>

        <div className="text-center">
          <p className="mb-2 font-semibold text-gray-900 text-sm">
            Click to upload{" "}
            <span className="font-normal text-gray-500">or drag and drop</span>
          </p>
          <p className="text-gray-500 text-xs">PDF or DOCX (max 10MB)</p>
        </div>
      </div>

      {error && <p className="text-center text-red-600 text-sm">{error}</p>}
    </div>
  );
}
