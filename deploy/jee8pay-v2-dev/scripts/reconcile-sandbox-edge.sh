#!/usr/bin/env bash
set -euo pipefail

readonly edge=nnviopp-sandbox-edge
readonly expected_host=server1.nnviopp.com
readonly sandbox_ip=159.198.40.128
readonly expected_config_sha=67bedd649546d54eb6337f205c5b5edb8e7dd9f1910105e726cddbdc1a2bc915
readonly expected_overlay_sha=4e583abf4253e69daef8aa8c0dd7f612d669595528ff081330ef8b6c4eec5a9b
readonly final_config=/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf
readonly overlay=/opt/jee8pay-v2-dev/public-callback/compose.edge-overlay.yaml
readonly v1_dir=/opt/payment/payment-service-sandbox
readonly env_file=/etc/nnviopp-sandbox/payment-service.env
readonly transit_network=jee8pay-v2-dev-edge-transit

readonly -a compose_files=(
  "$v1_dir/compose.yaml"
  "$v1_dir/compose.sandbox-hardened.yaml"
  "$v1_dir/compose.sandbox-edge.yaml"
  "$overlay"
)

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
[[ -f $overlay ]] || fail OVERLAY_MISSING
[[ $(sha256sum "$overlay" | awk '{print $1}') == "$expected_overlay_sha" ]] || fail OVERLAY_CHECKSUM
[[ $(stat -c '%u:%g:%a' "$overlay") == '0:0:600' ]] || fail OVERLAY_OWNER_MODE
[[ -f $env_file ]] || fail ENV_FILE_MISSING

[[ $(grep -Fc 'location = /api/pay/unifiedOrder {' "$final_config") -eq 1 ]] || fail CREATE_ROUTE
[[ $(grep -Fc 'location = /api/pay/query {' "$final_config") -eq 1 ]] || fail QUERY_ROUTE
[[ $(grep -Fc 'location = /api/pay/notify/ccat {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE
# 白名單改為 include：主設定檔不再內嵌 allow，allow 檔由 host cron 依 allowlist.json 產生
[[ $(grep -Fc 'include /etc/nginx/allowlist/uat.conf;' "$final_config") -eq 2 ]] || fail ALLOWLIST_INCLUDE
! grep -Fq '35.220.239.87' "$final_config" || fail PRODUCTION_IP_PRESENT
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

compose_args=()
for compose_file in "${compose_files[@]}"; do
  compose_args+=( -f "$compose_file" )
done

cd "$v1_dir"
docker compose --env-file "$env_file" -p nnviopp-sandbox \
  "${compose_args[@]}" up -d --no-deps --no-build --force-recreate sandbox-edge

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
[[ $network_names == 'jee8pay-v2-dev-edge-transit,nnviopp-sandbox_edge,nnviopp-sandbox_edge-public' ]] ||
  fail NETWORK_SET
while read -r network_id; do
  docker network inspect "$network_id" >/dev/null 2>&1 || fail NETWORK_ID
done < <(docker inspect "$edge" --format '{{json .NetworkSettings.Networks}}' |
  jq -r 'to_entries[].value.NetworkID')

ss -H -lnt | grep -Fq "$sandbox_ip:80 " || fail PORT_80
ss -H -lnt | grep -Fq "$sandbox_ip:443 " || fail PORT_443

printf 'RECONCILE=PASS\n'
printf 'EDGE=%s\n' "$edge"
printf 'ACTIVE_CONFIG_SHA256=%s\n' "$expected_config_sha"
printf 'NETWORKS=%s\n' "$network_names"
printf 'RESTART_POLICY=unless-stopped\n'
