# Auralis design system (`:core:designsystem`)

The visual foundation for Auralis: monochrome "Liquid Glass" (spec section 8). Feature modules
use only what this module exposes. They never define colors, fonts, sizes, corner radii or
animation specs themselves.

## Theme

Wrap UI in `AuralisTheme`. Dark is the default; `ThemeMode.Light` and `ThemeMode.System` are
available for the future appearance setting.

```kotlin
AuralisTheme(themeMode = ThemeMode.Dark) {
    Text("Auralis", style = AuralisTheme.typography.wordmark, color = AuralisTheme.colors.contentPrimary)
}
```

`AuralisTheme` also installs a Material 3 theme built from the tokens, so Material components
stay neutral. Dynamic (wallpaper) color is never used.

## Tokens

| Token set | Access | Notes |
| --- | --- | --- |
| Colors | `AuralisTheme.colors` | Named by role (`contentPrimary`, `surfaceGlass`, `borderSubtle`, `scrim`, …). Dark and light values, black, white and grays only. Album artwork is the only color in the app. |
| Typography | `AuralisTheme.typography` | Space Grotesk for display (`wordmark`, `screenTitle`, `nowPlayingTitle`, `sectionTitle`), Inter for everything else. `timestamp` uses tabular digits. |
| Shapes | `AuralisTheme.shapes` | `small`, `medium`, `large`, `extraLarge`, `capsule`. Everything is rounded. |
| Spacing | `AuralisTheme.spacing` | 4dp grid from `xxs` (2dp) to `xxxl` (48dp), plus `screenGutter`. |
| Motion | `AuralisTheme.motion` | `standard()`, `emphasized()`, `press()` springs, `fade()`, `enter` / `exit`, `pressedScale`. Respects the system "Remove animations" setting. |

### Color rules

- No accent color. State is shown by shape and fill (filled vs outline icon, selection pill), never by color.
- Errors use an icon plus text, not red.
- Text over artwork sits on a `scrim` and must keep at least 4.5:1 contrast (checked by `ColorContrastTest`).

### Fonts

Both fonts are bundled variable fonts (`res/font`) under the SIL Open Font License. Downloadable
Fonts are not used because they need Google Play Services. Non-Latin scripts fall back to the
system font. The licenses are in `src/main/assets/licenses/` and listed in the root `NOTICE`.
