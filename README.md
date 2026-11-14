<p align="center">
  <img src="docs/assets/auralis-app-icon.svg" alt="Auralis app icon" width="112" height="112">
</p>

<h1 align="center">Auralis</h1>

<p align="center"><strong>YouTube Music, with a native Android feel.</strong></p>

<p align="center">
  <a href="https://github.com/ignitrondev/Auralis/releases/download/v2.0.0-beta.2/Auralis-v2.0.0-beta.2.apk"><strong>Download beta APK</strong></a>
  ·
  <a href="https://github.com/ignitrondev/Auralis/releases/tag/v2.0.0-beta.2">Release details</a>
  ·
  <a href="https://github.com/ignitrondev/Auralis/issues/new/choose">Send feedback</a>
</p>

<p align="center">
  <a href="https://developer.android.com"><img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white"></a>
  <a href="https://developer.android.com/compose"><img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white"></a>
  <a href="LICENSE"><img alt="GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-555555?style=flat-square"></a>
</p>

Auralis is an independent YouTube Music client for Android. Explore music, search for songs and
artists, and listen through a native player wrapped in a calm, monochrome **Liquid Glass** interface.

> **Beta · v2.0.0-beta.2.** Try it out and tell us what works well and what needs attention. Auralis
> is still in active development, so you may run into bugs.

## The app

- **Home** — a place to discover music.
- **Search** — find songs, albums, artists, and playlists.
- **Library** — browse albums, recently played music, and liked tracks after signing in.
- **Player** — keep music playing in the background, control playback from the notification, and
  manage the queue.
- **Settings** — sign in or out and check for new releases.

The interface uses Auralis's monochrome Liquid Glass design across its Home, Search, Library, and
Settings screens, with a compact player that stays close while you browse.

## Download and install

Get the [Auralis v2.0.0-beta.2 APK](https://github.com/ignitrondev/Auralis/releases/download/v2.0.0-beta.2/Auralis-v2.0.0-beta.2.apk)
for Android 8.0 or newer. When prompted, allow your browser or file manager to install apps from this
source.

If you have a **debug build of Auralis** installed, remove it before installing this release. Android
does not allow a release APK to update a debug build because they use different signing keys.
Removing the debug build also removes its local app data. Future release APKs will update normally.

## Help test the beta

Try browsing, searching, signing in, playing music in the background, and using the queue. If you find
a problem, [open an issue](https://github.com/ignitrondev/Auralis/issues/new/choose) with your phone
model, Android version, and the steps that led to it. Please leave out account details and private
session information.

## Privacy

Auralis connects to YouTube Music and its media hosts. It does not ask for or store your Google
password. Your session is stored encrypted on your device and removed when you sign out. Auralis has
no analytics or third-party trackers. See the [security policy](SECURITY.md) to report a security
issue.

## About the project

Auralis is an unofficial, independent project and is not affiliated with YouTube or Google. This
repository previously hosted **IgniteTunes**, a Flutter app; Auralis is its native rebrand and
rewrite. The old [IgniteTunes releases](https://github.com/ignitrondev/Auralis/releases) remain
available and are not maintained.

Auralis is licensed under the [GNU General Public License v3.0](LICENSE). Third-party licenses are
listed in [NOTICE](NOTICE). The Auralis and IgnitronDev names and logo are brand assets and are not
covered by the source code license.

---

Copyright (c) 2026 IgnitronDev
