import type { Config } from "tailwindcss";

/**
 * Design system: "Editorial Gallery".
 *
 * The page is warm paper and near-black ink. Colour is rationed — it comes
 * from the product itself (the device, the artwork), never from the chrome.
 * `vermilion` is the only brand accent and appears on well under 1% of the
 * surface: active markers, focus rings, one live dot.
 */
const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        /* ── Page ─────────────────────────────────────────── */
        paper: {
          DEFAULT: "#F2F1EC",
          raised: "#FFFFFF",
          deep: "#E7E6E0",
        },
        /* ── Text on paper ────────────────────────────────── */
        graphite: "#3A3A40",
        ash: "#6E6E76",
        /* ── Hairlines ────────────────────────────────────── */
        rule: "rgba(12,12,14,0.16)",
        "rule-soft": "rgba(12,12,14,0.08)",
        /* ── The single accent ────────────────────────────── */
        vermilion: {
          DEFAULT: "#D8452A",
          deep: "#B3351B",
        },
        /* ── Ink sections + the product (kept from v1 so the
               device mockup renders unchanged) ─────────────── */
        void: "#06060C",
        ink: "#0B0B15",
        elevated: "#151522",
        mist: "#EDEDF2",
        muted: "#9A9AB0",
        accent: {
          DEFAULT: "#7C5CFF",
          soft: "#BFAEFF",
          deep: "#5A3FDB",
        },
        signal: "#3DDC97",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        display: ["var(--font-sans)", "system-ui", "sans-serif"],
        serif: ["var(--font-serif)", "Georgia", "Times New Roman", "serif"],
      },
      letterSpacing: {
        /* Apple-design §15: tracking is size-specific. */
        editorial: "0.16em",
        display: "-0.045em",
        tight: "-0.025em",
      },
      animation: {
        "scroll-x": "scroll-x var(--animation-duration,40s) var(--animation-direction,forwards) linear infinite",
        "pulse-soft": "pulse-soft 3.2s ease-in-out infinite",
        float: "float 7s ease-in-out infinite",
        "float-slow": "float-slow 9s ease-in-out infinite",
      },
      keyframes: {
        "scroll-x": {
          to: { transform: "translateX(calc(-50% - 0.5rem))" },
        },
        "pulse-soft": {
          "0%, 100%": { opacity: "0.45" },
          "50%": { opacity: "1" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-8px)" },
        },
        "float-slow": {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(8px)" },
        },
      },
      boxShadow: {
        /* Bigger surfaces read as thicker (apple-design §12). */
        lift: "0 1px 2px rgba(12,12,14,0.05), 0 12px 32px -18px rgba(12,12,14,0.28)",
        "lift-lg": "0 2px 4px rgba(12,12,14,0.05), 0 32px 70px -32px rgba(12,12,14,0.38)",
        object: "0 40px 80px -40px rgba(12,12,14,0.55), 0 8px 24px -12px rgba(12,12,14,0.3)",
        card: "0 24px 60px -24px rgba(0, 0, 0, 0.8)",
        "glow-soft": "0 0 120px -20px rgba(124, 92, 255, 0.35)",
      },
      maxWidth: {
        editorial: "78rem",
      },
    },
  },
  darkMode: "class",
  plugins: [],
};

export default config;
