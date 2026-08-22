#!/usr/bin/env python3
"""產生可直接貼到 Talend REST 的建單/查單 Header + Body。

原因：sign 依 reqTime（5 分鐘新鮮度）與 body 內容每次重算，無法給靜態範例，
所以由本腳本讀取 credential、產生目前 reqTime 並簽名，輸出整塊可貼內容。

用法：
    export UAT_MERCHANT_ID=M_D01_EXTERNAL_UAT
    export UAT_APP_ID=APP_D01_EXTERNAL_UAT
    export UAT_APP_SECRET=<secure handoff 取得的 App Secret>
    export UAT_NOTIFY_URL=https://<你的接收端>/callback   # full UAT 必填（fail closed）

    python3 talend-request-gen.py [--way-code RYO_IBON|JAY_IBON|CHI_IBON]
        # 同時輸出建單 + 查單（預設 RYO_IBON；JAY/CHI 用 --way-code 指定）
    python3 talend-request-gen.py --query         # 只輸出查單（自動用剛才建單的 mchOrderNo 檔）
    python3 talend-request-gen.py --query --mch-order-no UAT-xxx   # 指定 mchOrderNo 查單

產出格式：Header 一行一欄；Body 為可整段複製的 JSON。
安全：本腳本只印 Header/Body（含 sign），不會印 App Secret 或含 secret 的 canonical string。
"""
import argparse
import hashlib
import json
import os
import sys
import time
import uuid
from datetime import datetime

BASE_URL = "https://api-v2-dev.nnviopp.com"
CREATE_PATH = "/api/pay/unifiedOrder"
QUERY_PATH = "/api/pay/query"

STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".last-mch-order-no")


def require_env(name):
    value = os.environ.get(name)
    if not value:
        print(f"MISSING_ENV={name}", file=sys.stderr)
        sys.exit(2)
    return value


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


def sign(payload, secret):
    canonical = canonicalize(payload, secret)
    return hashlib.md5(canonical.encode("utf-8")).hexdigest().upper()


def print_block(title, method, url, payload):
    print("=" * 70)
    print(title)
    print("=" * 70)
    print(f"{method} {url}")
    print("Content-Type: application/json; charset=UTF-8")
    print()
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    print()


def build_create(secret, notify_url, way_code):
    # 唯一性：毫秒 + 6 位隨機 hex，避免同秒/平行執行碰撞（重複 mchOrderNo 會回「商戶訂單已存在」）
    mch_order_no = "UAT-TALEND-" + datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:6].upper()
    channel_extra = json.dumps(
        {
            "payerName": "王小明",
            "payerPostcode": "100",
            "payerAddress": "台北市測試路1號",
            "payerMobile": "0900000000",
            "payerEmail": "uat@example.test",
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )
    if way_code == "JAY_IBON":
        subject = "JAY ibon UAT"
    elif way_code == "CHI_IBON":
        subject = "CHI ibon UAT"
    else:
        subject = "RYO ibon UAT"
    payload = {
        "version": "1.0",
        "signType": "MD5",
        "reqTime": str(int(time.time() * 1000)),
        "mchNo": os.environ["UAT_MERCHANT_ID"],
        "appId": os.environ["UAT_APP_ID"],
        "mchOrderNo": mch_order_no,
        "wayCode": way_code,
        "amount": 4000,  # = TWD 40（已實測；必須可整除 100）
        "currency": "TWD",
        "subject": subject,
        "body": "Talend UAT test order",
        "expiredTime": 3600,
        "channelExtra": channel_extra,
    }
    if notify_url:
        payload["notifyUrl"] = notify_url
    payload["sign"] = sign(payload, secret)

    with open(STATE_FILE, "w", encoding="utf-8") as fh:
        fh.write(mch_order_no)
    return payload


def build_query(secret, mch_order_no):
    payload = {
        "version": "1.0",
        "signType": "MD5",
        "reqTime": str(int(time.time() * 1000)),
        "mchNo": os.environ["UAT_MERCHANT_ID"],
        "appId": os.environ["UAT_APP_ID"],
        "mchOrderNo": mch_order_no,
    }
    payload["sign"] = sign(payload, secret)
    return payload


def main():
    parser = argparse.ArgumentParser(description="產生 Talend 可貼上的建單/查單 Header+Body")
    parser.add_argument("--query", action="store_true", help="只輸出查單")
    parser.add_argument("--mch-order-no", help="查單用的 mchOrderNo（預設用上次建單的值）")
    parser.add_argument("--way-code", choices=("RYO_IBON", "JAY_IBON", "CHI_IBON"),
                        default="RYO_IBON", help="上游通道（預設 RYO_IBON）")
    args = parser.parse_args()

    require_env("UAT_MERCHANT_ID")
    require_env("UAT_APP_ID")
    require_env("UAT_APP_SECRET")
    # full UAT 需要 Merchant Notify 驗收，notifyUrl 必填（fail closed）；純 Create/Query smoke 不需要本腳本
    notify_url = require_env("UAT_NOTIFY_URL")
    secret = os.environ["UAT_APP_SECRET"]

    if args.query:
        mch_order_no = args.mch_order_no
        if not mch_order_no and os.path.exists(STATE_FILE):
            mch_order_no = open(STATE_FILE, encoding="utf-8").read().strip()
        if not mch_order_no:
            print("QUERY_NEEDS_MCH_ORDER_NO：請用 --mch-order-no 指定，或先跑一次建單", file=sys.stderr)
            sys.exit(2)
        print_block("查單 Query（POST）", "POST", BASE_URL + QUERY_PATH, build_query(secret, mch_order_no))
        return

    print_block("建單 Create（POST）", "POST", BASE_URL + CREATE_PATH,
                build_create(secret, notify_url, args.way_code))
    print_block("查單 Query（POST）", "POST", BASE_URL + QUERY_PATH, build_query(secret, read_last_mch_order_no()))


def read_last_mch_order_no():
    if os.path.exists(STATE_FILE):
        return open(STATE_FILE, encoding="utf-8").read().strip()
    return ""


if __name__ == "__main__":
    main()
