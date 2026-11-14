# Releasing Auralis

Pushing a version tag that starts with `v` builds a signed release APK and creates or updates the matching GitHub Release. Tags containing a hyphen, such as `v2.0.0-beta.1`, are published as prereleases. The `Build and publish APK` workflow can also be run manually against an existing tag.

## Configure APK signing

The new release key for this checkout is in the ignored `.release/auralis-release.jks` file; its GitHub secret values are in the ignored, permission-restricted `.release/credentials.env` file. Keep a protected backup of the keystore. Android updates must keep using this same keystore and key alias. Do not commit or share either local file publicly.

For a different environment, create a release keystore and keep a protected backup:

```bash
keytool -genkeypair -v -keystore auralis-release.jks -alias auralis \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w 0 auralis-release.jks
```

Add these repository Actions secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The base64 output for `auralis-release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password entered when creating the key |
| `ANDROID_KEY_ALIAS` | `auralis` (or the alias you chose) |
| `ANDROID_KEY_PASSWORD` | Password for the key |

The workflow fails before publishing if any signing secret is missing. Never commit the keystore or its passwords.

## Build a tag

After the workflow and signing configuration are merged into the default branch and the signing secrets are configured, push a new tag such as `v2.0.0-beta.2`. The tag must point to a commit that includes the signing configuration. To build a compatible existing tag, open **Actions → Build and publish APK → Run workflow** and enter the tag name. The workflow verifies the APK signature before publishing, uses its run number as Android's `versionCode`, and uses the tag without the leading `v` as `versionName`.
