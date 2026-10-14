import { PopNumber } from "../ui/pop-number";
import { SectionHead } from "./SectionHead";

const stats = [
  { value: "237", label: "Unit tests", note: "all green" },
  { value: "4", label: "Ecosystems", note: "Stremio · CloudStream · Aniyomi · Mihon" },
  { value: "25k", label: "Lines of Kotlin", note: "no wrapper, no WebView" },
  { value: "2X", label: "Hold-for-2X", note: "player speed, on a press" },
];

/**
 * A black plate dropped into the middle of the paper — the rhythm break
 * that stops a long light page from reading flat.
 */
export function Numbers() {
  return (
    <section id="numbers" className="bg-ink text-paper">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <SectionHead
          on="ink"
          index="02"
          rubric="By the numbers"
          title={
            <>
              Built like a flagship,{" "}
              <span className="font-serif italic">not a wrapper.</span>
            </>
          }
          intro="Every screen, sheet and gesture went through dozens of documented build
            phases. The polish is measurable."
        />

        <dl className="mt-12 grid grid-cols-2 gap-x-8 gap-y-12 md:grid-cols-4 md:gap-x-0">
          {stats.map((s, i) => (
            <div
              key={s.label}
              className={
                i === 0
                  ? "flex flex-col md:pr-8"
                  : "flex flex-col md:border-l md:border-white/15 md:pl-8 md:pr-8"
              }
            >
              <dt className="eyebrow order-2 mt-5 text-white/80">{s.label}</dt>
              <dd
                className="order-1 font-sans font-bold tabular text-paper"
                style={{
                  fontSize: "clamp(2.75rem, 6vw, 5rem)",
                  lineHeight: 0.9,
                  letterSpacing: "-0.05em",
                }}
              >
                <PopNumber value={s.value} />
              </dd>
              <dd className="order-3 mt-2 text-[13px] leading-relaxed text-white/45">
                {s.note}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
