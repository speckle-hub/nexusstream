import { cn } from "@/lib/utils";

export function BentoGrid({ className, children }: { className?: string; children?: React.ReactNode }) {
  return (
    <div className={cn("mx-auto grid max-w-7xl grid-cols-1 gap-4 md:grid-cols-6", className)}>
      {children}
    </div>
  );
}

export function BentoGridItem({
  className,
  title,
  description,
  header,
}: {
  className?: string;
  title: string;
  description: string;
  header?: React.ReactNode;
}) {
  return (
    <div
      className={cn(
        "group/bento relative row-span-1 flex flex-col justify-between overflow-hidden rounded-2xl border border-white/[0.08] bg-ink/80 shadow-card transition duration-300 hover:border-white/[0.16]",
        className,
      )}
    >
      {header}
      <div className="p-5 md:p-6">
        <div className="font-display text-lg font-semibold text-mist transition duration-300 group-hover/bento:translate-x-1">
          {title}
        </div>
        <div className="mt-1.5 text-sm leading-relaxed text-muted transition duration-300 group-hover/bento:translate-x-1">
          {description}
        </div>
      </div>
    </div>
  );
}
