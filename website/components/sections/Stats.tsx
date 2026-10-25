import { PopNumber } from "../ui/pop-number";
import { Stagger } from "../ui/reveal";

const stats = [
  { value: "0.1.0", label: "Desktop version", note: "Windows release" },
  { value: "180", label: "MB", note: "ZIP / MSI installer" },
  { value: "24+", label: "Tests", note: "all green" },
  { value: "0", label: "Electron", note: "pure native Compose" },
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
                className="tabular order-1 font-display font-bold text-paper"
                style={{
                  fontSize: "clamp(3rem, 7vw, 5.5rem)",
                  lineHeight: 0.9,
                  letterSpacing: "-0.05em",
                }}
              >
                <PopNumber value={s.value} />
              </dd>
              <dd className="order-3 mt-2 text-[13px] leading-relaxed text-faint">
                {s.note}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
