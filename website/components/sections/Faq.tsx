"use client";

import { Accordion, AccordionItem } from "@heroui/react";
import { TextGenerateEffect } from "../ui/text-generate-effect";

const faqs = [
  {
    q: "Is NexusStream free?",
    a: "Yes — free and open source. No accounts, no ads, no tracking, no paywalls. Download the APK from GitHub Releases and you're set.",
  },
  {
    q: "Where does the content come from?",
    a: "NexusStream is a player and organizer, not a content source. Metadata comes from free public APIs (TMDB, AniList, MangaDex, Jikan, Cinemeta) and subtitles from OpenSubtitles. Any streaming capability comes from third-party add-ons that you choose to install — the app ships with no stream providers preinstalled.",
  },
  {
    q: "How do I install it?",
    a: "Download app-release.apk from the latest GitHub release, tap it, and allow “Install unknown apps” for your file manager. Android 7.0 (SDK 24) or newer is required. Future updates can be applied in place — and the app can check for new releases itself.",
  },
  {
    q: "What are Stremio / CloudStream / Aniyomi / Mihon add-ons?",
    a: "They are four popular open extension ecosystems. NexusStream speaks all four: paste a Stremio manifest URL or a provider repository URL in the Add-on Manager, browse its extensions, and install what you want. Nothing is enabled without your say-so.",
  },
  {
    q: "Can it play and read offline?",
    a: "Yes. Videos download through Media3 (progressive and HLS, including encrypted playlists) and manga chapters download page-by-page. Both render offline-first from the Library — with pause/resume, mirror failover and a storage meter.",
  },
  {
    q: "Is there NSFW content handling?",
    a: "The NSFW area is fully separate from normal content and locked by default. You can require a PIN and/or biometrics, it auto-locks when the app is backgrounded, and incognito mode keeps it out of your watch history.",
  },
];

export function Faq() {
  return (
    <section id="faq" className="mx-auto max-w-3xl px-6 py-24 md:py-32">
      <div className="mb-12 text-center">
        <p className="mb-3 text-xs font-semibold uppercase tracking-[0.28em] text-accent-soft">FAQ</p>
        <h2 className="font-display text-3xl font-bold tracking-tight text-mist md:text-5xl">
          Good questions
        </h2>
        <TextGenerateEffect
          className="mx-auto mt-4 max-w-xl text-sm text-muted md:text-base"
          words="The honest answers, including the legal ones."
        />
      </div>

      <Accordion
        variant="splitted"
        className="gap-3"
        itemClasses={{
          base: "glass !bg-ink/60 !rounded-2xl !border-white/[0.07]",
          title: "font-display font-semibold text-mist text-[15px]",
          content: "text-muted text-sm leading-relaxed pb-5",
          trigger: "py-4 px-5",
        }}
      >
        {faqs.map((f) => (
          <AccordionItem key={f.q} title={f.q} aria-label={f.q}>
            {f.a}
          </AccordionItem>
        ))}
      </Accordion>
    </section>
  );
}
