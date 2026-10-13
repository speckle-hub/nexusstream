export function generate(el: Element, options?: Record<string, unknown>): { finished: Promise<void>; cancel: () => void };
export function auto(root?: Document | Element): void;
