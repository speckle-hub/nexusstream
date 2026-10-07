# NexusStream — Website

The marketing/landing site for the [NexusStream Android app](https://github.com/speckle-hub/nexusstream).

**Stack:** Next.js 15 (App Router, static) · TypeScript · Tailwind CSS v3 · framer-motion ·
HeroUI · Aceternity-style components (hand-ported, see `components/ui/`) ·
[ThreeUI](https://github.com/MengTo/threeui) Nebula shader · design references via 21st.dev.

## Develop

```bash
npm install
npm run dev      # http://localhost:3000
npm run build    # production build (static, outputs .next)
```

## Deploy to Vercel

1. [vercel.com/new](https://vercel.com/new) → import the `speckle-hub/nexusstream` repo.
2. **Root Directory** → `website` (Edit → select the `website` folder).
3. Framework preset auto-detects **Next.js** — no env vars needed. Deploy.

Every push to `main` that touches `website/` then redeploys automatically.
