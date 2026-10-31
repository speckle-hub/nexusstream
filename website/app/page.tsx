import { Navbar } from "@/components/sections/Navbar";
import { Hero } from "@/components/sections/Hero";
import { Sources } from "@/components/sections/Sources";
import { Features } from "@/components/sections/Features";
import { Showcase } from "@/components/sections/Showcase";
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
