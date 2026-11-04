import {
  ANDROID_APK_SIZE,
  ANDROID_VERSION,
  SFW_SOURCE_COUNT,
  TEST_COUNT,
} from "@/lib/utils";
import { Reveal } from "@/components/motion/Reveal";
import { SlidingNumber } from "@/components/motion/SlidingNumber";

const numeric = [
  { value: Number(TEST_COUNT), label: "Unit tests, all passing", decimals: 0 },
  { value: Number(ANDROID_APK_SIZE.replace(" MB", "")), label: "Signed APK", decimals: 1, unit: "MB" },
  { value: Number(SFW_SOURCE_COUNT), label: "Built-in playable sources", decimals: 0 },
];

export function Stats() {
  return (
    <section aria-label="Project numbers" className="border-b border-rule">
      <div className="shell py-20 md:py-24">
        <Reveal>
          <p className="marker mono max-w-[240px] text-[11.5px] text-amber">00:27:40</p>
        </Reveal>

        <dl className="mt-10 grid grid-cols-2 gap-x-8 gap-y-10 md:grid-cols-4 md:gap-x-0">
          <Reveal className="md:pr-8">
            <dd className="mono text-[clamp(2.1rem,4.6vw,3.4rem)] leading-[0.95] tracking-[-0.03em] text-amber">
              {ANDROID_VERSION}
            </dd>
            <dt className="mt-4 text-[13px] leading-[1.5] text-muted">Current Android release</dt>
          </Reveal>

          {numeric.map((stat, index) => (
            <Reveal
              key={stat.label}
              delay={70 + index * 60}
              className="md:border-l md:border-rule md:px-8"
            >
              <dd className="mono text-[clamp(2.1rem,4.6vw,3.4rem)] leading-[0.95] tracking-[-0.03em] text-paper">
                <SlidingNumber value={stat.value} decimals={stat.decimals} />
                {stat.unit ? (
                  <span className="ml-1 text-[0.4em] tracking-normal text-muted">{stat.unit}</span>
                ) : null}
              </dd>
              <dt className="mt-4 text-[13px] leading-[1.5] text-muted">{stat.label}</dt>
            </Reveal>
          ))}
        </dl>
      </div>
    </section>
  );
}
