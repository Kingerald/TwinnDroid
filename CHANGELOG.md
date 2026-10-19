# Changelog

All notable changes to TwinnDroid are listed here. The newest version is at the top.
Each version's APK is on the [Releases](../../releases) page.

## 1.1.3

- **Warns before cloning apps that won't run.** TwinnDroid now scans an app for commercial
  app-shielding (Promon SHIELD, and other RASP/anti-tamper products) and Play Integrity, and
  shows a clear warning in the editor. Re-signed clones of shielded apps — such as Clash Royale
  and Clash of Clans — crash on launch, and no no-root cloner can get past that.
- **Icon customization is now its own screen**, with the icon preview pinned at the top so it
  stays visible while you change the badge, colour, tint, brightness and the rest.
- **Softer light theme.** The light background is no longer glaring white, cards stand out from
  it, and the animated backgrounds show through properly.

## 1.1.2

- **New logo:** the *Split droid* — one droid head split in two, the clone half lifting away. It
  replaces the old icon on all 12 app icons, the themed icon, the home header and the README.
- **New cloning animation:** the droid splits apart as your app is copied, then becomes two whole
  droids when the clone is ready.

## 1.1.1

- **Reworked backgrounds**, inspired by [Morphe Patcher](https://github.com/MorpheApp/morphe-manager):
  Space is now a warp starfield, and Circles, Mesh, Shapes, Snow and Lava lamp were redone. Aurora
  became soft glowing curtains.
- **New backgrounds:** Waves, Fireflies, Bokeh and Meteors, plus a **Preview background** button.
- **Customisable home header:** Gradient, Glass, Solid or None, with opacity, real frosted blur,
  roundness and a floating style, plus optional frosted pills behind the icon, title and buttons.
- **5 new presets** (Lavender, Cyberpunk, Nord, Dracula, Matcha), each with a matching app icon,
  plus a **Match app icon** option.
- Background & effects and Home header moved into their own menus.

## 1.1.0

- **Animated backgrounds**, theme presets, custom colours, Material You, glass cards and text size.
- **Bottom tab bar** with a swipeable pager: Clones, New clone, Extract.
- **Extract APKs:** save any installed app, splits included, as `.apk`, `.apks`, `.xapk`, `.apkm`,
  or one merged `.apk`.
- **Clone icon adjustments:** hue, saturation, brightness, contrast, tint, invert, flip, rotate.
- **Animated cloning screen** that shows each step.
- **Backup & restore** of clones, signing key, sources and settings.
- Release signing with your own key, and R8-minified release builds (~8 MB instead of ~26 MB).
- Fixed the Discord clone hanging on launch (the resource-table package is now renamed too).

## 1.0.0

- First release. Clone installed apps (including split APKs) or `.apk` / `.apks` / `.xapk` /
  `.apkm` files into independently installed copies, each with its own login.
- Custom name, icon (original, number badge, or your own image) and permission removal.
- Update tracking with one-tap updates that keep the clone's data.
- Optional prompt-free installs with Shizuku.
