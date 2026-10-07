import type { Config } from "tailwindcss";
import { heroui } from "@heroui/react";

const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./node_modules/@heroui/theme/dist/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        void: "#050508",
        ink: "#0A0A12",
        elevated: "#14141C",
        accent: {
          DEFAULT: "#7C5CFF",
          soft: "#B9A8FF",
          deep: "#5A3FDB",
        },
        mist: "#EDEDF2",
        muted: "#8A8A9C",
      },
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "system-ui", "sans-serif"],
      },
      animation: {
        spotlight: "spotlight 2s ease .75s 1 forwards",
        "scroll-x": "scroll-x var(--animation-duration,40s) var(--animation-direction,forwards) linear infinite",
        float: "float 7s ease-in-out infinite",
        "float-slow": "float 11s ease-in-out infinite",
        shimmer: "shimmer 2.4s linear infinite",
        "spin-slow": "spin 9s linear infinite",
        "pulse-soft": "pulse-soft 3.2s ease-in-out infinite",
      },
      keyframes: {
        spotlight: {
          "0%": { opacity: "0", transform: "translate(-72%,-62%) scale(0.5)" },
          "100%": { opacity: "1", transform: "translate(-50%,-40%) scale(1)" },
        },
        "scroll-x": {
          to: { transform: "translateX(calc(-50% - 0.5rem))" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0px)" },
          "50%": { transform: "translateY(-16px)" },
        },
        shimmer: {
          from: { backgroundPosition: "200% 0" },
          to: { backgroundPosition: "-200% 0" },
        },
        "pulse-soft": {
          "0%, 100%": { opacity: "0.55" },
          "50%": { opacity: "1" },
        },
      },
      backgroundImage: {
        "grid-white": "linear-gradient(to right, rgb(255 255 255 / 0.045) 1px, transparent 1px), linear-gradient(to bottom, rgb(255 255 255 / 0.045) 1px, transparent 1px)",
      },
      boxShadow: {
        "glow-accent": "0 0 60px -12px rgba(124, 92, 255, 0.55)",
        "glow-soft": "0 0 120px -20px rgba(124, 92, 255, 0.35)",
        card: "0 24px 60px -24px rgba(0, 0, 0, 0.8)",
      },
    },
  },
  darkMode: "class",
  plugins: [heroui()],
};

export default config;
