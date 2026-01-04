import { createCommand, type LexicalCommand } from "lexical";

/**
 * Custom command that DecoratorNodes can dispatch to trigger a save.
 * This is necessary because Lexical's update listener doesn't detect changes
 * to DecoratorNode internal state (only structural changes to the node tree).
 */
export const DECORATOR_NODE_CHANGED_COMMAND: LexicalCommand<void> =
  createCommand("DECORATOR_NODE_CHANGED_COMMAND");
