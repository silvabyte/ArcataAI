import { Progress } from "@arcata/components";

type ImportProgressProps = {
  progress: number;
  message: string;
};

export function ImportProgress({ progress, message }: ImportProgressProps) {
  return (
    <div className="w-full space-y-4">
      <Progress className="h-2" value={progress} />
      <p className="text-center text-gray-500 text-sm">{message}</p>
    </div>
  );
}
