import type { MetadataRoute } from "next";

const base = process.env.NEXT_PUBLIC_SITE_URL ?? "https://website-zeta-one-92.vercel.app";

export default function sitemap(): MetadataRoute.Sitemap {
  return [{ url: base, lastModified: new Date(), changeFrequency: "weekly", priority: 1 }];
}
