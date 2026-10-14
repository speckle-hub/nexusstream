import { Navbar } from "@/components/sections/Navbar";
import { Hero } from "@/components/sections/Hero";
import { Showcase } from "@/components/sections/Showcase";
import { Numbers } from "@/components/sections/Numbers";
import { Chapters } from "@/components/sections/Chapters";
import { Ecosystem } from "@/components/sections/Ecosystem";
import { Faq } from "@/components/sections/Faq";
import { DownloadCta } from "@/components/sections/DownloadCta";
import { Footer } from "@/components/sections/Footer";

export default function Page() {
  return (
    <div className="relative min-h-screen bg-paper">
      <Navbar />
      <main>
        <Hero />
        <Showcase />
        <Numbers />
        <Chapters />
        <Ecosystem />
        <Faq />
        <DownloadCta />
      </main>
      <Footer />
    </div>
  );
}
