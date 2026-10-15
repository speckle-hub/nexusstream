import type { Config } from "tailwindcss";

/**
 * Design system: "Dark Flagship".
 * Deep void, hairline rules, one violet signal + mint secondary.
 * Colour lives in glows and product plates, never in chrome.
 */
const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        void: {
          DEFAULT: "#06060C",
          raised: "#0B0B15",
          deep: "#030308",
        },
        paper: "#EDEDF2",
        muted: "#9A9AB0",
        faint: "#5C5C72",
        accent: {
          DEFAULT: "#7C5CFF",
          soft: "#BFAEFF",
          deep: "#5A3FDB",
        },
        signal: "#3DDC97",
        vermilion: {
          DEFAULT: "#D8452A",
          deep: "#B3351B",
        },
        elevated: "#151522",
        rule: "rgba(255,255,255,0.08)",
        "rule-soft": "rgba(255,255,255,0.05)",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        display: ["var(--font-sans)", "system-ui", "sans-serif"],
        serif: ["var(--font-serif)", "Georgia", "serif"],
      },
      animation: {
        float: "float 7s ease-in-out infinite",
        "float-slow": "float-slow 9s ease-in-out infinite",
        "pulse-soft": "pulse-soft 3.2s ease-in-out infinite",
      },
      keyframes: {
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-8px)" },
        },
        "float-slow": {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(8px)" },
        },
        "pulse-soft": {
          "0%, 100%": { opacity: "0.45" },
          "50%": { opacity: "1" },
        },
      },
      boxShadow: {
        lift: "0 1px 2px rgba(0,0,0,0.4), 0 12px 32px -18px rgba(0,0,0,0.7)",
        card: "0 24px 60px -24px rgba(0, 0, 0, 0.8)",
        object:
          "0 40px 80px -40px rgba(0,0,0,0.8), 0 8px 24px -12px rgba(0,0,0,0.5)",
        "glow-accent": "0 0 80px -20px rgba(124, 92, 255, 0.55)",
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
