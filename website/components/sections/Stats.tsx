import { StatNumber } from "../ui/stat-number";

const stats = [
  { value: "216", label: "Unit tests, all green" },
  { value: "4", label: "Add-on ecosystems" },
  { value: "25k", label: "Lines of Kotlin" },
  { value: "2X", label: "Hold-for-2X player speed" },
];

export function Stats() {
  return (
    <section id="numbers" className="relative border-y border-white/[0.06] bg-ink/40">
      <div className="mx-auto grid max-w-6xl grid-cols-2 gap-y-10 px-6 py-14 md:grid-cols-4 md:py-16">
        {stats.map((s) => (
          <div key={s.label} className="text-center">
            <div className="font-display text-4xl font-bold tracking-tight text-mist md:text-5xl">
              <StatNumber value={s.value} />
            </div>
            <div className="mt-2 text-xs uppercase tracking-[0.18em] text-muted">{s.label}</div>
          </div>
        ))}
      </div>
    </section>
  );
}
