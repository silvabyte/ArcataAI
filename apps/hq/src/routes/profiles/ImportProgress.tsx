import { Progress } from "@arcata/components";
import { SparklesIcon } from "@heroicons/react/24/outline";
import { useEffect, useState } from "react";

type ImportProgressProps = {
  progress: number;
  message: string;
};

const AI_THINKING_MESSAGES = [
  "Reading your experience...",
  "Analyzing skills and qualifications...",
  "Extracting contact information...",
  "Identifying key achievements...",
  "Processing employment history...",
  "Organizing your profile...",
];

export function ImportProgress({ progress, message }: ImportProgressProps) {
  const [thinkingIndex, setThinkingIndex] = useState(0);
  const [dots, setDots] = useState("");

  // Cycle through thinking messages when progress is in the "AI working" range
  useEffect(() => {
    if (progress >= 30 && progress < 95) {
      const interval = setInterval(() => {
        setThinkingIndex((prev) => (prev + 1) % AI_THINKING_MESSAGES.length);
      }, 3000);
      return () => clearInterval(interval);
    }
  }, [progress]);

  // Animated dots to show activity
  useEffect(() => {
    const interval = setInterval(() => {
      setDots((prev) => (prev.length >= 3 ? "" : `${prev}.`));
    }, 400);
    return () => clearInterval(interval);
  }, []);

  const showAiIndicator = progress >= 30 && progress < 95;
  const displayMessage = showAiIndicator
    ? AI_THINKING_MESSAGES[thinkingIndex]
    : message;

  return (
    <div className="w-full space-y-6">
      {/* Progress bar with subtle pulse animation when AI is working */}
      <div className="relative">
        <Progress
          className={`h-2 transition-all ${showAiIndicator ? "animate-pulse" : ""}`}
          value={progress}
        />
      </div>

      {/* AI thinking indicator */}
      {showAiIndicator ? (
        <div className="flex items-center justify-center gap-2">
          <SparklesIcon className="h-5 w-5 animate-bounce text-indigo-500" />
          <span className="font-medium text-indigo-600 text-sm">
            AI is processing
          </span>
          <span className="w-4 font-mono text-indigo-500 text-sm">{dots}</span>
        </div>
      ) : null}

      {/* Current step message */}
      <p
        className={`text-center text-sm transition-opacity duration-300 ${
          showAiIndicator ? "text-gray-600" : "text-gray-500"
        }`}
      >
        {displayMessage}
      </p>

      {/* Time estimation hint */}
      {showAiIndicator ? (
        <p className="text-center text-gray-400 text-xs">
          This usually takes 10-30 seconds
        </p>
      ) : null}
    </div>
  );
}
