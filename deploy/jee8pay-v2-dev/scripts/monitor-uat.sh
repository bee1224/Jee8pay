#!/usr/bin/env bash
# JeePay V2 External UAT — read-only monitoring snapshot
# 用法：在 nnviopp-sandbox 上以 sudo 執行：
#   sudo -n ./deploy/jee8pay-v2-dev/scripts/monitor-uat.sh
# 全程唯讀：僅查詢 DB 與 container 狀態，不修改任何資料。
set -uo pipefail

DB=jee8pay-v2-dev-db-1
PAYMENT=jee8pay-v2-dev-payment-1
DB_NAME=jee8pay_v2_dev

db() {
  sudo -n docker exec -i "$DB" sh -c \
    "mariadb -uroot -p\$(cat /run/secrets/db-root-password) $DB_NAME -N" <<<"$1" 2>/dev/null
}

echo "===== UAT 監控快照 $(date '+%Y-%m-%d %H:%M:%S %z') ====="

echo "--- 1. D01 商戶訂單狀態分佈 ---"
db "SELECT state, COUNT(*) FROM t_pay_order WHERE mch_no='M_D01_EXTERNAL_UAT' GROUP BY state;"
echo "(state: 0=INIT 1=ING 2=SUCCESS 3=FAIL 4=CANCEL 5=REFUND 6=CLOSED)"

echo "--- 2. 重點觀測訂單 ---"
db "SELECT pay_order_id, mch_order_no, state, amount, expired_time FROM t_pay_order
    WHERE pay_order_id IN ('P2088706121058914305','P2088706123088957441','P2088297924112322562');"

echo "--- 3. 最近 10 筆 notify record ---"
db "SELECT notify_id, order_id, state, notify_count, notify_count_limit, LEFT(notify_url,45)
    FROM t_mch_notify_record ORDER BY notify_id DESC LIMIT 10;"
echo "(state: 1=通知中 2=成功 3=失敗)"

echo "--- 4. 最近 15 分鐘 payment log 黑猫 PAY（RYO/JAY/CHI）事件數 ---"
sudo -n docker logs --since 15m "$PAYMENT" 2>&1 | grep -cE "RYO_CREATE|JAY_CREATE|CHI_CREATE|RYO_APN|JAY_APN|CHI_APN|RYO.*reject|JAY.*reject|CHI.*reject" || echo 0

echo "--- 5. container 健康 ---"
sudo -n docker ps --filter "name=jee8pay-v2-dev" --format "{{.Names}} | {{.Status}}" | head -12
