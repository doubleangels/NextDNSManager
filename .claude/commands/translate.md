---
description: Check every string in all 13 locales, translate anything missing or left in English, and show a summary
allowed-tools: Bash, PowerShell, Read, Edit, Write, Grep, Glob
---

Make sure every user-visible string is translated into all 13 locales (`de es fi fr in it ja nl pl pt sv tr zh`), fix any gaps, then show me a summary. Do not push. Only commit if I ask (or if I invoked this with the word "commit").

## 1. Check

1. Find the last release tag: `git tag -l 'v*' --sort=-v:refname | head -1`.
2. Run `python .claude/scripts/check_translations.py --since <that tag>`. It compares `values-<locale>/strings.xml` with `values/` and reports:
   - **ERROR**: missing or extra names, placeholder mismatches, wrong plural quantities for the language, wrong string-array item counts, unescaped apostrophes, and hardcoded `android:text`/`hint`/`contentDescription`/`label`/`title` in layouts, menus or the manifest.
   - **CHECK**: values identical to English, which are probably untranslated.
   - the strings added or changed in `values/` since the tag.
   It exits 1 when there is any ERROR. Strings in `values/donottranslate.xml` (URLs, preference values) are not checked and must stay untranslated.
3. Also look for user-visible text that is not a resource at all: string literals passed to `setText`, `Toast.makeText`, `Snackbar.make`, `setTitle`, `setMessage`, `setContentTitle`/`setContentText` or `setContentDescription` in `app/src/main/java`, `app/src/gms/java` and `app/src/foss/java`. Log tags, intent extras, preference keys, Sentry messages and exception messages that are never shown are fine. The notification channel name in `CustomFirebaseMessagingService` is user-visible (system settings).
4. If there are no ERRORs, no CHECKs and no hardcoded text, skip to step 3 (Verify).

## 2. Fix

- **Missing strings, arrays or plurals**: translate from the English in `values/` into each locale, and insert the entry at the same position as in `values/strings.xml` (usually the end). Match the tone and terms already used in that locale's file: read its neighbouring strings first, and reuse the same words for "Private DNS", "app lock", "settings", "dashboard" and so on. Keep "NextDNS" untranslated.
- **Hardcoded text**: move it into `values/strings.xml` under a name that follows the file's existing naming, reference it, then translate it everywhere as above.
- **Placeholders**: every `%1$s`, `%2$d`, … in the English must appear in the translation; reorder them for the language's grammar but never drop or renumber them.
- **Plurals**: give each locale exactly the quantities its language needs (`pl`: one few many other; `es fr it pt`: one many other; `in ja zh`: other; the rest: one other). This matches `QUANTITIES` in the checker.
- **String arrays** whose items are all `@string/…` references need no locale copy, because the references resolve per locale. Translate the referenced strings instead. Arrays with literal items need a full copy per locale.
- **CHECK (same as English)**: translate it unless the English really is correct in that language (a loanword such as Dutch "Home" or "Feedback", a unit, or a brand name). When it is correct, add `"<locale>:<name>"` to `REVIEWED_SAME_AS_ENGLISH` in `.claude/scripts/check_translations.py`, so it is not flagged again.
- **Escaping**: `&` as `&amp;`, `'` as `\'`, `"` as `\"`, `<` as `&lt;`; a leading `@` or `?` needs a backslash. Save files as UTF-8.
- Never add `translatable="false"` to user-visible text.

## 3. Verify

1. Re-run the checker. The goal is `Total: 0 problems, 0 same-as-English values to review`.
2. Run `./gradlew lintGmsRelease -q` to catch resource errors (for example a bad escape) that only show up when aapt compiles the strings.
3. If any string used in a layout or menu got noticeably longer (for example in German or Finnish), say which screen may need a visual check.

## 4. Summary

End with:

- a table with one row per locale: problems found, fixed, and same-as-English values reviewed,
- the strings added since the last tag, and whether each is now present in all 13 locales,
- every string you translated, grouped by name, with the English and a short back-translation for anything you were unsure of,
- anything that needs a native speaker or a human decision.

Only if I asked to commit: stage only the resource files (and the checker script, if you edited its allowlist), then commit with a message like "Translate <what> into all locales". Never push.
