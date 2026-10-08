"use client";

import { m } from "framer-motion";
import { Download, FileDown, Github, Smartphone } from "lucide-react";
import { APK_URL, GITHUB_URL, RELEASES_URL } from "@/lib/utils";
import { BackgroundBeams } from "../ui/background-beams";
import { MovingBorderButton } from "../ui/moving-border";
import { Sparkles } from "../ui/sparkles";

export function DownloadCta() {
  return (
    <section id="download" className="relative overflow-hidden py-28 md:py-40">
      <BackgroundBeams />
      <Sparkles density={50} color="#8F74FF" />
      <div className="absolute left-1/2 top-1/2 h-[380px] w-[680px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/15 blur-[130px]" />

      <div className="relative z-10 mx-auto max-w-3xl px-6 text-center">
        <m.h2
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.7 }}
          className="font-display text-4xl font-bold leading-tight tracking-tight text-mist md:text-6xl"
        >
          Ready when you are.
          <br />
          <span className="text-gradient">Free forever.</span>
        </m.h2>

        <m.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.7, delay: 0.12 }}
          className="mx-auto mt-5 max-w-xl text-sm text-muted md:text-base"
        >
          Grab the APK from GitHub Releases and install it like any other app.
          Android 7.0+ · 38 MB · updates install in place.
        </m.p>

        <m.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.7, delay: 0.24 }}
          className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row"
        >
          <MovingBorderButton href={APK_URL} containerClassName="shadow-glow-accent rounded-full">
            <FileDown className="h-4 w-4" />
            Download v1.4.1 APK
          </MovingBorderButton>
          <a
            href={RELEASES_URL}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2 rounded-full border border-white/15 px-8 py-3.5 font-semibold text-mist transition hover:border-white/35 hover:bg-white/5"
          >
            <Github className="h-4 w-4" />
            All releases
          </a>
        </m.div>

        <m.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, delay: 0.4 }}
          className="mt-12 inline-flex items-center gap-2.5 rounded-full border border-white/10 bg-white/[0.03] px-5 py-2.5 text-xs text-muted"
        >
          <Smartphone className="h-4 w-4 text-accent-soft" />
          No account. No ads. No tracking. Just your library.
        </m.div>
      </div>
    </section>
  );
}
