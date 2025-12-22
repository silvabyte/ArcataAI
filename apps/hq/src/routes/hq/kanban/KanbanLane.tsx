import { EditableText } from "@arcata/components";
import type React from "react";
import { useState } from "react";
import "./KanBanLane.css";

type KanbanLaneProps = {
  title: string;
  count: number;
  children: React.ReactNode;
  statusId?: number;
  onRename?: (statusId: number, newName: string) => Promise<void>;
};

const KanbanLane: React.FC<KanbanLaneProps> = ({
  title,
  count,
  children,
  statusId,
  onRename,
}) => {
  const [isEditingTitle, setIsEditingTitle] = useState(false);

  const handleSave = async (newValue: string) => {
    if (statusId !== undefined && onRename) {
      await onRename(statusId, newValue);
    }
  };

  const isEditable = statusId !== undefined && onRename !== undefined;

  return (
    <div className="flex h-full w-58 shrink-0 flex-col">
      <div className="kb-header mb-3 flex h-12 w-58 items-center justify-between rounded-lg px-4">
        {isEditable ? (
          <EditableText
            className="font-semibold text-2xl text-white"
            inputClassName="font-semibold text-2xl text-white"
            onEditingChange={setIsEditingTitle}
            onSave={handleSave}
            value={title}
          />
        ) : (
          <p className="font-semibold text-2xl text-white">{title}</p>
        )}
        {!isEditingTitle && (
          <p className="font-extralight text-4xl text-white">{count}</p>
        )}
      </div>
      <div className="flex-1">{children}</div>
    </div>
  );
};

export default KanbanLane;
