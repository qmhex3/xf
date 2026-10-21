#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/build/codecpack-signing"
KEY="$OUT/codecpack-signing-private.pem"
PUB="$OUT/codecpack-signing-public.pem"
SRC="$ROOT/codecpack/src/main/java/com/frameout/codecpack/CodecPackVerifier.kt"
mkdir -p "$OUT"
if [[ -e "$KEY" || -e "$PUB" ]]; then
    printf '%s\n' "Refusing to overwrite existing signing material at $OUT" >&2
    exit 1
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$KEY"
openssl rsa -pubout -in "$KEY" -out "$PUB" >/dev/null 2>&1
python3 - "$PUB" "$SRC" <<'PY'
from pathlib import Path
import re
import sys
pub = Path(sys.argv[1]).read_text(encoding="utf-8").rstrip()
path = Path(sys.argv[2])
text = path.read_text(encoding="utf-8")
block = 'private const val PUBLIC_KEY_PEM = """' + pub + '"""'
updated, count = re.subn(r'private const val PUBLIC_KEY_PEM = """.*?"""', block, text, count=1, flags=re.S)
if count != 1:
    raise SystemExit("PUBLIC_KEY_PEM block was not found")
path.write_text(updated, encoding="utf-8")
PY
chmod 600 "$KEY"
printf '%s\n' "Private key: $KEY"
printf '%s\n' "Public key: $PUB"
printf '%s\n' "The private key must be stored as the CODECPACK_SIGNING_KEY_PEM GitHub Secret."
printf '%s\n' "The public key is embedded in CodecPackVerifier.kt and is safe to commit."
