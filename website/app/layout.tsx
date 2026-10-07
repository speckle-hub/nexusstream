import type { Metadata, Viewport } from "next";
import { Inter, Space_Grotesk } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

const inter = Inter({ subsets: ["latin"], variable: "--font-sans" });
const display = Space_Grotesk({ subsets: ["latin"], variable: "--font-display" });

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "https://nexusstream.vercel.app"),
  title: "NexusStream — Every stream. One app.",
  description:
    "NexusStream unifies Stremio add-ons, CloudStream extensions, Aniyomi and Mihon repos into one beautiful Android app — movies, TV, anime and manga, with a player and reader that feel purpose-built.",
  keywords: ["Android", "streaming", "Stremio", "CloudStream", "Aniyomi", "Mihon", "MangaDex", "open source"],
  openGraph: {
    title: "NexusStream — Every stream. One app.",
    description: "One beautiful Android app for movies, TV, anime and manga. Free and open source.",
    type: "website",
    siteName: "NexusStream",
  },
  twitter: {
    card: "summary_large_image",
    title: "NexusStream — Every stream. One app.",
    description: "One beautiful Android app for movies, TV, anime and manga. Free and open source.",
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#050508",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.variable} ${display.variable} font-sans antialiased`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
