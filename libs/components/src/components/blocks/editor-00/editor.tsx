"use client";

import {
  type InitialConfigType,
  LexicalComposer,
} from "@lexical/react/LexicalComposer";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import {
  COMMAND_PRIORITY_NORMAL,
  type EditorState,
  type SerializedEditorState,
} from "lexical";
import { useEffect, useRef } from "react";
import { TooltipProvider } from "../../../ui/tooltip";
import { editorTheme } from "../../editor/themes/editor-theme";

import { DECORATOR_NODE_CHANGED_COMMAND } from "./commands";
import { nodes } from "./nodes";
import { Plugins } from "./plugins";

const editorConfig: InitialConfigType = {
  namespace: "Editor",
  theme: editorTheme,
  nodes,
  onError: (error: Error) => {
    console.error(error);
  },
};

/**
 * Plugin that listens for updates and save triggers
 */
function SavePlugin({
  onChange,
  onSerializedChange,
}: {
  onChange?: (editorState: EditorState) => void;
  onSerializedChange?: (editorSerializedState: SerializedEditorState) => void;
}) {
  const [editor] = useLexicalComposerContext();
  const hasInitialized = useRef(false);

  // Listen for the custom command from DecoratorNodes
  useEffect(
    () =>
      editor.registerCommand(
        DECORATOR_NODE_CHANGED_COMMAND,
        () => {
          const editorState = editor.getEditorState();
          onChange?.(editorState);
          onSerializedChange?.(editorState.toJSON());
          return true;
        },
        COMMAND_PRIORITY_NORMAL
      ),
    [editor, onChange, onSerializedChange]
  );

  // Also listen for structural changes (adding/removing nodes)
  useEffect(() => {
    return editor.registerUpdateListener(
      ({ editorState, dirtyElements, dirtyLeaves }) => {
        // Skip the first update (initial state load)
        if (!hasInitialized.current) {
          hasInitialized.current = true;
          return;
        }

        // Only trigger for structural changes (dirtyElements > 0 means nodes changed)
        if (dirtyElements.size === 0 && dirtyLeaves.size === 0) {
          return;
        }

        onChange?.(editorState);
        onSerializedChange?.(editorState.toJSON());
      }
    );
  }, [editor, onChange, onSerializedChange]);

  return null;
}

export function Editor({
  editorState,
  editorSerializedState,
  onChange,
  onSerializedChange,
}: {
  editorState?: EditorState;
  editorSerializedState?: SerializedEditorState;
  onChange?: (editorState: EditorState) => void;
  onSerializedChange?: (editorSerializedState: SerializedEditorState) => void;
}) {
  return (
    <div className="space-y-4">
      <LexicalComposer
        initialConfig={{
          ...editorConfig,
          ...(editorState ? { editorState } : {}),
          ...(editorSerializedState
            ? { editorState: JSON.stringify(editorSerializedState) }
            : {}),
        }}
      >
        <TooltipProvider>
          <Plugins />

          <SavePlugin
            onChange={onChange}
            onSerializedChange={onSerializedChange}
          />
        </TooltipProvider>
      </LexicalComposer>
    </div>
  );
}
