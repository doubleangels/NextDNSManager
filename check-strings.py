import re
import os

BASE = "app/src/main/res/values/strings.xml"
LOCALES = [
    "values-de", "values-es", "values-fi", "values-fr", "values-in",
    "values-it", "values-ja", "values-nl", "values-pl", "values-pt",
    "values-sv", "values-tr", "values-zh", "values-night",
]

def keys(path):
    with open(path, encoding="utf-8") as f:
        content = f.read()
    return set(re.findall(r'name="([^"]+)"', content))

base_keys = keys(BASE)
print(f"Base keys: {len(base_keys)}")

for loc in LOCALES:
    path = f"app/src/main/res/{loc}/strings.xml"
    if not os.path.exists(path):
        print(f"\n{loc}: FILE MISSING")
        continue
    loc_keys = keys(path)
    missing = sorted(base_keys - loc_keys)
    if missing:
        print(f"\n{loc}: missing {len(missing)}")
        for k in missing:
            print(f"  - {k}")
    else:
        print(f"{loc}: OK")
