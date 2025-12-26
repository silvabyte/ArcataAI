import type { KanbanApplication } from "./types";

const ORDER_GAP = 1000;

/**
 * Calculate the new status_order for a card being inserted into a column.
 * Uses sparse ordering with gaps of 1000 to minimize reordering.
 */
export function calculateNewOrder(
  siblings: KanbanApplication[], // Cards in destination column (excluding moved card), sorted by status_order
  insertIndex: number
): number {
  // Empty column
  if (siblings.length === 0) {
    return ORDER_GAP;
  }

  // Insert at beginning
  if (insertIndex === 0) {
    const firstOrder = siblings[0].application.status_order;
    return Math.max(0, firstOrder - ORDER_GAP);
  }

  // Insert at end
  if (insertIndex >= siblings.length) {
    const lastOrder = siblings.at(-1)?.application.status_order ?? 0;
    return lastOrder + ORDER_GAP;
  }

  // Insert between two cards
  const before = siblings[insertIndex - 1].application.status_order;
  const after = siblings[insertIndex].application.status_order;
  return Math.floor((before + after) / 2);
}

/**
 * Find which item changed between old and new data arrays.
 * Returns the changed item from newData, or null if no change.
 */
export function findChangedItem(
  oldData: KanbanApplication[],
  newData: KanbanApplication[]
): KanbanApplication | null {
  for (const newItem of newData) {
    const oldItem = oldData.find((d) => d.id === newItem.id);
    if (!oldItem) {
      continue;
    }

    // Check if column changed
    if (newItem.column !== oldItem.column) {
      return newItem;
    }

    // Check if position changed within same column
    const oldIndex = oldData
      .filter((d) => d.column === oldItem.column)
      .findIndex((d) => d.id === oldItem.id);
    const newIndex = newData
      .filter((d) => d.column === newItem.column)
      .findIndex((d) => d.id === newItem.id);
    if (oldIndex !== newIndex) {
      return newItem;
    }
  }
  return null;
}
