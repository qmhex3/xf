import re
import sys
from pathlib import Path

feature = Path("core/src/main/java/com/frameout/core/pro/ProFeature.kt").read_text()
strings = Path("core/src/main/java/com/frameout/core/pro/ProStrings.kt").read_text()
keys = re.findall(r'^\s+(\w+)\("', feature, re.M)
missing = [k for k in keys if f"ProFeature.{k} ->" not in strings]
if missing:
    print("FAIL: ProStrings.featureLine misses", missing)
    sys.exit(1)
print("OK: every ProFeature has a feature line")
