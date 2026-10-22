"""Report translation gaps across every locale of the app's string resources.

Checks each values-<locale>/ copy of strings.xml and arrays.xml against values/:
missing and extra names, values identical to English (likely untranslated),
format placeholder mismatches, string-array item counts, plural quantities,
and hardcoded text in layouts and the manifest. Exits 1 if anything needs fixing.

Usage: python .claude/scripts/check_translations.py [--since <git ref>]
  --since lists which strings changed since that ref, so the summary can say
  what this release added.
"""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
FILES = ("strings.xml",)
LOCALES = ("de", "es", "fi", "fr", "in", "it", "ja", "nl", "pl", "pt", "sv", "tr", "zh")
FORMAT = re.compile(r"%(\d+\$)?[sdf]")
UNESCAPED_APOSTROPHE = re.compile(r"(?<!\\)'")

# Plural categories each language needs (CLDR). Locales not listed need {"one", "other"}.
QUANTITIES = {
    "pl": {"one", "few", "many", "other"},
    "es": {"one", "many", "other"}, "fr": {"one", "many", "other"},
    "it": {"one", "many", "other"}, "pt": {"one", "many", "other"},
    "in": {"other"}, "ja": {"other"}, "zh": {"other"},
}

# Values that are legitimately the same as English in a locale: brand names, codec and
# format names, symbols. Add to this when a check flags something that really is correct.
TOKEN = r"(NextDNS|NextDNS Manager|DNS|DoH|DoT|DoQ|HTTPS|Git[Hh]ub|Sentry|Tally|Redact|Stripe|OK|%(\d+\$)?[sdf])"
SAME_AS_ENGLISH_OK = re.compile(
    rf"^(https?://\S+|@string/\S+|[\s\d%$.,:×x/+()·\-–…]*({TOKEN}[\s\d%$.,:×x/+()·\-–…]*)*)$"
)

# Words that a reviewer confirmed are correct as-is in that language (e.g. German "Video").
# Add "locale:name" here after checking, so the review list only shows new suspects.
REVIEWED_SAME_AS_ENGLISH = set()


def text(e):
    return "".join(e.itertext()).strip()


def load(path):
    """name -> (kind, value): value is a str, a list of array items, or a dict of plural items."""
    out = {}
    if not path.exists():
        return out
    for e in ET.parse(path).getroot():
        name = e.get("name")
        if e.get("translatable") == "false":
            out[name] = ("untranslatable", None)
        elif e.tag == "string":
            out[name] = ("string", text(e))
        elif e.tag == "string-array":
            out[name] = ("array", [text(i) for i in e.iter("item")])
        elif e.tag == "plurals":
            out[name] = ("plurals", {i.get("quantity"): text(i) for i in e.iter("item")})
    return out


def placeholders(s):
    return sorted({m.group().replace("%1$", "%") for m in FORMAT.finditer(s)})


def values_of(kind, v):
    if kind == "string":
        return [v]
    if kind == "array":
        return list(v)
    if kind == "plurals":
        return list(v.values())
    return []


def changed_since(ref):
    """Names added or changed in the default locale since a git ref."""
    names = set()
    for f in FILES:
        diff = subprocess.run(["git", "diff", ref, "--", f"app/src/main/res/values/{f}"],
                              cwd=ROOT, capture_output=True, text=True, encoding="utf-8").stdout
        names |= set(re.findall(r'^\+\s*<(?:string|string-array|plurals) name="([^"]+)"', diff, re.M))
    return names


def hardcoded_text():
    hits = []
    attr = re.compile(r'android:(text|hint|contentDescription|label|title)="([^"@?][^"]*)"')
    for p in list((RES / "layout").glob("*.xml")) + list((RES / "menu").glob("*.xml")) + [ROOT / "app/src/main/AndroidManifest.xml"]:
        if not p.exists():
            continue
        for n, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
            for m in attr.finditer(line):
                hits.append(f"{p.relative_to(ROOT)}:{n} android:{m.group(1)}=\"{m.group(2)}\"")
    return hits


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    since = sys.argv[sys.argv.index("--since") + 1] if "--since" in sys.argv else None
    base = {}
    for f in FILES:
        base.update(load(RES / "values" / f))
    translatable = {k: v for k, v in base.items() if v[0] != "untranslatable"}

    problems = {}  # locale -> list of str
    warnings = {}  # locale -> list of str (same-as-English values to review)
    for loc in LOCALES:
        have = {}
        for f in FILES:
            have.update(load(RES / f"values-{loc}" / f))
        p, w = [], []
        for name, (kind, v) in translatable.items():
            if kind == "array" and all(i.startswith("@string/") for i in v):
                # Items resolve per locale, so no copy is needed; just make sure each one exists.
                if name in have and have[name][1] != v:
                    p.append(f"{name} differs from the default's @string list; delete the locale copy")
                for ref in v:
                    if ref[len("@string/"):] not in have:
                        p.append(f"{name} item {ref} is not translated")
                continue
            if name not in have:
                p.append(f"missing {kind} {name}")
                continue
            lkind, lv = have[name]
            if lkind != kind:
                p.append(f"{name} is a {lkind}, default is a {kind}")
                continue
            if kind == "array" and len(lv) != len(v):
                p.append(f"{name} has {len(lv)} items, default has {len(v)}")
            if kind == "plurals":
                need = QUANTITIES.get(loc, {"one", "other"})
                if set(lv) != need:
                    p.append(f"{name} has quantities {sorted(lv)}, needs {sorted(need)}")
                if placeholders(lv.get("other", "")) != placeholders(v.get("other", "")):
                    p.append(f"{name} 'other' placeholders {placeholders(lv.get('other', ''))} != {placeholders(v.get('other', ''))}")
            if kind == "string" and placeholders(lv) != placeholders(v):
                p.append(f"{name} placeholders {placeholders(lv)} != {placeholders(v)}")
            for tr in values_of(kind, lv):
                # aapt fails the whole build on a bare apostrophe unless the value is in double quotes.
                if UNESCAPED_APOSTROPHE.search(tr) and not (tr.startswith('"') and tr.endswith('"')):
                    p.append(f"{name} has an unescaped apostrophe (write \\'): {tr[:60]}")
            for en, tr in zip(values_of(kind, v), values_of(kind, lv)):
                if en == tr and not SAME_AS_ENGLISH_OK.match(en) and f"{loc}:{name}" not in REVIEWED_SAME_AS_ENGLISH:
                    w.append(f"{name} is still English: \"{en}\"")
        for name in have:
            if name not in base:
                p.append(f"extra {name} (not in default locale)")
        problems[loc], warnings[loc] = p, w

    print(f"Default locale: {len(translatable)} translatable resources "
          f"({sum(1 for v in translatable.values() if v[0] == 'string')} strings, "
          f"{sum(1 for v in translatable.values() if v[0] == 'array')} arrays, "
          f"{sum(1 for v in translatable.values() if v[0] == 'plurals')} plurals)\n")
    print(f"{'Locale':<8} {'Problems':>8} {'Same as EN':>11}")
    for loc in LOCALES:
        print(f"{loc:<8} {len(problems[loc]):>8} {len(warnings[loc]):>11}")

    for loc in LOCALES:
        if problems[loc] or warnings[loc]:
            print(f"\n[{loc}]")
            for x in problems[loc]:
                print(f"  ERROR {x}")
            for x in warnings[loc]:
                print(f"  CHECK {x}")

    hard = hardcoded_text()
    if hard:
        print("\nHardcoded text in layouts/menus/manifest (should be @string resources):")
        for h in hard:
            print(f"  {h}")

    if since:
        names = sorted(changed_since(since))
        print(f"\nAdded or changed in values/ since {since}: {len(names)}")
        for n in names:
            missing = [loc for loc in LOCALES if any(n in x.split() for x in problems[loc])]
            print(f"  {n}" + (f"  (problems in {', '.join(missing)})" if missing else ""))

    total = sum(len(v) for v in problems.values()) + len(hard)
    print(f"\nTotal: {total} problems, {sum(len(v) for v in warnings.values())} same-as-English values to review")
    sys.exit(1 if total else 0)


if __name__ == "__main__":
    main()
