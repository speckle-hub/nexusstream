# Gson data classes are populated by reflection — keep every field.
-keep class com.novastream.app.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Torrent streaming engine (TorrentStream-Android over jlibtorrent).
# JNI bridges and the TorrentListener callbacks are resolved by name at runtime, so they must
# survive shrinking. Required if isMinifyEnabled is turned on in app/build.gradle.kts.
-keep class com.github.se_bastiaan.torrentstream.** { *; }
-keep class com.frostwire.jlibtorrent.** { *; }
-dontwarn com.frostwire.jlibtorrent.**
