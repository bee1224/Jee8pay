#!/usr/bin/env bash
# 將 allowlist.json 套用到 edge nginx 的 allow 檔（uat.conf），有變更就 reload edge。
#
# 背景：allowlist.json 由 manager 後端 API（UAT Edge 白名單頁面）寫入 /opt/jee8pay-v2-dev/edge-allowlist，
#       edge 容器以 read-only bind mount 該目錄為 /etc/nginx/allowlist，nginx 設定以
#       `include /etc/nginx/allowlist/uat.conf;` 引用 allow 規則。
# 本腳本由 root crontab 每分鐘執行：比對 json 與 uat.conf 的 sha，變更時重新產生並 nginx -t + reload，
# 失敗自動回滾，並把結果寫入 status.txt 供頁面顯示。
set -uo pipefail

dir=/opt/jee8pay-v2-dev/edge-allowlist
json="$dir/allowlist.json"
conf="$dir/uat.conf"
status="$dir/status.txt"
edge=nnviopp-sandbox-edge

[ -f "$json" ] || exit 0

json_sha=$(sha256sum "$json" | awk '{print $1}')
tmp=$(mktemp)
{
  echo "# generated from allowlist.json sha=$json_sha at $(date -Iseconds)"
  python3 -c '
import json, sys
try:
    data = json.load(open(sys.argv[1], encoding="utf-8"))
except Exception:
    sys.exit(1)
for entry in data:
    ip = entry.get("ip")
    if not ip:
        sys.exit(1)
    print("allow %s;" % ip)
' "$json"
} > "$tmp" || {
  echo "$(date -Iseconds) FAILED json=$json_sha invalid-json-not-applied" > "$status"
  rm -f "$tmp"
  exit 0
}

new_sha=$(sha256sum "$tmp" | awk '{print $1}')
old_sha=missing
[ -f "$conf" ] && old_sha=$(sha256sum "$conf" | awk '{print $1}')

if [ "$new_sha" = "$old_sha" ]; then
  rm -f "$tmp"
  exit 0
fi

[ -f "$conf" ] && cp -p "$conf" "$conf.bak"
cp "$tmp" "$conf"
rm -f "$tmp"

if sudo -n docker exec "$edge" nginx -t >/dev/null 2>&1; then
  if sudo -n docker exec "$edge" nginx -s reload >/dev/null 2>&1; then
    echo "$(date -Iseconds) APPLIED json=$json_sha conf=$new_sha reload=OK" > "$status"
  else
    echo "$(date -Iseconds) FAILED json=$json_sha reload-failed" > "$status"
  fi
else
  [ -f "$conf.bak" ] && cp "$conf.bak" "$conf"
  echo "$(date -Iseconds) FAILED json=$json_sha nginx-test-failed-rolled-back" > "$status"
fi
