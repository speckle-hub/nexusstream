import type { Metadata, Viewport } from "next";
import { IBM_Plex_Mono, Instrument_Sans } from "next/font/google";
import "./globals.css";

const sans = Instrument_Sans({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-sans",
  display: "swap",
});

const mono = IBM_Plex_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-mono",
  display: "swap",
});

const description =
  "Moovio is an Android player for movies, TV, anime and manga. It plays what your Stremio add-ons, CloudStream extensions and built-in sources return, with one library, offline downloads and a PIN-gated private area. Free, open source, no account.";

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL ?? "https://website-zeta-one-92.vercel.app",
  ),
  title: "Moovio — Every source. One player.",
  description,
  keywords: [
    "Android",
    "streaming",
    "media player",
    "Stremio",
    "CloudStream",
    "anime",
    "manga",
    "Moovio",
  ],
  openGraph: {
    title: "Moovio — Every source. One player.",
    description,
    type: "website",
    siteName: "Moovio",
  },
  twitter: {
    card: "summary_large_image",
    title: "Moovio — Every source. One player.",
    description,
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#0A0A09",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={`${sans.variable} ${mono.variable} font-sans antialiased`}>
        {children}
      </body>
    </html>
  );
}
