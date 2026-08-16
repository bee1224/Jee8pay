#!/usr/bin/env bash
# 驗證 Production admin-v2 路由的 declarative 契約（F01 修復）。
#
# 背景：V1 edge（lp33ing-production-edge）的 compose 屬 V1 legacy infra（不在本 repo），
# 但它 runtime 必須 attach 到 jee8pay-v2-production-network 才能解析 V2 manager-ui。
# 本腳本把「edge ↔ v2-network ↔ manager-ui」的契約與現況一次驗證，edge recreate 後應重跑。
#
# 用法：在 lp33ing-production 上以 sudo 執行；需與 V1/V2 containers 同一 host。
set -uo pipefail

edge=lp33ing-production-edge
net=jee8pay-v2-production-network
host_v2=admin-v2.lp33ing.com
host_v1_api=api.lp33ing.com
host_v1_admin=admin.lp33ing.com
fail=0

check() {
  local name="$1"; shift
  if "$@"; then printf 'PASS %s\n' "$name"; else printf 'FAIL %s\n' "$name"; fail=1; fi
}

check EDGE_RUNNING docker inspect "$edge" --format '{{.State.Status}}|{{.State.Health.Status}}' | grep -q 'running|healthy'
check EDGE_ON_V2_NETWORK docker inspect "$edge" --format '{{json .NetworkSettings.Networks}}' | grep -q "$net"
check MANAGER_UI_RESOLVES docker exec "$edge" getent hosts manager-ui | grep -q 'manager-ui'
check RESOLVER_VARIABLE_ROUTE docker exec "$edge" sh -c "grep -q 'resolver 127.0.0.11' /etc/nginx/nginx.conf && grep -q 'set \$admin_ui manager-ui' /etc/nginx/nginx.conf"
check NGINX_SYNTAX docker exec "$edge" nginx -t >/dev/null 2>&1

admin_title=$(curl -s --max-time 15 "https://$host_v2/" | grep -oE '<title>[^<]*</title>' | head -1)
check ADMIN_V2_SERVES_MANAGER [ -n "$admin_title" ]
printf 'INFO  %s => %s\n' "$host_v2" "$admin_title"

v1_api_status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://$host_v1_api/edge-health" 2>/dev/null || echo 000)
check V1_API_NO_REGRESSION [ "$v1_api_status" != "000" ]
printf 'INFO  %s edge-health => %s\n' "$host_v1_api" "$v1_api_status"

# V1 admin 登入頁（HTTP status 可接受 200/302/403，只要不是 444/000）
v1_admin_status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://$host_v1_admin/" 2>/dev/null || echo 000)
check V1_ADMIN_NO_REGRESSION [ "$v1_admin_status" != "000" ] && [ "$v1_admin_status" != "444" ]
printf 'INFO  %s => %s\n' "$host_v1_admin" "$v1_admin_status"

if [ "$fail" -eq 0 ]; then
  printf 'PROD_EDGE_ADMIN_ROUTE=PASS\n'
else
  printf 'PROD_EDGE_ADMIN_ROUTE=FAIL\n' >&2
  exit 1
fi
