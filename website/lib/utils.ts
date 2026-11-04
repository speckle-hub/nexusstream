/**
 * The GitHub slug every link on the site is built from.
 *
 * Renaming the repository (`speckle-hub/nexusstream` → `moovio`, roadmap §7 item 0) becomes a
 * one-line change here. The app carries its own copy in `UpdateChecker.DEFAULT_REPO`; move both
 * together, and only after the rename is live on GitHub — otherwise the in-app updater and this
 * CTA point at a 404.
 */
const REPO = "speckle-hub/nexusstream";

export const GITHUB_URL = `https://github.com/${REPO}`;
export const APK_URL = `${GITHUB_URL}/releases/latest/download/app-release.apk`;
export const RELEASES_URL = `${GITHUB_URL}/releases`;

/** Windows companion build (unchanged by the Android release). */
export const DESKTOP_ZIP_URL = `${GITHUB_URL}/releases/download/v0.1.1/Moovio-0.1.1-windows-x64.zip`;
export const DESKTOP_MSI_URL = `${GITHUB_URL}/releases/download/v0.1.1/Moovio-0.1.1.msi`;

/**
 * Facts quoted across the page. Kept here so the version can never drift between the hero,
 * the numbers row and the download block again (it had three different values).
 */
export const ANDROID_VERSION = "1.14.0";
export const ANDROID_APK_SIZE = "26.7 MB";
export const ANDROID_MIN_SDK = "Android 7.0";
export const TEST_COUNT = "370";
export const SFW_SOURCE_COUNT = "14";
export const DESKTOP_VERSION = "0.1.1";
