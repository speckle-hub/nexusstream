import { Navbar } from "@/components/sections/Navbar";
import { Hero } from "@/components/sections/Hero";
import { EcosystemMarquee } from "@/components/sections/EcosystemMarquee";
import { Features } from "@/components/sections/Features";
import { Showcase } from "@/components/sections/Showcase";
import { Stats } from "@/components/sections/Stats";
import { Faq } from "@/components/sections/Faq";
import { DownloadCta } from "@/components/sections/DownloadCta";
import { Footer } from "@/components/sections/Footer";

export default function Page() {
  return (
    <div className="relative min-h-screen bg-void text-paper antialiased">
      <Navbar />
      <main>
        <Hero />
        <EcosystemMarquee />
        <Features />
        <Showcase />
        <Stats />
        <Faq />
        <DownloadCta />
      </main>
      <Footer />
    </div>
  );
}
