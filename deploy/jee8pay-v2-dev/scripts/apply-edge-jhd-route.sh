#!/usr/bin/env bash
# 測試環境（jee8pay-v2-dev）：為公開 edge 加上 /api/pay/notify/jhd 路由（TD-014）。
# 由 operator 以 root 親自執行；執行前需手動輸入 YES，失敗時自動還原並重新 reconcile。
# 用法：sudo bash apply-edge-jhd-route.sh /path/to/repo/deploy/jee8pay-v2-dev/merchant-uat/prepare-edge-nginx.py
set -euo pipefail

readonly D=/opt/jee8pay-v2-dev/merchant-uat
readonly BIN=/opt/jee8pay-v2-dev/bin
readonly OLD_SHA=840afb1a28b46f783059c4186c449ad949a6b60f8e838034b32eecee22be1b3e
readonly NEW_SHA=7a393f332a6830c932a4e51b1754165af2be652b61a31640edcfbd79ca328ea4
readonly SRC=${1:?需要 repo 版 prepare-edge-nginx.py 路徑}
readonly BK=/opt/jee8pay-v2-dev/state/edge-jhd-$(date +%Y%m%d-%H%M%S)

[[ $EUID -eq 0 ]] || { echo "請以 root 執行"; exit 1; }
current=$(sha256sum "$D/nginx.proposed.conf" | awk '{print $1}')
if [[ $current == "$NEW_SHA" ]]; then echo "ALREADY_APPLIED"; exit 0; fi
[[ $current == "$OLD_SHA" ]] || { echo "ABORT: 現行 edge 設定 SHA 非預期（$current）"; exit 1; }

echo "將套用 edge 設定 $NEW_SHA（新增 jhd APN 路由，dns-only 模式）。"
read -r -p "確認執行請輸入 YES：" answer
[[ $answer == YES ]] || { echo "已取消"; exit 1; }

mkdir -p "$BK" && chmod 700 "$BK"
cp -p "$D/nginx.proposed.conf" "$D/prepare-edge-nginx.py" "$BIN/reconcile-sandbox-edge" "$BIN/validate-sandbox-edge" "$BK/"

rollback() {
  echo "失敗，還原備份並重新 reconcile…"
  cp -p "$BK/nginx.proposed.conf" "$BK/prepare-edge-nginx.py" "$D/"
  cp -p "$BK/reconcile-sandbox-edge" "$BK/validate-sandbox-edge" "$BIN/"
  SANDBOX_EDGE_RECONCILE_APPROVED=YES "$BIN/reconcile-sandbox-edge" || true
  "$BIN/validate-sandbox-edge" || true
  echo "ROLLED_BACK（備份：$BK）"
  exit 1
}

install -m 0700 -o root -g root "$SRC" "$D/prepare-edge-nginx.py"
python3 "$D/prepare-edge-nginx.py" --origin-mode dns-only || rollback
chown root:10002 "$D/nginx.proposed.conf" && chmod 0640 "$D/nginx.proposed.conf"
[[ $(sha256sum "$D/nginx.proposed.conf" | awk '{print $1}') == "$NEW_SHA" ]] || rollback
sed -i "s/^readonly expected_config_sha=.*/readonly expected_config_sha=$NEW_SHA/" "$BIN/reconcile-sandbox-edge" "$BIN/validate-sandbox-edge"

SANDBOX_EDGE_RECONCILE_APPROVED=YES "$BIN/reconcile-sandbox-edge" || rollback
"$BIN/validate-sandbox-edge" || rollback

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' --data '{}' https://ccat-v2-dev.nnviopp.com/api/pay/notify/jhd || true)
echo "公開 jhd 回呼（預期 400 fail-closed）：$code"
[[ $code == 400 ]] || rollback
echo "DONE（備份：$BK）"
