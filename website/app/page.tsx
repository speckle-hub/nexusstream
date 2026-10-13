import { Navbar } from "@/components/sections/Navbar";
import { Hero } from "@/components/sections/Hero";
import { EcosystemMarquee } from "@/components/sections/EcosystemMarquee";
import { Stats } from "@/components/sections/Stats";
import { Features } from "@/components/sections/Features";
import { Faq } from "@/components/sections/Faq";
import { DownloadCta } from "@/components/sections/DownloadCta";
import { Footer } from "@/components/sections/Footer";
import { GenAuto } from "@/components/ui/gen-reveal";

export default function Page() {
  return (
    <main className="relative min-h-screen bg-void">
      <GenAuto />
      <Navbar />
      <Hero />
      <EcosystemMarquee />
      <Stats />
      <Features />
      <Faq />
      <DownloadCta />
      <Footer />
    </main>
  );
}
