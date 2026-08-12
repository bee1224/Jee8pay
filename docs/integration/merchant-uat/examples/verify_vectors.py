#!/usr/bin/env python3
import hashlib
import json
from pathlib import Path


def canonicalize(payload, secret):
    values = {
        key: value
        for key, value in payload.items()
        if key != "sign" and value is not None and value != ""
    }
    preimage = "".join(
        f"{key}={values[key]}&" for key in sorted(values, key=str.casefold)
    )
    return preimage + "key=" + secret


def verify(path, payload_key):
    vector = json.loads(path.read_text(encoding="utf-8"))
    canonical = canonicalize(vector[payload_key], vector["secret"])
    signature = hashlib.md5(canonical.encode("utf-8")).hexdigest().upper()
    assert canonical == vector["canonical"], f"{path.name}: canonical mismatch"
    assert signature == vector["expectedSign"], f"{path.name}: signature mismatch"
    assert signature == vector[payload_key]["sign"], f"{path.name}: payload sign mismatch"
    print(f"{path.name}=PASS sign={signature}")


base = Path(__file__).resolve().parent
verify(base / "create-vector.json", "request")
verify(base / "notify-vector.json", "payload")
print("MERCHANT_UAT_VECTORS=PASS")
