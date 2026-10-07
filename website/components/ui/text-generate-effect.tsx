"use client";

import { m } from "framer-motion";
import { cn } from "@/lib/utils";

/** Aceternity Text Generate Effect: words blur-fade in one by one. */
export function TextGenerateEffect({
  words,
  className,
  wordClassName,
  delay = 0,
}: {
  words: string;
  className?: string;
  wordClassName?: string;
  delay?: number;
}) {
  const tokens = words.split(" ");
  return (
    <div className={cn("leading-snug", className)}>
      {tokens.map((word, i) => (
        <m.span
          key={`${word}-${i}`}
          className={cn("inline-block opacity-0", wordClassName)}
          initial={{ opacity: 0, filter: "blur(8px)", y: 6 }}
          whileInView={{ opacity: 1, filter: "blur(0px)", y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.5, delay: delay + i * 0.06, ease: "easeOut" }}
        >
          {word}
          {i < tokens.length - 1 ? " " : ""}
        </m.span>
      ))}
    </div>
  );
}
