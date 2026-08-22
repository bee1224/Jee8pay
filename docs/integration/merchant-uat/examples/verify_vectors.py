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


def verify_unified_order_response(path):
    response = json.loads(path.read_text(encoding="utf-8"))
    assert response["code"] == 0, f"{path.name}: code mismatch"
    assert response["msg"] == "SUCCESS", f"{path.name}: msg mismatch"
    assert response["sign"], f"{path.name}: outer response sign missing"

    data = response["data"]
    assert data["payOrderId"], f"{path.name}: payOrderId missing"
    assert data["mchOrderNo"], f"{path.name}: mchOrderNo missing"
    assert data["orderState"] == 1, f"{path.name}: orderState mismatch"
    assert data["payDataType"] == "ryoIbon", f"{path.name}: payDataType mismatch"
    assert isinstance(data["payData"], str), f"{path.name}: payData must be a JSON string"

    payment_instruction = json.loads(data["payData"])
    assert payment_instruction["ibonCode"], f"{path.name}: ibonCode missing"
    assert payment_instruction["paymentCode"], f"{path.name}: paymentCode missing"
    assert payment_instruction["expireDate"], f"{path.name}: expireDate missing"
    assert payment_instruction["billAmount"] == 40, f"{path.name}: billAmount mismatch"
    assert payment_instruction["paymentCode"] == (
        payment_instruction["ibonShopId"] + payment_instruction["ibonCode"]
    ), f"{path.name}: composed paymentCode mismatch"
    assert "shortUrl" not in payment_instruction, f"{path.name}: fixture must omit shortUrl"
    print(
        f"{path.name}=PASS payData=JSON_STRING doubleParse=PASS "
        "shortUrlOptional=PASS"
    )


base = Path(__file__).resolve().parent
verify(base / "create-vector.json", "request")
verify(base / "notify-vector.json", "payload")
verify_unified_order_response(base / "unified-order-success.json")
print("MERCHANT_UAT_VECTORS=PASS")
