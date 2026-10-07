import { ImageResponse } from "next/og";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export const alt = "NexusStream — Every stream. One app.";

export default function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          background: "#050508",
          position: "relative",
        }}
      >
        <div
          style={{
            position: "absolute",
            inset: 0,
            background:
              "radial-gradient(ellipse 70% 60% at 50% 30%, rgba(124,92,255,0.35), transparent 70%)",
          }}
        />
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            width: 120,
            height: 120,
            borderRadius: 32,
            background: "linear-gradient(135deg, #8F74FF, #7C5CFF 55%, #4F8DFF)",
            marginBottom: 40,
          }}
        >
          <svg viewBox="0 0 48 48" width="76" height="76">
            <path
              d="M16 33V15l9 10.5L34 15v18"
              stroke="white"
              strokeWidth="3.4"
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
            <circle cx="34" cy="33" r="2.6" fill="white" opacity="0.9" />
          </svg>
        </div>
        <div
          style={{
            display: "flex",
            fontSize: 76,
            fontWeight: 700,
            color: "#EDEDF2",
            letterSpacing: "-0.02em",
            marginBottom: 16,
          }}
        >
          Nexus
          <span
            style={{
              background: "linear-gradient(120deg, #B9A8FF, #7C5CFF 45%, #4F8DFF)",
              backgroundClip: "text",
              color: "transparent",
            }}
          >
            Stream
          </span>
        </div>
        <div style={{ fontSize: 34, color: "#8A8A9C" }}>Every stream. One app.</div>
        <div
          style={{
            display: "flex",
            marginTop: 48,
            fontSize: 22,
            color: "#B9A8FF",
            border: "1px solid rgba(124,92,255,0.4)",
            borderRadius: 999,
            padding: "10px 28px",
            background: "rgba(124,92,255,0.12)",
          }}
        >
          Free & open source · Android 7.0+
        </div>
      </div>
    ),
    size,
  );
}
