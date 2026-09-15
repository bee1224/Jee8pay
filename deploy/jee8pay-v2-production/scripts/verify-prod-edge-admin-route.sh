#!/usr/bin/env bash
# 驗證 Production admin-v2 路由的 declarative 契約（F01 修復；V1 退役後更新 2026-08-23）。
#
# 背景：edge（jee8pay-v2-production-edge）由 V2 compose 管理，attach 到
# jee8pay-v2-production-network 才能解析 V2 manager-ui。
# 本腳本把「edge ↔ v2-network ↔ manager-ui」的契約與現況一次驗證，edge recreate 後應重跑。
#
# 用法：在 lp33ing-production 上以 sudo 執行。
set -uo pipefail

edge=jee8pay-v2-production-edge
net=jee8pay-v2-production-network
host_v2=admin-v2.lp33ing.com
fail=0

check() {
  local name="$1"; shift
  if "$@" >/dev/null 2>&1; then printf 'PASS %s\n' "$name"; else printf 'FAIL %s\n' "$name"; fail=1; fi
}

check_match() {
  local name="$1" pattern="$2"; shift 2
  if "$@" 2>/dev/null | grep -qE "$pattern"; then printf 'PASS %s\n' "$name"; else printf 'FAIL %s\n' "$name"; fail=1; fi
}

check_match EDGE_RUNNING 'running\|healthy' \
  docker inspect "$edge" --format '{{.State.Status}}|{{.State.Health.Status}}'
check_match EDGE_ON_V2_NETWORK "$net" \
  docker inspect "$edge" --format '{{json .NetworkSettings.Networks}}'
check_match MANAGER_UI_RESOLVES 'manager-ui' \
  docker exec "$edge" getent hosts manager-ui
check RESOLVER_VARIABLE_ROUTE \
  docker exec "$edge" sh -c "grep -q 'resolver 127.0.0.11' /etc/nginx/nginx.conf && grep -q 'set \$admin_ui manager-ui' /etc/nginx/nginx.conf"
check NGINX_SYNTAX docker exec "$edge" nginx -t

admin_title=$(curl -s --max-time 15 "https://$host_v2/" | grep -oE '<title>[^<]*</title>' | head -1)
check ADMIN_V2_SERVES_MANAGER [ -n "$admin_title" ]
printf 'INFO  %s => %s\n' "$host_v2" "$admin_title"

if [ "$fail" -eq 0 ]; then
  printf 'PROD_EDGE_ADMIN_ROUTE=PASS\n'
else
  printf 'PROD_EDGE_ADMIN_ROUTE=FAIL\n' >&2
  exit 1
fi
