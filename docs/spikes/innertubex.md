# Spike: InnerTubeX (spec section 6.2)

Date: 2026-11-14. Library: InnerTubeX `v0.11.7` (JitPack), QuackJS `0.1.4`.
Test: `./gradlew :provider:youtube:test -Plive` (`LiveSpikeTest`). Live tests never run in the
normal test task.

## Checklist (spec 6.2)

| Check | Result |
| --- | --- |
| README, CHANGELOG, SECURITY read; latest version | ✅ `v0.11.7` (2026-11-13). 0.x: breaking changes possible, pin exact versions. |
| Licenses of the library and its dependencies | ✅ InnerTubeX GPL-3.0, QuackJS GPL-3.0, Ktor and kotlinx Apache-2.0. All compatible with Auralis (GPL-3.0). |
| minSdk and Kotlin compatibility | ✅ minSdk 23 (Auralis 26), Kotlin 2.4.21 (same), compileSdk 37. |
| Network hosts | ✅ Only YouTube hosts when the remote player-config store is not passed (Auralis passes none). `reddit.com` appears only as an embed referer string. The optional config store downloads from GitHub (`MetrolistGroup/faraday`, or a host-chosen URL). |
| Logging | ✅ Injectable `InnerTubeLogger`, default `NONE`. Library `toString()` redacts tokens, signed URLs and session fields. |
| Signed-out search | ✅ |
| Signed-out browse: home, album, artist, playlist | ✅ |
| One track playing (direct / HLS / SABR) | ❌ No playable stream (see below). |
| Sign-in flow | Not tested yet. |

## Catalog: works signed out

`search` and `browse` (`FEmusic_home`, album `MPREb_…`, artist `UC…`, playlist `VL…`) return
full responses with the `WEB_REMIX` client. Scrubbed responses are stored in
`provider/youtube/src/test/resources/fixtures/` for parser tests. Scrubbing removes
`responseContext`, `visitorData` and tracking parameters.

InnerTubeX response models are thin (`JsonObject` fields), so Auralis parses the renderers
itself and maps them to domain models inside `:provider:youtube`.

InnerTubeX requires the caller's `HttpClient` to install `ContentNegotiation` (JSON) and to
disable redirects (the library follows them itself).

## Playback: blocked without a PO token or a session

`/player` for a public song (`dX3k_QDnzHE`, signed out, home network):

| Client | Status |
| --- | --- |
| WEB_REMIX, WEB, IOS, TVHTML5 | `LOGIN_REQUIRED` "Please sign in" (YouTube bot check) |
| WEB_EMBEDDED_PLAYER | `ERROR` "This video is unavailable" |

`InnerTubeExtractor.extract` fails with `NO_PLAYABLE_STREAM` in direct, HLS and SABR modes.
There is also a second, separate problem: the current player script (`fad7e4e9`) cannot be
compiled by QuackJS ("Template interpolation is unsupported"), so web clients cannot solve the
`n`/signature cipher. Enabling the remote player-config store (GitHub) did not change the result.

Metrolist, the reference consumer, plays music with:

- a WebView BotGuard **PO-token provider** (`TokenProvider`, `WEB_BOTGUARD`)
- direct URLs only (`allowSabr = false`, `allowHls = false`), played by ExoPlayer
- a remote player-config store hosted on GitHub

## Options for playback

1. **PO-token provider (WebView BotGuard)**: keeps signed-out playback, as the spec requires.
   Runs Google's own attestation script in a hidden WebView; traffic stays on Google/YouTube
   hosts. Largest effort. Web clients still also need the cipher fix from upstream.
2. **Sign-in first**: an authenticated YouTube Music session may clear `LOGIN_REQUIRED`.
   Moves the account flow (spec 12.1) into Phase 4, and playback would need an account.
3. **Wait for upstream**: re-run the spike on new InnerTubeX/QuackJS releases. The catalog
   work (Phase 3) does not depend on playback and can continue.

The decision is recorded in the PR that follows this spike.
