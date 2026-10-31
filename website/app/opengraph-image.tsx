import { ImageResponse } from "next/og";
import { ANDROID_VERSION } from "@/lib/utils";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export const alt = "Moovio — Every source. One player.";

export default function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: "#0A0A09",
          padding: 72,
          position: "relative",
        }}
      >
        <div
          style={{
            position: "absolute",
            right: -200,
            top: -260,
            width: 760,
            height: 760,
            borderRadius: 760,
            background:
              "radial-gradient(circle, rgba(223,164,92,0.15), rgba(223,164,92,0) 70%)",
          }}
        />

        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              width: 64,
              height: 64,
              borderRadius: 14,
              background: "#141412",
              border: "1px solid rgba(239,237,232,0.14)",
            }}
          >
            <svg viewBox="0 0 48 48" width={44} height={44}>
              <path
                fill="#EFEDE8"
                d="M30.23,12.73 34.57,12.75 34.57,31.59 30.21,31.59 30.19,17.65 29.60,19.73 25.57,31.41 22.17,31.48 17.98,19.73 17.16,16.52 17.23,31.45 13.55,31.48 13.52,13.12 19.62,13.17 23.46,24.33 24.05,26.65 24.63,24.42 28.52,13.17 30.19,13.10Z"
              />
            </svg>
          </div>
          <div style={{ display: "flex", fontSize: 34, color: "#EFEDE8" }}>Moovio</div>
        </div>

        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ width: 56, height: 1, background: "#DFA45C", marginBottom: 36 }} />
          <div
            style={{
              display: "flex",
              fontSize: 88,
              lineHeight: 1.05,
              letterSpacing: "-0.03em",
              color: "#EFEDE8",
            }}
          >
            Every source.
          </div>
          <div
            style={{
              display: "flex",
              fontSize: 88,
              lineHeight: 1.05,
              letterSpacing: "-0.03em",
              color: "#EFEDE8",
            }}
          >
            One player.
          </div>
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-end",
            borderTop: "1px solid rgba(239,237,232,0.10)",
            paddingTop: 24,
            fontSize: 24,
            color: "#8C887F",
          }}
        >
          <div style={{ display: "flex" }}>
            Stremio add-ons · CloudStream extensions · 14 built-in
          </div>
          <div style={{ display: "flex", color: "#DFA45C" }}>Android {ANDROID_VERSION}</div>
        </div>
      </div>
    ),
    size,
  );
}
