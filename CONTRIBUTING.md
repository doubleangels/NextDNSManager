# Contributing to NextDNS Manager

Thank you for helping! NextDNS Manager is a small Android app for managing NextDNS, and contributions of every size are welcome: typo fixes, translations, bug fixes and new features.

By taking part you agree to be respectful and constructive. NextDNS Manager is released under the [GNU General Public License v3.0](LICENSE), and your contributions will be too.

## Ways to help

- **Report a bug or suggest an idea** in a [GitHub issue](https://github.com/doubleangels/nextdnsmanager/issues). Please do not include your NextDNS profile ID, query logs or other personal information.
- **Improve translations.** Many are machine-written and benefit from native-speaker review.
- **Fix a bug or build a feature.** For anything larger than a small fix, open an issue first so we can agree on the approach before you spend time on it.
- **Report a security problem privately.** See [SECURITY.md](SECURITY.md).

## Privacy comes first

The NextDNS dashboard shows a user's DNS query log, which is effectively their browsing history. Changes that add network access, tracking, analytics, new permissions or new data collection need a clear justification and will get extra scrutiny. Error reporting stays opt-in, and nothing from the dashboard may be sent to Sentry.

## Setting up

Requirements: **JDK 21**, **Android Studio** (recent stable) with **Android SDK 37**, and an Android device or emulator on API 32 or newer.

```bash
git clone https://github.com/<your-fork>/nextdnsmanager.git
cd nextdnsmanager
./gradlew assembleGmsDebug      # build a debug APK (use assembleFossDebug for the F-Droid flavor)
./gradlew installGmsDebug       # build and install on a connected device or emulator
```

On Windows use `gradlew.bat`, or Git Bash. The app has two flavors: **gms** (Google Play, with Firebase push notifications) and **foss** (F-Droid, no Google services). Code that only one flavor needs lives in `app/src/gms` or `app/src/foss`.

You do not need a Sentry DSN to build, run or test the app. Debug builds only report errors when you configure a DSN: copy `sentry.dsn.local.example` to `sentry.dsn.local` (gitignored) and put your own project's DSN in it, or set `sentry.dsn` in `local.properties`.

## Workflow

1. **Fork** the repository and create a branch from **`dev`**. Day-to-day work happens on `dev`; `main` is used for releases.
2. **Keep the change focused.** One concern per pull request, matching the style of the surrounding code.
3. **Run the checks** before you push:

   ```bash
   ./gradlew test
   ./gradlew lintGmsRelease lintFossRelease
   ```

4. **Localize any user-visible text** (see below).
5. **Open a pull request against `dev`.** Describe what changed and why, and include screenshots for UI changes. CI runs the tests, Android Lint and release builds of both flavors on every pull request.

### Commit messages

Write short, descriptive messages in the imperative mood ("Fix the status icon on mobile data"), with a body explaining *why* when it is not obvious.

## Translations

User-facing strings live in `app/src/main/res/values*/strings.xml`. URLs and preference values that must not be translated live in `values/donottranslate.xml`. When you add or change a string:

- Add it to **every** locale folder: `de`, `es`, `fi`, `fr`, `in`, `it`, `ja`, `nl`, `pl`, `pt`, `sv`, `tr` and `zh`.
- Use `<plurals>` for anything that shows a count, since plural rules differ between languages.
- Never hard-code user-visible text in Java; use string resources.
- Run `python .claude/scripts/check_translations.py` to find missing strings, placeholder mismatches and untranslated text.

If you are not confident in a language, a machine translation is acceptable, as long as you say so in the pull request so a native speaker can review it.

## Store listing

The Google Play text and screenshots live in `fastlane/metadata/`. Release notes are written by CI from the commit history when a release is created; to write them yourself, add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.

## Questions

Open an issue and ask. No question is too small.
