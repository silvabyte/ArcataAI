import { LexicalErrorBoundary } from "@lexical/react/LexicalErrorBoundary";
import { HistoryPlugin } from "@lexical/react/LexicalHistoryPlugin";
import { RichTextPlugin } from "@lexical/react/LexicalRichTextPlugin";
import { useState } from "react";

import { ContentEditable } from "../../editor/editor-ui/content-editable";
import { DraggableBlockPlugin } from "../../editor/plugins/draggable-block-plugin";
import { InsertSectionPlugin } from "./plugins/InsertSectionPlugin";

export function Plugins() {
  const [floatingAnchorElem, setFloatingAnchorElem] =
    useState<HTMLDivElement | null>(null);

  const onRef = (elem: HTMLDivElement | null) => {
    if (elem !== null) {
      setFloatingAnchorElem(elem);
    }
  };

  return (
    <div className="space-y-4">
      {/* toolbar - now floating */}
      <InsertSectionPlugin />
      {/* document */}
      <div
        className="relative rounded-xl border border-gray-200 bg-white shadow-sm"
        ref={onRef}
      >
        <RichTextPlugin
          contentEditable={
            <ContentEditable
              className="relative block min-h-[600px] px-6 py-8 focus:outline-none lg:px-10 lg:py-10"
              placeholder=""
            />
          }
          ErrorBoundary={LexicalErrorBoundary}
        />
        <HistoryPlugin />
        <DraggableBlockPlugin anchorElem={floatingAnchorElem} />
      </div>
    </div>
  );
}
