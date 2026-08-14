#!/usr/bin/env bash
set -euo pipefail

readonly edge=nnviopp-sandbox-edge
readonly sandbox_ip=159.198.40.128
readonly evidence_dir=/opt/jee8pay-v2-dev/state/n01
readonly evidence_file="$evidence_dir/forensic-before.txt"
readonly baseline_config=/opt/payment/payment-service-sandbox/edge/nginx.conf
readonly callback_config=/opt/jee8pay-v2-dev/public-callback/nginx.proposed.conf
readonly final_config=/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf

[[ $EUID -eq 0 ]] || {
  echo 'CAPTURE=FAIL_REQUIRES_ROOT' >&2
  exit 2
}

install -d -m 0700 -o root -g root "$evidence_dir"
install -m 0600 -o root -g root /dev/null "$evidence_file"

{
  printf 'CAPTURED_AT=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'HOSTNAME=%s\n' "$(hostname)"
  printf 'EDGE_CONTAINER=%s\n' "$edge"
  docker ps -a --filter "name=^/${edge}$" \
    --format 'EDGE_PS={{.ID}}|{{.Names}}|{{.Status}}|{{.Image}}'
  docker inspect "$edge" --format \
    'EDGE_STATE={{.State.Status}}|EXIT={{.State.ExitCode}}|STARTED={{.State.StartedAt}}|FINISHED={{.State.FinishedAt}}'
  docker inspect "$edge" --format \
    'RESTART_POLICY={{.HostConfig.RestartPolicy.Name}}|MAX_RETRY={{.HostConfig.RestartPolicy.MaximumRetryCount}}'
  docker inspect "$edge" --format \
    'COMPOSE_CONFIG_FILES={{index .Config.Labels "com.docker.compose.project.config_files"}}'
  docker inspect "$edge" --format 'MOUNTS={{json .Mounts}}'
  docker inspect "$edge" --format 'NETWORKS={{json .NetworkSettings.Networks}}'
  docker inspect "$edge" --format 'PORT_BINDINGS={{json .HostConfig.PortBindings}}'
  docker inspect "$edge" --format 'HEALTHCHECK={{json .Config.Healthcheck}}'

  printf 'STOPPED_CONTAINER_CONFIG_SHA256='
  docker cp "$edge:/etc/nginx/nginx.conf" - | tar -xO | sha256sum | awk '{print $1}'

  for config in "$baseline_config" "$callback_config" "$final_config"; do
    if [[ -f $config ]]; then
      stat -c 'CONFIG=%n|OWNER=%U:%G|MODE=%a|SIZE=%s' "$config"
      sha256sum "$config"
    else
      printf 'CONFIG_MISSING=%s\n' "$config"
    fi
  done

  printf 'SANDBOX_LISTENERS_BEGIN\n'
  ss -lntp | awk -v ip="$sandbox_ip" '$4 ~ ("^" ip ":(80|443)$")'
  printf 'SANDBOX_LISTENERS_END\n'

  printf 'DOCKER_NETWORKS_BEGIN\n'
  docker network ls --no-trunc --format '{{.ID}}|{{.Name}}|{{.Driver}}|{{.Scope}}'
  printf 'DOCKER_NETWORKS_END\n'

  printf 'TRANSIT_NETWORK='
  docker network inspect jee8pay-v2-dev-edge-transit --format \
    '{{.Name}}|{{.Id}}|{{.Created}}|{{.Driver}}|INTERNAL={{.Internal}}|LABELS={{json .Labels}}'

  printf 'RESTORE_FAILURE_BEGIN\n'
  journalctl -u docker.service --since '2026-08-14 00:00:00 UTC' --no-pager |
    grep -E 'nnviopp-sandbox-edge|Failed to restore endpoint|network .* not found' || true
  printf 'RESTORE_FAILURE_END\n'

  printf 'TEMP_NETWORK_HISTORY_BEGIN\n'
  journalctl -u docker.service --since '2026-08-12 20:00:00 UTC' --no-pager |
    grep -E 'nnviopp-sandbox-edge.*jee-d01-route-test|jee-d01-route-test.*nnviopp-sandbox-edge|0f732423b0d1' || true
  printf 'TEMP_NETWORK_HISTORY_END\n'
} >"$evidence_file"

printf 'CAPTURE=PASS\n'
printf 'EVIDENCE_FILE=%s\n' "$evidence_file"
stat -c 'EVIDENCE_OWNER=%U:%G EVIDENCE_MODE=%a EVIDENCE_SIZE=%s' "$evidence_file"
sha256sum "$evidence_file"
