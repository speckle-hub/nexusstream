import type { Metadata, Viewport } from "next";
import { Inter_Tight, Instrument_Serif } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

/* One grotesk carries UI and display; a high-contrast serif is held
   back for editorial moments only (headlines set at 36px and up). */
const sans = Inter_Tight({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

const serif = Instrument_Serif({
  subsets: ["latin"],
  weight: "400",
  style: ["normal", "italic"],
  variable: "--font-serif",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL ?? "https://website-zeta-one-92.vercel.app",
  ),
  title: "NexusStream — Every stream. One app.",
  description:
    "NexusStream unifies Stremio add-ons, CloudStream extensions, Aniyomi and Mihon repos into one beautiful Android app — movies, TV, anime and manga, with a player and reader that feel purpose-built.",
  keywords: [
    "Android",
    "streaming",
    "Stremio",
    "CloudStream",
    "Aniyomi",
    "Mihon",
    "MangaDex",
    "open source",
  ],
  openGraph: {
    title: "NexusStream — Every stream. One app.",
    description:
      "One beautiful Android app for movies, TV, anime and manga. Free and open source.",
    type: "website",
    siteName: "NexusStream",
  },
  twitter: {
    card: "summary_large_image",
    title: "NexusStream — Every stream. One app.",
    description:
      "One beautiful Android app for movies, TV, anime and manga. Free and open source.",
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#F2F1EC",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={`${sans.variable} ${serif.variable} font-sans antialiased`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
