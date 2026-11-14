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

## Components

| Component | Use |
| --- | --- |
| `GlassLayers(overlay) { content }` | Stacks floating glass over content. `content` (on the theme background) is recorded as the backdrop; glass in `overlay` blurs and refracts it. |
| `GlassSurface` | The base glass material. Over a backdrop it blurs (Android 12+) and refracts at the edges (Android 13+). Without one it draws a readable fallback fill. Nested inside another glass surface it draws a light inner layer. |
| `GlassCard` | Rounded glass container for grouped content. Optional `onClick` adds press scale, ripple and button semantics. |
| `GlassButton` | Capsule button with a label and optional icon. |
| `GlassIconButton` | Circular icon-only button. `contentDescription` is required. |
| `GlassNavigationBar` + `GlassNavigationBarItem` | Floating capsule navigation bar. Goes in the `GlassLayers` overlay. The selected tab sits on a small glass lens (`GlassIndicator`) that refracts the content behind the bar, with a heavier label (no color change). Items have the `Tab` role. |
| `GlassMiniPlayer` | Floating capsule mini player: artwork, title, artist, play/pause, optional next, and a thin progress line. Stateless. `progress` is a lambda read only while drawing, so position updates do not recompose. Tapping the player opens Now Playing (`onClick`). |
| `ArtworkFallback` | Neutral gradient for tracks without artwork. Never a colored placeholder. |
| `LocalBottomChromePadding` | Bottom space covered by floating chrome (bar plus system inset). Scrolling screens add it to their bottom content padding so content scrolls under the glass but ends above it. |
| `ScreenPlaceholder` | Temporary text-only body for screens that are not built yet. |

### Motion pattern

`Modifier.pressScale(interactionSource)` is the shared press effect: the control scales to
`AuralisTheme.motion.pressedScale` while pressed and springs back. The scale is applied in
`graphicsLayer` (draw phase), so it never recomposes the content. Blur is never animated.

### Glass rendering

Glass rendering comes from [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) by Kyant0
(Apache-2.0): the backdrop is blurred on Android 12+ and refracted through a lens at the edges
on Android 13+, with a specular rim and a soft shadow. Backdrop types never leave this module,
so the library can be replaced. The glass tint includes the theme scrim so text stays readable
over bright artwork, and below Android 12 the tint is close to opaque. Every glass surface has a
hairline `borderSubtle` edge, which keeps it visible in the light theme.

### Icons

`AuralisIcons` is the only icon source. Most icons are Material rounded icons; `Pause` and
`SkipNext` are drawn in the same rounded style because `material-icons-core` does not include
them.

### Gallery (debug builds)

```bash
adb shell am start -n com.ignitrondev.auralis/.debug.DesignSystemGalleryActivity
```

It shows the type scale, color tokens and every glass component over artwork-like content,
with a button that switches between the dark and light themes.
