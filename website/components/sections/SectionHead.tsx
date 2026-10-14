import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

const tone = {
  paper: {
    rule: "border-rule",
    index: "text-vermilion",
    rubric: "text-ash",
    title: "text-ink",
    intro: "text-graphite",
  },
  ink: {
    rule: "border-white/15",
    index: "text-vermilion",
    rubric: "text-white/45",
    title: "text-paper",
    intro: "text-white/60",
  },
} as const;

/**
 * The masthead every chapter opens with: a numbered rubric in the left
 * rail, the headline and standfirst in the right. Structure comes from a
 * hairline, never from a box.
 */
export function SectionHead({
  index,
  rubric,
  title,
  intro,
  aside,
  on = "paper",
  className,
}: {
  index: string;
  rubric: string;
  title: ReactNode;
  intro?: ReactNode;
  aside?: ReactNode;
  on?: keyof typeof tone;
  className?: string;
}) {
  const t = tone[on];

  return (
    <div
      className={cn(
        "grid gap-6 border-b pb-9 md:grid-cols-[190px_1fr] md:gap-12 lg:grid-cols-[220px_1fr]",
        t.rule,
        className,
      )}
    >
      <div className="flex items-baseline gap-3 md:block">
        <span className={cn("eyebrow tabular", t.index)}>{index}</span>
        <span className={cn("eyebrow block md:mt-2", t.rubric)}>{rubric}</span>
      </div>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between lg:gap-14">
        <div>
          <h2
            className={cn(
              "display font-sans font-bold text-balance",
              t.title,
            )}
            style={{ fontSize: "clamp(1.9rem, 3.6vw, 3.25rem)" }}
          >
            {title}
          </h2>
          {intro && (
            <p className={cn("measure mt-5 text-[16px] md:text-[17px]", t.intro)}>
              {intro}
            </p>
          )}
        </div>
        {aside && <div className="shrink-0 lg:pb-2">{aside}</div>}
      </div>
    </div>
  );
}
