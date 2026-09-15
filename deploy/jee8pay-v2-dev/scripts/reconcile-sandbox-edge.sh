#!/usr/bin/env bash
set -euo pipefail

readonly edge=nnviopp-sandbox-edge
readonly expected_host=server1.nnviopp.com
readonly sandbox_ip=159.198.40.128
readonly expected_config_sha=7a393f332a6830c932a4e51b1754165af2be652b61a31640edcfbd79ca328ea4
readonly final_config=/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf
readonly compose_file=/opt/jee8pay-v2-dev/edge/compose.edge.yaml
readonly project=jee8pay-v2-dev-edge
readonly transit_network=jee8pay-v2-dev-edge-transit
readonly expected_network_set=jee8pay-v2-dev-edge-transit,jee8pay-v2-dev-network

fail() {
  printf 'RECONCILE=FAIL_%s\n' "$1" >&2
  exit 2
}

[[ $EUID -eq 0 ]] || fail REQUIRES_ROOT
[[ ${SANDBOX_EDGE_RECONCILE_APPROVED:-} == YES ]] || fail EXPLICIT_APPROVAL_REQUIRED
[[ $(hostname) == "$expected_host" ]] || fail WRONG_HOST
[[ -f $final_config ]] || fail CONFIG_MISSING
[[ $(sha256sum "$final_config" | awk '{print $1}') == "$expected_config_sha" ]] || fail CONFIG_CHECKSUM
[[ $(stat -c '%u:%g:%a' "$final_config") == '0:10002:640' ]] || fail CONFIG_OWNER_MODE
[[ -f $compose_file ]] || fail COMPOSE_MISSING
[[ $(stat -c '%u:%g:%a' "$compose_file") == '0:0:644' ]] || fail COMPOSE_OWNER_MODE

# V2 路由契約
[[ $(grep -Fc 'location = /api/pay/unifiedOrder {' "$final_config") -eq 1 ]] || fail CREATE_ROUTE
[[ $(grep -Fc 'location = /api/pay/query {' "$final_config") -eq 1 ]] || fail QUERY_ROUTE
[[ $(grep -Fc 'location = /api/pay/notify/ryo {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_RYO
[[ $(grep -Fc 'location = /api/pay/notify/jay {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_JAY
[[ $(grep -Fc 'location = /api/pay/notify/chi {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_CHI
[[ $(grep -Fc 'location = /api/pay/notify/jhd {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_JHD
[[ $(grep -Fc 'include /etc/nginx/allowlist/uat.conf;' "$final_config") -eq 2 ]] || fail ALLOWLIST_INCLUDE
! grep -Fq '35.220.239.87' "$final_config" || fail PRODUCTION_IP_PRESENT
# V1 已退役：config 不得含 V1 hostname / upstream
! grep -Eq 'sandbox-api\.nnviopp\.com|sandbox\.nnviopp\.com|merchant-sandbox\.nnviopp\.com|upstream payment_(api|admin)|upstream merchant_receiver' "$final_config" ||
  fail V1_REFERENCE_PRESENT
# allow 檔內容：Talend 兩台測試機必須存在（系統保留）
[[ -f /opt/jee8pay-v2-dev/edge-allowlist/uat.conf ]] || fail ALLOWLIST_FILE_MISSING
[[ $(grep -Fc 'allow 34.92.245.74;' /opt/jee8pay-v2-dev/edge-allowlist/uat.conf) -eq 1 ]] || fail ALLOWLIST_PRIMARY
[[ $(grep -Fc 'allow 34.92.52.162;' /opt/jee8pay-v2-dev/edge-allowlist/uat.conf) -eq 1 ]] || fail ALLOWLIST_SECONDARY

[[ $(docker network inspect "$transit_network" --format '{{.Name}}|{{.Internal}}') == "$transit_network|true" ]] ||
  fail TRANSIT_NETWORK
[[ $(docker inspect jee8pay-v2-dev-callback-ingress-1 --format '{{.State.Status}}|{{.State.Health.Status}}') == 'running|healthy' ]] ||
  fail CALLBACK_INGRESS_HEALTH
[[ $(docker inspect jee8pay-v2-dev-merchant-uat-merchant-api-ingress-1 --format '{{.State.Status}}|{{.State.Health.Status}}') == 'running|healthy' ]] ||
  fail MERCHANT_INGRESS_HEALTH

cd /opt/jee8pay-v2-dev/edge
docker compose -p "$project" -f "$compose_file" up -d --no-deps --no-build --force-recreate sandbox-edge

state=
for _ in $(seq 1 18); do
  state=$(docker inspect "$edge" --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' 2>/dev/null || true)
  [[ $state == 'running|healthy' ]] && break
  sleep 5
done
[[ $state == 'running|healthy' ]] || fail EDGE_HEALTH_TIMEOUT

docker exec "$edge" nginx -t >/dev/null 2>&1 || fail NGINX_CONFIG
[[ $(docker exec "$edge" sha256sum /etc/nginx/nginx.conf | awk '{print $1}') == "$expected_config_sha" ]] ||
  fail ACTIVE_CONFIG
[[ $(docker inspect "$edge" --format '{{.HostConfig.RestartPolicy.Name}}') == 'unless-stopped' ]] ||
  fail RESTART_POLICY
[[ $(docker inspect "$edge" --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/nginx.conf"}}{{.Source}}|{{.RW}}{{end}}{{end}}') == "$final_config|false" ]] ||
  fail CONFIG_MOUNT

network_names=$(docker inspect "$edge" --format '{{json .NetworkSettings.Networks}}' |
  jq -r 'keys | sort | join(",")')
[[ $network_names == "$expected_network_set" ]] || fail NETWORK_SET
while read -r network_id; do
  docker network inspect "$network_id" >/dev/null 2>&1 || fail NETWORK_ID
done < <(docker inspect "$edge" --format '{{json .NetworkSettings.Networks}}' |
  jq -r 'to_entries[].value.NetworkID')

ss -H -lnt | grep -Fq "$sandbox_ip:80 " || fail PORT_80
ss -H -lnt | grep -Fq "$sandbox_ip:443 " || fail PORT_443

printf 'RECONCILE=PASS\n'
printf 'EDGE=%s\n' "$edge"
printf 'PROJECT=%s\n' "$project"
printf 'ACTIVE_CONFIG_SHA256=%s\n' "$expected_config_sha"
printf 'NETWORKS=%s\n' "$network_names"
printf 'RESTART_POLICY=unless-stopped\n'
