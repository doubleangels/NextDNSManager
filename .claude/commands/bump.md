---
description: Read the current version, choose a major, minor or patch bump from the changes since the last release, and update versionCode and versionName
allowed-tools: Bash, PowerShell, Read, Edit, Write, Grep, Glob
argument-hint: "[major|minor|patch|x.y.z] [commit]"
---

Bump the app version for the next release. If I passed `major`, `minor` or `patch`, use that level. If I passed an exact version such as `5.6.1`, use it as the new `versionName` and skip the classification in step 2 (still show the commit list, and warn me if the version is not higher than the current one). Otherwise decide from the changes. Do not push. Only commit if I ask (or if I invoked this with the word "commit").

## 1. Read the current state

1. Read `versionCode` and `versionName` from `app/build.gradle` (the `defaultConfig` block).
2. Find the last release tag: `git tag -l 'v*' --sort=-v:refname | head -1`. CI creates `v<versionName>` when a release reaches `main`.
3. If `versionName` already differs from that tag's version (`git show <tag>:app/build.gradle`), this release has already been bumped. Say so, show both versions, and stop unless I asked for a different level; in that case re-bump from the tag's version and keep the existing new `versionCode`.

## 2. Decide the bump

List the commits since the tag with `git log <tag>..HEAD --no-merges --format='%h %s'`, and read the diff (`git diff --stat <tag>..HEAD`, plus the code for anything unclear) instead of trusting the subjects alone. Ignore merges, dependabot bumps, docs-only and CI-only changes when classifying.

- **major**: a change that removes or reworks something users rely on, such as removing a feature or setting, raising `minSdk`, dropping a flavor (gms or foss), or a redesign of the main screen. Never pick major on your own: propose it, and ask me.
- **minor**: at least one new user-facing feature or setting, such as a new screen, menu item, setting, or notification type.
- **patch**: only fixes, performance work, wording, translations or dependency updates.

New version: increment the chosen part of `versionName` and reset the parts after it (`5.5.25` → minor → `5.6.0`). Always increment `versionCode` by exactly 1; CI's `check-version` job fails a release whose `versionCode` is not higher than the last published one.

Show me the classified commit list (features / fixes / ignored), the level you chose and why, and the old and new versions before editing.

## 3. Apply

1. Update `versionCode` and `versionName` in `app/build.gradle`. Nothing else stores the version (the Sentry release name is derived from them); `grep -rn "<old versionName>"` outside `build/` to make sure.
2. Do not write release notes. CI writes the Play "What's new" text, its translations and `changelogs/<versionCode>.txt` from the commit history when the release is created (the `release-notes` job in `deploy.yml`, using `anthropics/claude-code-action`). Notes already saved as `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` are kept instead, so only add one by hand to override what CI would write. Without the `CLAUDE_CODE_OAUTH_TOKEN` secret, CI ships the committed `whatsnew/` files instead.

## 4. Verify

1. Run `./gradlew test -q` (use `gradlew.bat` from PowerShell) and confirm it passes.
2. Run `python .claude/scripts/check_translations.py --since <tag>` and report its total. If it finds problems, tell me to run `/translate` before releasing; do not fix them here.

## 5. Report

End with: the old and new `versionName`/`versionCode`, the level and the one-line reason for it, test and translation results, and the reminder that the `v<new versionName>` tag and GitHub release are created by CI when this reaches `main`.

Only if I asked to commit: stage `app/build.gradle` only, and commit with the message `Bump version to <new versionName>`. Never push, and never create tags.
