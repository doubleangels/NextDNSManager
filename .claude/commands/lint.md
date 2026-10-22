---
description: Run Android Lint on both flavors, list every issue from the reports, fix each one, and verify lint and the tests are clean
allowed-tools: Bash, PowerShell, Read, Edit, Write, Grep, Glob
---

Run lint on the app, list every issue it reports, fix each one, then prove lint is clean. Do not push. Only commit if I ask (or if I invoked this with the word "commit").

## 1. Run lint and list every issue

1. Run `./gradlew lintGmsRelease lintFossRelease -q` (use `gradlew.bat` from PowerShell). These are the tasks CI runs. They write `app/build/reports/lint-results-gmsRelease.*` and `lint-results-fossRelease.*` (`.html`, `.xml`, `.txt`, `.sarif`). A failing exit code means lint found errors; warnings alone still exit 0.
2. Build the list from both XML reports, merging issues that appear in both flavors (same id, file and line). For example:

   ```python
   import xml.etree.ElementTree as ET
   seen = set()
   for flavor in ("gmsRelease", "fossRelease"):
       for i in ET.parse(f"app/build/reports/lint-results-{flavor}.xml").getroot().iter("issue"):
           loc = i.find("location")
           key = (i.get("id"), loc.get("file"), loc.get("line"))
           if key not in seen:
               seen.add(key)
               print(i.get("severity"), *key, i.get("message"))
   ```

   Sort by severity (errors first), then by issue id, then by file. Show me the full numbered list, grouped by issue id, before changing anything, and say which flavor each comes from when it is only one. Say how many there are.
3. If there are no issues, say so and stop.

## 2. Fix each issue

Fix every issue. Prefer a real fix over a suppression. Read the surrounding code first; never fix from the message alone.

- **ObsoleteSdkInt**: delete the unneeded `SDK_INT` check (`minSdkVersion` is in `app/build.gradle`) and any import that becomes unused.
- **InlinedApi / NewApi**: add a correct SDK guard if one is missing; if the guard is correct but lint cannot follow it, add `@SuppressLint` on the smallest method with a comment saying why.
- **MissingClass** in a manifest: code that exists in only one flavor belongs in that flavor's manifest (`app/src/gms/AndroidManifest.xml`), not in `main` with a `tools:node="remove"` in the other flavor.
- **NotifyDataSetChanged**: use a specific change event or `DiffUtil` when that is correct; otherwise `@SuppressLint("NotifyDataSetChanged")` on the method that calls it, with a reason.
- **UnusedResources**: remove the resource from the default file **and every locale** (`values-*`). Confirm nothing in `src/` (all flavors) still uses it.
- **SetJavaScriptEnabled**: the NextDNS dashboard needs JavaScript; keep the suppression on the WebView setup method, and make sure the WebView still only stays on NextDNS hosts (`ExternalLinkHandler.isNextDnsHost`).
- **Strings**: any string edit goes into all 13 locales (`de es fi fr in it ja nl pl pt sv tr zh`); escape `&` as `&amp;` and `'` as `\'`.
- **Typos** on text that is intentionally as written (for example a product name): add `tools:ignore` on that element, and `xmlns:tools` on the file's root if it is missing.
- If lint still reports an issue after a source-level suppression, do not keep stacking annotations; scope an `<ignore path="...">` for that one issue and file in `app/lint.xml` (create it if needed) with a comment explaining why.
- A suppression must always carry a short comment explaining why the code is safe. Never turn a check off globally or lower its severity to get a clean report.

Do not touch files I have uncommitted changes in unless an issue is in that file; check `git status` first and tell me if one is. Keep every change behavior-preserving, and call out anything where lint points at a possible real bug instead of hiding it (for example, a WebView setting, an exported component, or an API guard that may be wrong at runtime).

## 3. Verify

1. Re-run lint and re-list the issues from the new XML reports. The goal is **0 issues**. If any remain, fix them and repeat; if one cannot be fixed without a behavior change, stop and ask me.
2. Run `./gradlew test -q` and `./gradlew assembleGmsRelease assembleFossRelease -q`, and fix anything the lint changes broke.
3. If any layout changed, install the debug build on an emulator (never a physical device), open the affected screen, take a screenshot and look at it. Skip this step if no layout or visual resource changed.

## 4. Report

Finish with a short report:

- how many issues there were and how many remain (should be 0),
- the fixes grouped by issue id, one line each, separating real fixes from intentional suppressions (with the reason),
- anything that deserves a human decision or device testing,
- test and build results, and any uncommitted changes that were not mine.

Only if I asked to commit: stage just the files this task changed (leave my other uncommitted files out), and commit with a message that lists the fixes. Never push.
