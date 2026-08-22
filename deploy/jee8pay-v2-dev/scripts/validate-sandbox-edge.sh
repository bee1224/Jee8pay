#!/usr/bin/env bash
set -euo pipefail

readonly edge=nnviopp-sandbox-edge
readonly expected_host=server1.nnviopp.com
readonly sandbox_ip=159.198.40.128
readonly expected_config_sha=cb1500d31110f06e5211089976ac8436329567ba007ef854f4baceaaf24e56b6
readonly expected_overlay_sha=4e583abf4253e69daef8aa8c0dd7f612d669595528ff081330ef8b6c4eec5a9b
readonly final_config=/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf
readonly overlay=/opt/jee8pay-v2-dev/public-callback/compose.edge-overlay.yaml
readonly evidence_dir=/opt/jee8pay-v2-dev/state/n01
readonly evidence_file="$evidence_dir/validation-latest.txt"

fail() {
  printf 'VALIDATION=FAIL_%s\n' "$1" >&2
  exit 2
}

[[ $EUID -eq 0 ]] || fail REQUIRES_ROOT
[[ $(hostname) == "$expected_host" ]] || fail WRONG_HOST
install -d -m 0700 -o root -g root "$evidence_dir"
install -m 0600 -o root -g root /dev/null "$evidence_file"

edge_state=$(docker inspect "$edge" --format '{{.State.Status}}|{{.State.Health.Status}}')
[[ $edge_state == 'running|healthy' ]] || fail EDGE_HEALTH
[[ $(docker inspect "$edge" --format '{{.HostConfig.RestartPolicy.Name}}') == 'unless-stopped' ]] ||
  fail RESTART_POLICY
[[ $(docker exec "$edge" sha256sum /etc/nginx/nginx.conf | awk '{print $1}') == "$expected_config_sha" ]] ||
  fail ACTIVE_CONFIG
[[ $(sha256sum "$final_config" | awk '{print $1}') == "$expected_config_sha" ]] || fail HOST_CONFIG
[[ $(stat -c '%u:%g:%a' "$final_config") == '0:10002:640' ]] || fail CONFIG_OWNER_MODE
[[ $(sha256sum "$overlay" | awk '{print $1}') == "$expected_overlay_sha" ]] || fail OVERLAY
[[ $(stat -c '%u:%g:%a' "$overlay") == '0:0:600' ]] || fail OVERLAY_OWNER_MODE
[[ $(docker inspect "$edge" --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/nginx.conf"}}{{.Source}}|{{.RW}}{{end}}{{end}}') == "$final_config|false" ]] ||
  fail CONFIG_MOUNT
# allowlist 掛載 + 內容（Talend 兩台測試機必須存在）
[[ $(docker inspect "$edge" --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/allowlist"}}{{.Source}}|{{.RW}}{{end}}{{end}}') == '/opt/jee8pay-v2-dev/edge-allowlist|false' ]] ||
  fail ALLOWLIST_MOUNT
[[ $(docker exec "$edge" grep -Fc 'allow 34.92.245.74;' /etc/nginx/allowlist/uat.conf) -eq 1 ]] ||
  fail ALLOWLIST_PRIMARY
[[ $(docker exec "$edge" grep -Fc 'allow 34.92.52.162;' /etc/nginx/allowlist/uat.conf) -eq 1 ]] ||
  fail ALLOWLIST_SECONDARY

compose_files=$(docker inspect "$edge" --format '{{index .Config.Labels "com.docker.compose.project.config_files"}}')
[[ $compose_files == *"$overlay"* ]] || fail COMPOSE_PROVENANCE
network_json=$(docker inspect "$edge" --format '{{json .NetworkSettings.Networks}}')
network_names=$(jq -r 'keys | sort | join(",")' <<<"$network_json")
[[ $network_names == 'jee8pay-v2-dev-edge-transit,nnviopp-sandbox_edge,nnviopp-sandbox_edge-public' ]] ||
  fail NETWORK_SET
while read -r network_id; do
  docker network inspect "$network_id" >/dev/null 2>&1 || fail NETWORK_ID
done < <(jq -r 'to_entries[].value.NetworkID' <<<"$network_json")

healthcheck=$(docker inspect "$edge" --format '{{json .Config.Healthcheck.Test}}')
for required in 'nginx -t' ':0050' ':01BB' 'jee8pay-v2-callback:8080/healthz' 'jee8pay-v2-merchant-api:8080/healthz'; do
  [[ $healthcheck == *"$required"* ]] || fail HEALTHCHECK_CONTRACT
done
docker exec "$edge" nginx -t >/dev/null 2>&1 || fail NGINX_CONFIG
[[ $(docker exec "$edge" wget -q -O - http://127.0.0.1:80/edge-health) == OK ]] || fail EDGE_LOCAL_ROUTE
[[ $(docker exec "$edge" wget -q -O - http://jee8pay-v2-callback:8080/healthz) == OK ]] ||
  fail CALLBACK_UPSTREAM
[[ $(docker exec "$edge" wget -q -O - http://jee8pay-v2-merchant-api:8080/healthz) == OK ]] ||
  fail MERCHANT_UPSTREAM

ss -H -lnt | grep -Fq "$sandbox_ip:80 " || fail PORT_80
ss -H -lnt | grep -Fq "$sandbox_ip:443 " || fail PORT_443

v2_healthy=$(docker ps --filter label=com.docker.compose.project=jee8pay-v2-dev \
  --filter health=healthy --format '{{.Names}}' | wc -l)
[[ $v2_healthy -eq 11 ]] || fail V2_CORE_HEALTH
[[ $(docker inspect nnviopp-sandbox-api --format '{{.State.Status}}|{{.State.Health.Status}}') == 'running|healthy' ]] ||
  fail V1_BACKEND_HEALTH

[[ $(grep -Fc 'location = /api/pay/unifiedOrder {' "$final_config") -eq 1 ]] || fail CREATE_ROUTE
[[ $(grep -Fc 'location = /api/pay/query {' "$final_config") -eq 1 ]] || fail QUERY_ROUTE
[[ $(grep -Fc 'location = /api/pay/notify/ryo {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_RYO
[[ $(grep -Fc 'location = /api/pay/notify/jay {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_JAY
[[ $(grep -Fc 'location = /api/pay/notify/chi {' "$final_config") -eq 1 ]] || fail CALLBACK_ROUTE_CHI
[[ $(grep -Fc 'allow 34.92.245.74;' "$final_config") -eq 2 ]] || fail ALLOWLIST_PRIMARY
[[ $(grep -Fc 'allow 34.92.52.162;' "$final_config") -eq 2 ]] || fail ALLOWLIST_SECONDARY
! grep -Fq '35.220.239.87' "$final_config" || fail PRODUCTION_IP_PRESENT

{
  printf 'VALIDATED_AT=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'HOSTNAME=%s\n' "$(hostname)"
  printf 'EDGE_STATE=%s\n' "$edge_state"
  printf 'EDGE_RESTART_COUNT=%s\n' "$(docker inspect "$edge" --format '{{.RestartCount}}')"
  printf 'EDGE_STARTED_AT=%s\n' "$(docker inspect "$edge" --format '{{.State.StartedAt}}')"
  printf 'RESTART_POLICY=unless-stopped\n'
  printf 'ACTIVE_CONFIG_SHA256=%s\n' "$expected_config_sha"
  printf 'CONFIG_SOURCE=%s\n' "$final_config"
  printf 'CONFIG_OWNER_MODE=0:10002:640\n'
  printf 'OVERLAY=%s\n' "$overlay"
  printf 'OVERLAY_SHA256=%s\n' "$expected_overlay_sha"
  printf 'OVERLAY_OWNER_MODE=0:0:600\n'
  printf 'COMPOSE_CONFIG_FILES=%s\n' "$compose_files"
  printf 'NETWORKS=%s\n' "$network_names"
  printf 'EPHEMERAL_NETWORK_ID_DEPENDENCY=0\n'
  printf 'PORT_80=LISTENING\n'
  printf 'PORT_443=LISTENING\n'
  printf 'NGINX_CONFIG=PASS\n'
  printf 'EDGE_LOCAL_ROUTE=PASS\n'
  printf 'CALLBACK_UPSTREAM=PASS\n'
  printf 'MERCHANT_UPSTREAM=PASS\n'
  printf 'V2_CORE_HEALTH=%s/11\n' "$v2_healthy"
  printf 'V1_BACKEND_HEALTH=PASS\n'
  printf 'V2_CREATE_ROUTE=PASS\n'
  printf 'V2_QUERY_ROUTE=PASS\n'
  printf 'RYO_JAY_CHI_CALLBACK_ROUTE=PASS\n'
  printf 'ALLOWLIST_PRIMARY=34.92.245.74\n'
  printf 'ALLOWLIST_SECONDARY=34.92.52.162\n'
  printf 'PRODUCTION_IP_IN_ALLOWLIST=NO\n'
  printf 'VALIDATION=PASS\n'
} >"$evidence_file"

printf 'VALIDATION=PASS\n'
printf 'EVIDENCE_FILE=%s\n' "$evidence_file"
sha256sum "$evidence_file"
