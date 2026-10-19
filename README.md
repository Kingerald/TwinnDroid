<div align="center">

<img src="docs/logo.svg" width="128" alt="TwinnDroid logo: a droid head split in two" />

# TwinnDroid

**Run two (or more) copies of the same Android app, each with its own login.**

No root. No virtual space. Each copy is a real, separately installed app.

[Download](../../releases/latest) · [Changelog](CHANGELOG.md) · [Support on Ko-fi](https://ko-fi.com/vvanadium)

</div>

---

## ✨ What can it do?

- 👯 **Clone any app**: pick an installed app, or open an `.apk` / `.apks` / `.xapk` / `.apkm` file
- ✏️ **Rename it**: give every copy its own name, like "Discord (Alt)"
- 🎨 **Change its icon**: keep the original, add a coloured number badge, or use your own picture, then fine-tune hue, saturation, brightness, contrast, tint, invert, flip and rotation with a live preview
- 🔒 **Remove permissions**: e.g. stop a clone from using your location or contacts
- 🔄 **Keep clones updated**: when the original app updates, TwinnDroid tells you, and one tap updates the copy *without logging you out*
- 📦 **Extract APKs**: save any installed app, splits included, as `.apk`, `.apks`, `.xapk`, `.apkm`, or one merged `.apk`
- ⚡ **Optional prompt-free installs** with [Shizuku](https://shizuku.rikka.app/)
- 🌈 **Make TwinnDroid yours**: 14 animated backgrounds, 12 theme presets (each with a matching app icon), your own colours (hex or colour picker), Material You, a customisable frosted-glass header, glass cards, text size, and 12 app icons (or your own picture)
- 💾 **Backup & restore**: move your clones, signing key and settings to a new phone or a fresh install

## 📱 Requirements

- Android 8.0 or newer (tested on Android 16, Samsung Galaxy S25 Ultra)
- No root needed

## 🚀 Getting started

1. **Download** the latest `TwinnDroid.apk` from the [Releases](../../releases) page.
2. **Open it** on your phone. If Android asks, allow your browser or file manager to *install unknown apps*.
3. **Open TwinnDroid** and go to the **New clone** tab at the bottom (or swipe left).
4. Pick the app you want to copy, or tap **From an APK file**.
5. Change the name, icon or permissions if you like, then tap **Clone & install**.
6. Confirm the install prompt. Your new copy appears in the app drawer. Log in with a different account and you're done!

> 💡 The first time you install a clone, Android asks you to allow TwinnDroid to install apps. Tap **Allow** on the banner in TwinnDroid, or in the prompt.

## 🔄 Updating clones

TwinnDroid checks every few hours whether the original app was updated. When it was:

- You get a notification, and the clone shows **Update available**.
- Tap **Update**. The clone is rebuilt from the new version and installed over the old one. **Your login and data stay.**

With Shizuku and **Settings → Auto-update in background** turned on, this happens on its own.

## ⚡ Shizuku (optional, still no root)

Shizuku lets TwinnDroid install clones silently, without an "Install?" prompt each time.

1. Install **Shizuku** from the [Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub](https://github.com/RikkaApps/Shizuku/releases).
2. Turn on **Developer options → Wireless debugging**, then follow Shizuku's pairing guide.
3. Start Shizuku. In TwinnDroid go to **Settings → Authorize TwinnDroid**.

Shizuku needs to be started again after each phone restart.

## 📦 Extracting APKs

Open the **Extract** tab, tap any app and choose a format:

| Format | Use it for |
|---|---|
| **APK** | Apps that are a single file |
| **Merged** | Apps with split APKs, combined into one `.apk` that installs anywhere. It's re-signed, so it installs as a separate copy and can't update the original. |
| **APKS** | The original split set, installable with SAI or TwinnDroid |
| **XAPK** | APKPure-style bundle with app info and icon |
| **APKM** | APKMirror-style bundle (unencrypted) |

Every format except Merged keeps the app's original signature.

## 🎨 Appearance

Tap the 🎨 palette button on the Clones tab:

| Setting | What it does |
|---|---|
| **Background & effects** | Space, Particles, Circles, Rings, Mesh, Shapes, Snow, Grid, Lava lamp, Aurora, Waves, Fireflies, Bokeh, Meteors, None or Random, plus tilt **Parallax**. Tap **Preview background** to see it without the interface. It pauses when TwinnDroid is in the background and turns off when Android's "Remove animations" is on. Inspired by [Morphe Patcher](https://github.com/MorpheApp/morphe-manager). |
| **Home header** | Gradient, Glass, Solid or None, with opacity, real background blur (Android 12+), roundness and a floating style. Optional frosted pills behind the icon, title and buttons, each with their own opacity, blur and roundness. |
| **Pure black** | True-black dark mode for OLED screens |
| **Glass cards / Outlines** | Translucent cards and thin edges |
| **Presets** | TwinnDroid, Midnight AMOLED, Ocean, Forest, Sunset, Rose, High contrast, Lavender, Cyberpunk, Nord, Dracula, Matcha. Turn on **Match app icon** to switch TwinnDroid's icon with the theme. |
| **System / Light / Dark** | Follow your phone, or force a mode |
| **Material You** | Use colours from your wallpaper (Android 12+) |
| **Custom colours** | Change accent, secondary, tertiary, background, cards and header gradient with a colour picker or hex code |
| **Text size** | 85% – 140% |
| **App icon** | 12 built-in icons, or add a home-screen shortcut with your own picture |

## 💾 Back up TwinnDroid!

TwinnDroid signs every clone with its own key. **If you uninstall TwinnDroid, that key is gone**, and you won't be able to update existing clones; you'd have to uninstall and recreate them.

👉 **Settings → Backup & restore → Back up** saves your clone list, signing key, imported APKs, icons and settings to one file. After reinstalling (or on a new phone), use **Restore**.

> **Updating from 1.0.0?** 1.1.0 is signed with a new release key, so Android needs you to uninstall 1.0.0 first. Before you do, use **Settings → Signing key → Export** in 1.0.0. Then install 1.1.0 and use **Import**, so your existing clones keep updating.

## ❓ Which apps work?

Most apps work fine, such as chat apps, social apps, note apps and games without strict anti-cheat. Tested with various social media apps, multiple tools apps, notes, and shopping apps.

Some apps protect themselves and **won't work as clones**. TwinnDroid detects the common cases and **warns you in the editor before you build**, so you don't waste time on a clone that just crashes:

- 🛡️ **App-shielding / anti-tamper** (Promon SHIELD and similar). These verify the app hasn't been modified and crash a re-signed clone on launch. **Supercell games like Clash Royale and Clash of Clans use this.** No no-root cloner can get past it; only a work-profile app like Shelter or Island, which runs the *unmodified* app, can.
- 🏦 Banking and payment apps (same anti-tamper reason).
- 🔐 Apps that use Google Play Integrity, or whose *Sign in with Google* is tied to the original app. The clone is signed with a different key, so sign-in may refuse even if the app opens. (This is about the app's own signature check — not about Google Play Services being installed, so MicroG does not help.)

Other things to know:

- 🔔 **Push notifications** may not arrive in clones (they're registered to the original app).
- 📎 Some apps may crash when **sharing files** from a clone.

This is the same for other cloning apps too.

## 🛠️ Troubleshooting

| Problem | Fix |
|---|---|
| "App not installed" / conflict error | A clone with that package already exists but was signed with a different key. Uninstall the old clone, then build again. |
| Clone crashes or closes instantly | The app is probably shielded (anti-tamper) — the editor shows a red warning for these, and they can't run as a clone. See [Which apps work?](#-which-apps-work) |
| Clone stuck on the loading screen | Rebuild it (**⋮ → Rebuild & reinstall**). If it still hangs, open an issue with the app name and version. |
| Shizuku shows "not running" | Start Shizuku again (needed after every restart), then reopen TwinnDroid. |
| Icon change didn't show up | The new icon applies when you leave TwinnDroid. Give your launcher a few seconds, and re-add TwinnDroid to your home screen from the app drawer if needed. |

---

## 👩‍💻 For developers

<details>
<summary><b>How cloning works</b></summary>

```
source APKs (base + splits)
  └─► ARSCLib: edit the binary AndroidManifest + resources.arsc of every APK
        • package name → new name (manifest and resource table)
        • relative class names (.Foo) → absolute
        • <provider android:authorities>, custom <permission>s and their references → unique
        • taskAffinity rewritten; sharedUserId + instrumentation removed
        • base APK: label, icon (new drawable resource), removed permissions, debuggable
  └─► strip old signatures / source stamp
  └─► apksig: sign v2 + v3 (+ v1 if minSdk < 24), 16 KB page-aligned
  └─► PackageInstaller session, or Shizuku `pm install-*` (all splits at once)
```

The clone is a normal app with a new package name, so Android gives it its own data folder, and that's what keeps logins separate. LSPatch isn't needed: it keeps the original package name, so it can't create side-by-side copies on its own.

</details>

<details>
<summary><b>Building from source</b></summary>

Requirements: JDK 17+, Android SDK (platform 36, build-tools 36.0.0).

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Release builds (R8-minified) are signed with your own key. Put a `keystore.properties` in the project root or in `~/.twinndroid-signing/` (never commit it):

```properties
storeFile=/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

or set `TWINN_KEYSTORE_FILE`, `TWINN_KEYSTORE_PASSWORD`, `TWINN_KEY_ALIAS` and `TWINN_KEY_PASSWORD`. Without a key, release builds fall back to the debug key.

```bash
./gradlew assembleRelease
```

Test the clone engine on your PC against any APK set (outputs go to `app/build/clone-test/`):

```bash
TWINN_TEST_APKS=base.apk,split_config.arm64_v8a.apk ./gradlew :app:testDebugUnitTest
```

Project layout:

| Path | What |
|---|---|
| `engine/` | `ManifestPatcher`, `CloneEngine` (patch + sign), `ApkExporter` (extract/merge), `ApkSources`, `IconFactory`, `SigningKeys` |
| `install/` | `SessionInstaller` (PackageInstaller), `ShizukuInstaller` + `ShellService` |
| `ui/` | Compose screens and the tab pager, `theme/` (presets, palette → Material 3 scheme, `AnimatedBackground`), `AppIcons` |
| `data/` | Clone configs, settings, `BackupManager` |
| `UpdateWorker.kt` | Periodic update check / auto-update |

</details>

## ☕ Support

TwinnDroid is free and open source. If it's useful to you, you can support its development on Ko-fi:

<a href="https://ko-fi.com/vvanadium"><img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="Support TwinnDroid on Ko-fi" /></a>

## 🤝 Contributing

Bug reports and pull requests are welcome! When reporting a clone that doesn't work, please include the **app name, app version, and your Android version**.

## 📄 License

TwinnDroid is open source under the [MIT License](LICENSE).

It's built on these open-source projects:

- [ARSCLib](https://github.com/REAndroid/ARSCLib) (Apache 2.0): binary manifest and resource editing. `app/libs/arsclib-1.4.0-android.jar` is the Maven Central release with its bundled `android.*` / `org.xmlpull.*` stub classes removed.
- [apksig](https://android.googlesource.com/platform/tools/apksig/) (Apache 2.0): APK signing
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API) (Apache 2.0)
- [Bouncy Castle](https://www.bouncycastle.org/) (MIT)
- Jetpack Compose, Material 3, WorkManager (Apache 2.0)

The background animations and appearance settings are inspired by [Morphe Patcher](https://github.com/MorpheApp/morphe-manager)'s design. TwinnDroid's implementations are its own, and no Morphe code is included.

> TwinnDroid is not affiliated with any of the apps you clone. Use clones in line with each app's terms of service.
