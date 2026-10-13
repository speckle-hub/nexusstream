"use client";

import { useEffect } from "react";

/**
 * Boots transitions-ui-generation `auto()`: any `[data-t-generate]` element
 * plays its wireframe-to-UI build when it scrolls into view.
 */
export function GenAuto() {
  useEffect(() => {
    let cancelled = false;
    import("./tgen/ui-generation.js").then((m) => {
      if (!cancelled) m.auto();
    });
    return () => {
      cancelled = true;
    };
  }, []);
  return null;
}
