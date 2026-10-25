import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

const sans = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL ?? "https://website-zeta-one-92.vercel.app",
  ),
  title: "Moovio — Every stream. One app.",
  description:
    "Moovio Desktop for Windows — a premium native streaming client built with Kotlin + Compose Multiplatform and libmpv. Stremio add-ons, Cinemeta & AniList metadata, PIN-gated NSFW area, local library, and MSI/portable downloads.",
  keywords: [
    "Windows",
    "desktop",
    "streaming",
    "Stremio",
    "Compose Multiplatform",
    "libmpv",
    "Moovio",
    "media player",
  ],
  openGraph: {
    title: "Moovio Desktop — Every stream. One app.",
    description:
      "Premium Windows desktop streaming client. Native Compose Multiplatform UI, libmpv playback, Stremio add-ons, and cinematic dark design.",
    type: "website",
    siteName: "Moovio",
  },
  twitter: {
    card: "summary_large_image",
    title: "Moovio Desktop — Every stream. One app.",
    description:
      "Premium Windows desktop streaming client. Native Compose Multiplatform UI, libmpv playback, Stremio add-ons, and cinematic dark design.",
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#06060C",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={`${sans.variable} font-sans antialiased`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
