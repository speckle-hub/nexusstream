import type { Config } from "tailwindcss";

/**
 * Projection booth.
 *
 * One matte-black ramp, three text roles, one amber accent for the beam. No glow shadows,
 * no gradient utilities, no looping animations: a booth is dark and still until the film runs.
 */
const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    container: { center: true, padding: "1.5rem" },
    extend: {
      colors: {
        ink: {
          DEFAULT: "#0A0A09",
          raised: "#101010",
          deep: "#060606",
        },
        paper: "#EFEDE8",
        muted: "#8C887F",
        faint: "#605C55",
        amber: {
          DEFAULT: "#DFA45C",
          soft: "#EFC391",
          deep: "#A9742F",
        },
        rule: "rgba(239, 237, 232, 0.10)",
        "rule-soft": "rgba(239, 237, 232, 0.06)",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "ui-monospace", "monospace"],
      },
      maxWidth: {
        shell: "1180px",
      },
    },
  },
  plugins: [],
};

export default config;
