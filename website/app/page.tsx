import { Navbar } from "@/components/sections/Navbar";
import { Hero } from "@/components/sections/Hero";
import { EcosystemMarquee } from "@/components/sections/EcosystemMarquee";
import { Features } from "@/components/sections/Features";
import { Stats } from "@/components/sections/Stats";
import { Faq } from "@/components/sections/Faq";
import { DownloadCta } from "@/components/sections/DownloadCta";
import { Footer } from "@/components/sections/Footer";

export default function Page() {
  return (
    <main className="relative min-h-screen bg-void">
      <Navbar />
      <Hero />
      <EcosystemMarquee />
      <Features />
      <Stats />
      <Faq />
      <DownloadCta />
      <Footer />
    </main>
  );
}
