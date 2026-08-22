#!/usr/bin/env python3
import hashlib
import json
import os
import time
import urllib.error
import urllib.request


BASE_URL = os.environ.get("UAT_INTERNAL_BASE_URL", "http://127.0.0.1:19216")
MCH_NO = os.environ["UAT_MERCHANT_ID"]
APP_ID = os.environ["UAT_APP_ID"]
SECRET = os.environ["UAT_APP_SECRET"]
WAY_CODE = os.environ.get("UAT_WAY_CODE", "RYO_IBON")
PROVIDER_PREFIXES = {
    "RYO_IBON": "RYO",
    "JAY_IBON": "JAY",
    "CHI_IBON": "CHI",
}
if WAY_CODE not in PROVIDER_PREFIXES:
    raise SystemExit("UAT_WAY_CODE must be RYO_IBON, JAY_IBON, or CHI_IBON")
PROVIDER_PREFIX = PROVIDER_PREFIXES[WAY_CODE]
EXISTING_MCH_ORDER_NO = "UAT-TALEND-RYO-20260821-092912-F8DAD9"
EXISTING_PAY_ORDER_ID = "P2090733029679960065"


def wire_value(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return str(value)


def sign(payload):
    values = {
        key: value
        for key, value in payload.items()
        if key != "sign" and value is not None and value != ""
    }
    canonical = "".join(
        f"{key}={wire_value(values[key])}&"
        for key in sorted(values, key=str.casefold)
    )
    return hashlib.md5((canonical + "key=" + SECRET).encode("utf-8")).hexdigest().upper()


def request(path, payload):
    body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    req = urllib.request.Request(
        BASE_URL + path,
        data=body,
        headers={"Content-Type": "application/json; charset=UTF-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            raw = response.read().decode("utf-8")
            return response.status, json.loads(raw)
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8")
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"raw": raw[:200]}
        return error.code, parsed


def auth_payload(**fields):
    payload = {
        "version": "1.0",
        "signType": "MD5",
        "reqTime": str(int(time.time() * 1000)),
        "mchNo": MCH_NO,
        "appId": APP_ID,
        **fields,
    }
    payload["sign"] = sign(payload)
    return payload


def assert_api_failure(name, response, expected_message=None):
    assert response.get("code") == 9999, f"{name}: expected code=9999, got {response}"
    if expected_message is not None:
        assert response.get("msg") == expected_message, (
            f"{name}: expected msg={expected_message!r}, got {response.get('msg')!r}"
        )


def log_result(name, http_status, response, extra=""):
    suffix = f" {extra}" if extra else ""
    print(
        f"{name}=PASS http={http_status} code={response.get('code')} "
        f"msg={response.get('msg')!r}{suffix}"
    )


def query_by(field, value):
    payload = auth_payload(**{field: value})
    return request("/api/pay/query", payload)


def assert_baseline_query(name, http_status, response):
    assert http_status == 200, f"{name}: HTTP {http_status}"
    assert response.get("code") == 0, f"{name}: {response}"
    data = response["data"]
    assert data["payOrderId"] == EXISTING_PAY_ORDER_ID
    assert data["mchOrderNo"] == EXISTING_MCH_ORDER_NO
    assert data["state"] == 2
    assert data["amount"] == 4000
    assert data["currency"] == "TWD"
    assert response.get("sign") == sign(data), f"{name}: response signature mismatch"
    log_result(name, http_status, response, "state=2 amount=4000 responseSign=VALID")
    return data


def create_payload(mch_order_no, **overrides):
    payer = {
        "payerName": "黑箱測試",
        "payerPostcode": "100",
        "payerAddress": "台北市測試路1號",
        "payerMobile": "0900000000",
        "payerEmail": "blackbox@example.test",
    }
    fields = {
        "mchOrderNo": mch_order_no,
        "wayCode": WAY_CODE,
        "amount": 4000,
        "currency": "TWD",
        "subject": f"{PROVIDER_PREFIX} ibon black-box negative test",
        "body": "Must be rejected before Provider Create",
        "notifyUrl": "https://merchant.example.test/callback/jeepay",
        "clientIp": "127.0.0.1",
        "expiredTime": 604800,
        "channelExtra": json.dumps(payer, ensure_ascii=False, separators=(",", ":")),
    }
    fields.update(overrides)
    return auth_payload(**fields)


def assert_order_absent(name, mch_order_no):
    time.sleep(0.01)
    http_status, response = query_by("mchOrderNo", mch_order_no)
    assert http_status == 200, f"{name}: follow-up Query HTTP {http_status}"
    assert_api_failure(name, response, "訂單不存在")
    log_result(name + "_NO_ORDER", http_status, response)


def main():
    baseline_http, baseline_response = query_by("mchOrderNo", EXISTING_MCH_ORDER_NO)
    baseline = assert_baseline_query("P0_QUERY_BY_MCH_ORDER_NO", baseline_http, baseline_response)

    pay_id_http, pay_id_response = query_by("payOrderId", EXISTING_PAY_ORDER_ID)
    pay_id_data = assert_baseline_query("P0_QUERY_BY_PAY_ORDER_ID", pay_id_http, pay_id_response)
    stable_keys = ("payOrderId", "mchOrderNo", "amount", "currency", "state", "createdAt")
    assert {key: baseline.get(key) for key in stable_keys} == {
        key: pay_id_data.get(key) for key in stable_keys
    }

    for index in range(1, 4):
        http_status, response = query_by("mchOrderNo", EXISTING_MCH_ORDER_NO)
        data = assert_baseline_query(f"P0_REPEAT_QUERY_{index}", http_status, response)
        assert {key: baseline.get(key) for key in stable_keys} == {
            key: data.get(key) for key in stable_keys
        }

    stale = auth_payload(mchOrderNo=EXISTING_MCH_ORDER_NO)
    stale["reqTime"] = "1577808000000"  # 2020-01-01，超出 5 分鐘 freshness 窗口
    stale["sign"] = sign(stale)
    http_status, response = request("/api/pay/query", stale)
    assert http_status == 200
    assert_api_failure("P0_STALE_REQTIME", response, "請求時間戳已過期")
    log_result("P0_STALE_REQTIME", http_status, response)

    tampered = auth_payload(mchOrderNo=EXISTING_MCH_ORDER_NO)
    tampered["mchOrderNo"] = EXISTING_MCH_ORDER_NO + "-TAMPERED"
    http_status, response = request("/api/pay/query", tampered)
    assert http_status == 200
    assert_api_failure("P0_TAMPERED_SIGNATURE", response, "簽章驗證失敗")
    assert not response.get("data")
    log_result("P0_TAMPERED_SIGNATURE", http_status, response, "data=EMPTY")

    missing_ids = auth_payload()
    http_status, response = request("/api/pay/query", missing_ids)
    assert http_status == 200
    assert_api_failure(
        "P0_MISSING_QUERY_IDENTIFIERS",
        response,
        "mchOrderNo 和 payOrderId 不能同時為空",
    )
    log_result("P0_MISSING_QUERY_IDENTIFIERS", http_status, response)

    duplicate = create_payload(EXISTING_MCH_ORDER_NO)
    http_status, response = request("/api/pay/unifiedOrder", duplicate)
    assert http_status == 200
    assert_api_failure(
        "P0_DUPLICATE_CREATE",
        response,
        f"商戶訂單[{EXISTING_MCH_ORDER_NO}]已存在",
    )
    assert not response.get("data")
    log_result("P0_DUPLICATE_CREATE", http_status, response, "data=EMPTY")

    stamp = str(int(time.time() * 1000))
    negative_cases = [
        (
            "P1_INVALID_AMOUNT",
            f"BB-{stamp}-AMOUNT",
            {"amount": 4050},
            "金額必須為整數 TWD 元",
        ),
        (
            "P1_INVALID_CURRENCY",
            f"BB-{stamp}-CURRENCY",
            {"currency": "USD"},
            f"{PROVIDER_PREFIX} ibon 僅支援 TWD",
        ),
        (
            "P1_MALFORMED_CHANNEL_EXTRA",
            f"BB-{stamp}-MALFORMED",
            {"channelExtra": "{not-json"},
            f"{PROVIDER_PREFIX} channelExtra 格式錯誤",
        ),
        (
            "P1_MISSING_PAYER_FIELD",
            f"BB-{stamp}-PAYER",
            {
                "channelExtra": json.dumps(
                    {
                        "payerName": "黑箱測試",
                        "payerPostcode": "100",
                        "payerAddress": "台北市測試路1號",
                        "payerMobile": "0900000000",
                    },
                    ensure_ascii=False,
                    separators=(",", ":"),
                )
            },
            f"{PROVIDER_PREFIX} channelExtra 缺少繳款人資料",
        ),
        (
            "P1_UNSUPPORTED_WAYCODE",
            f"BB-{stamp}-WAYCODE",
            {"wayCode": "ALI_JSAPI"},
            "商戶應用不支援該支付方式",
        ),
        (
            "P1_MISSING_SUBJECT",
            f"BB-{stamp}-SUBJECT",
            {"subject": None},
            "商品標題不能為空",
        ),
    ]
    for name, mch_order_no, overrides, expected_message in negative_cases:
        payload = create_payload(mch_order_no, **overrides)
        http_status, response = request("/api/pay/unifiedOrder", payload)
        assert http_status == 200
        assert_api_failure(name, response, expected_message)
        assert not response.get("data")
        log_result(name, http_status, response, "data=EMPTY")
        assert_order_absent(name, mch_order_no)

    final_http, final_response = query_by("mchOrderNo", EXISTING_MCH_ORDER_NO)
    assert_baseline_query("FINAL_EXISTING_ORDER_UNCHANGED", final_http, final_response)
    print(
        f"D01_BLACKBOX_SUITE=PASS wayCode={WAY_CODE} "
        "providerCreateCallsExpected=0 realPaymentTriggered=NO"
    )


if __name__ == "__main__":
    main()
