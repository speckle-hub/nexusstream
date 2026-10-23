import { PopNumber } from "../ui/pop-number";
import { Stagger } from "../ui/reveal";

const stats = [
  { value: "301", label: "Unit tests", note: "all green" },
  { value: "4", label: "Ecosystems", note: "Stremio · CloudStream · Aniyomi · Mihon" },
  { value: "25k", label: "Lines of Kotlin", note: "no wrapper, no WebView" },
  { value: "2X", label: "Hold-for-2X", note: "player speed, on a press" },
];

export function Stats() {
  return (
    <section aria-label="Project numbers" className="border-t border-rule">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-24">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              03 — By the numbers
            </p>,
          ]}
        />
        <dl className="mt-10 grid grid-cols-2 gap-x-8 gap-y-12 md:grid-cols-4 md:gap-x-0">
          {stats.map((s, i) => (
            <div
              key={s.label}
              className={
                i === 0
                  ? "flex flex-col md:pr-8"
                  : "flex flex-col md:border-l md:border-rule md:pl-8 md:pr-8"
              }
            >
              <dt className="eyebrow order-2 mt-5 text-muted">{s.label}</dt>
              <dd
                className="tabular order-1 font-sans font-bold text-paper"
                style={{ fontSize: "clamp(2.75rem, 6vw, 5rem)", lineHeight: 0.9, letterSpacing: "-0.05em" }}
              >
                <PopNumber value={s.value} />
              </dd>
              <dd className="order-3 mt-2 text-[13px] leading-relaxed text-faint">{s.note}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
