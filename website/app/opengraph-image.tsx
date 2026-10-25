import { ImageResponse } from "next/og";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export const alt = "Moovio — Every stream. One app.";

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
            width: 132,
            height: 132,
            borderRadius: 34,
            marginBottom: 40,
            position: "relative",
            overflow: "hidden",
            background: "linear-gradient(150deg, #232046 0%, #12121F 55%, #07070B 100%)",
            border: "1px solid rgba(255,255,255,0.14)",
          }}
        >
          <div
            style={{
              position: "absolute",
              width: 130,
              height: 130,
              left: -20,
              top: -28,
              borderRadius: 999,
              background: "radial-gradient(circle, rgba(139,92,246,0.40), transparent 70%)",
            }}
          />
          <svg viewBox="0 0 48 48" width={132} height={132}>
            <path
              fill="#F5F5FA"
              d="M30.23,12.73 34.57,12.75 34.57,31.59 30.21,31.59 30.19,17.65 29.60,19.73 25.57,31.41 22.17,31.48 17.98,19.73 17.16,16.52 17.23,31.45 13.55,31.48 13.52,13.12 19.62,13.17 23.46,24.33 24.05,26.65 24.63,24.42 28.52,13.17 30.19,13.10Z"
            />
          </svg>
          <div
            style={{
              position: "absolute",
              right: 14,
              bottom: 14,
              width: 38,
              height: 38,
              borderRadius: 999,
              background:
                "linear-gradient(135deg, #875FF6 0%, #4FA8F0 42%, #23CFEE 55%, #4FA8F0 68%, #875FF6 100%)",
            }}
          />
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
          Moovio
          <span
            style={{
              background: "linear-gradient(120deg, #B9A8FF, #7C5CFF 45%, #4F8DFF)",
              backgroundClip: "text",
              color: "transparent",
            }}
          >
            .
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
          Windows desktop · v0.1.0 · MSI + ZIP
        </div>
      </div>
    ),
    size,
  );
}
