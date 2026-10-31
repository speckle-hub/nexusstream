import {
  ANDROID_APK_SIZE,
  ANDROID_VERSION,
  SFW_SOURCE_COUNT,
  TEST_COUNT,
} from "@/lib/utils";

const stats = [
  { value: ANDROID_VERSION, label: "Current Android release", accent: true },
  { value: TEST_COUNT, label: "Unit tests, all passing" },
  { value: ANDROID_APK_SIZE.replace(" MB", ""), unit: "MB", label: "Signed APK" },
  { value: SFW_SOURCE_COUNT, label: "Built-in playable sources" },
];

export function Stats() {
  return (
    <section aria-label="Project numbers" className="border-b border-rule">
      <div className="shell py-20 md:py-24">
        <p className="mono text-[12px] text-amber">00:27:40</p>
        <dl className="mt-10 grid grid-cols-2 gap-x-8 gap-y-10 md:grid-cols-4 md:gap-x-0">
          {stats.map((s, i) => (
            <div
              key={s.label}
              className={
                i === 0
                  ? "flex flex-col md:pr-8"
                  : "flex flex-col md:border-l md:border-rule md:px-8"
              }
            >
              <dd
                className={`mono order-1 text-[clamp(2.2rem,5vw,3.6rem)] leading-[0.95] tracking-[-0.03em] ${
                  s.accent ? "text-amber" : "text-paper"
                }`}
              >
                {s.value}
                {s.unit ? (
                  <span className="ml-1 text-[0.4em] tracking-normal text-muted">{s.unit}</span>
                ) : null}
              </dd>
              <dt className="order-2 mt-4 text-[13.5px] leading-[1.5] text-muted">
                {s.label}
              </dt>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
