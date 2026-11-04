import { Navbar } from "@/components/sections/Navbar";
import { Hero } from "@/components/sections/Hero";
import { Sources } from "@/components/sections/Sources";
import { Bento } from "@/components/sections/Bento";
import { ScrollStory } from "@/components/sections/ScrollStory";
import { Stats } from "@/components/sections/Stats";
import { Faq } from "@/components/sections/Faq";
import { DownloadCta } from "@/components/sections/DownloadCta";
import { Footer } from "@/components/sections/Footer";

export default function Page() {
  return (
    <div className="min-h-screen bg-ink text-paper antialiased">
      <Navbar />
      <main>
        <Hero />
        <Sources />
        <Bento />
        <ScrollStory />
        <Stats />
        <Faq />
        <DownloadCta />
      </main>
      <Footer />
    </div>
  );
}
