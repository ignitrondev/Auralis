# Auralis for Windows

The desktop client keeps Auralis's dark glass visual style and uses the existing JVM YouTube Music
provider for Home, search, artwork, and stream resolution. Playback has a desktop-specific JavaFX
audio adapter; the Android Media3 service is not used here.

Run from the repository root:

```sh
./gradlew :desktopApp:run
```

Build the Windows installer on Windows with JDK 17 or newer:

```powershell
./gradlew :desktopApp:packageMsi
```

The release workflow builds the same installer for every version tag and attaches it to the GitHub
release as `Auralis-<tag>.msi`. The installer is not signed, so Windows shows a SmartScreen warning.
`packageReleaseMsi` does not work yet: it shrinks the code with ProGuard, which needs keep rules.

This first desktop version does not include account sign-in. YouTube may refuse signed-out playback,
and a stream whose audio codec is unsupported by JavaFX Media will show an error in the player.
