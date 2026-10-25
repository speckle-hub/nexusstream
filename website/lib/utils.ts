import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const GITHUB_URL = "https://github.com/speckle-hub/nexusstream";
export const APK_URL =
  "https://github.com/speckle-hub/nexusstream/releases/latest/download/app-release.apk";
export const RELEASES_URL = "https://github.com/speckle-hub/nexusstream/releases";
// Windows desktop release assets (v0.1.1)
export const DESKTOP_ZIP_URL =
  "https://github.com/speckle-hub/nexusstream/releases/download/v0.1.1/Moovio-0.1.1-windows-x64.zip";
export const DESKTOP_MSI_URL =
  "https://github.com/speckle-hub/nexusstream/releases/download/v0.1.1/Moovio-0.1.1.msi";
