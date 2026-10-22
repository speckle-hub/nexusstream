# Moovio — Website

The marketing/landing site for the [Moovio Android app](https://github.com/speckle-hub/nexusstream).

**Stack:** Next.js 15 (App Router) · TypeScript · Tailwind CSS v3 · framer-motion ·
hand-ported Aceternity-style components (see `components/ui/`) ·
[ThreeUI](https://github.com/MengTo/threeui) Nebula shader · design references via 21st.dev.

> The `three` / `@designcodeio/threeui` / `@heroui/react` packages are still listed in
> `package.json` but are no longer imported by any component — they can be dropped.

## Develop

```bash
npm install
npm run dev      # http://localhost:3000
npm run build    # production build (static, outputs .next)
```

## Deploy to Vercel

1. [vercel.com/new](https://vercel.com/new) → import the `speckle-hub/nexusstream` repo.
2. **Root Directory** → `website` (Edit → select the `website` folder).
3. Framework preset auto-detects **Next.js**. Deploy.
4. After the first deploy, set `NEXT_PUBLIC_SITE_URL` (Project Settings → Environment Variables)
   to your production URL (e.g. `https://moovio.vercel.app` or your custom domain) so the
   OpenGraph/sitemap/canonical URLs are absolute and correct, then redeploy.

Every push to `main` that touches `website/` then redeploys automatically.
