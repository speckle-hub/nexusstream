import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const GITHUB_URL = "https://github.com/speckle-hub/nexusstream";
export const APK_URL =
  "https://github.com/speckle-hub/nexusstream/releases/latest/download/app-release.apk";
export const RELEASES_URL = "https://github.com/speckle-hub/nexusstream/releases";
