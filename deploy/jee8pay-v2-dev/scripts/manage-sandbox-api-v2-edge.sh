#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo 'usage: manage-sandbox-api-v2-edge.sh <plan|apply> <dns-only|proxied> [none|IPv4]' >&2
  echo '       manage-sandbox-api-v2-edge.sh rollback <backup-directory>' >&2
  exit 2
fi

readonly action=$1
readonly argument=$2
readonly query_exception=${3:-none}
readonly edge=nnviopp-sandbox-edge
readonly expected_host=server1.nnviopp.com
readonly deploy_root=/opt/jee8pay-v2-dev
readonly renderer="$deploy_root/merchant-uat/prepare-edge-nginx.py"
readonly active_config="$deploy_root/merchant-uat/nginx.proposed.conf"
readonly pre_sha=88f89d370c65b936ce0997e2088e2c6f71c11fdab338cd6ba21058c7274191dc

fail() {
  printf 'EDGE_CHANGE=FAIL_%s\n' "$1" >&2
  exit 2
}

[[ $EUID -eq 0 ]] || fail REQUIRES_ROOT
[[ $(hostname) == "$expected_host" ]] || fail WRONG_HOST
[[ $action == plan || $action == apply || $action == rollback ]] || fail ACTION

validate_candidate() {
  local candidate_path=$1
  if ! (
    set -e
    validation_name="jee-cf01-nginx-test-$$"
    cleanup_validation() { docker rm -f "$validation_name" >/dev/null 2>&1 || true; }
    trap cleanup_validation EXIT
    docker create --name "$validation_name" --user 0 \
      --network jee8pay-v2-dev-edge-transit \
      -v "$candidate_path:/etc/nginx/nginx.conf:ro" \
      -v /etc/nnviopp-sandbox/edge-tls:/etc/nginx/tls:ro \
      "$(docker inspect "$edge" --format '{{.Config.Image}}')" nginx -t >/dev/null
    docker network connect nnviopp-sandbox_edge "$validation_name"
    docker network connect nnviopp-sandbox_edge-public "$validation_name"
    docker start -a "$validation_name" >/dev/null
  ); then
    fail CANDIDATE_NGINX_TEST
  fi
}

if [[ $action == rollback ]]; then
  readonly backup_dir=$argument
  [[ ${SANDBOX_API_V2_EDGE_ROLLBACK_APPROVED:-} == YES ]] || fail ROLLBACK_APPROVAL_REQUIRED
  [[ $backup_dir == "$deploy_root/state/cf01-"* && -d $backup_dir ]] || fail BACKUP_DIRECTORY
  readonly backup="$backup_dir/outer-nginx.before.conf"
  [[ -f $backup ]] || fail BACKUP_MISSING
  validate_candidate "$backup"
  inode_before=$(stat -c '%i' "$active_config")
  cp -- "$backup" "$active_config"
  chown 0:10002 "$active_config"
  chmod 0640 "$active_config"
  [[ $(stat -c '%i' "$active_config") == "$inode_before" ]] || fail ACTIVE_INODE_CHANGED
  docker exec "$edge" nginx -t >/dev/null 2>&1 || fail ACTIVE_NGINX_TEST
  docker exec "$edge" nginx -s reload
  [[ $(docker exec "$edge" sha256sum /etc/nginx/nginx.conf | awk '{print $1}') == "$(sha256sum "$backup" | awk '{print $1}')" ]] || fail RUNTIME_READBACK
  printf 'EDGE_ROLLBACK=PASS\nACTIVE_SHA256=%s\n' "$(sha256sum "$active_config" | awk '{print $1}')"
  exit 0
fi

readonly origin_mode=$argument
[[ $origin_mode == dns-only || $origin_mode == proxied ]] || fail ORIGIN_MODE
readonly temporary_dir=$(mktemp -d /run/jee-cf01-edge.XXXXXX)
trap 'rm -rf -- "$temporary_dir"' EXIT
readonly candidate="$temporary_dir/nginx.candidate.conf"
python3 "$renderer" --origin-mode "$origin_mode" --query-exception "$query_exception" --output "$candidate"
validate_candidate "$candidate"
candidate_sha=$(sha256sum "$candidate" | awk '{print $1}')
active_sha=$(sha256sum "$active_config" | awk '{print $1}')
printf 'EDGE_PLAN=PASS\nORIGIN_MODE=%s\nQUERY_EXCEPTION=%s\nCURRENT_SHA256=%s\nCANDIDATE_SHA256=%s\n' \
  "$origin_mode" "$query_exception" "$active_sha" "$candidate_sha"

[[ $action == apply ]] || exit 0
[[ ${SANDBOX_API_V2_EDGE_CHANGE_APPROVED:-} == YES ]] || fail APPLY_APPROVAL_REQUIRED
if [[ $origin_mode == dns-only ]]; then
  [[ $active_sha == "$pre_sha" ]] || fail DNS_ONLY_REQUIRES_FROZEN_BASELINE
fi

readonly stamp=$(date -u +%Y%m%dT%H%M%SZ)
readonly backup_dir="$deploy_root/state/cf01-${stamp}-edge-apply"
install -d -m 0700 -o root -g root "$backup_dir"
install -m 0600 -o root -g root "$active_config" "$backup_dir/outer-nginx.before.conf"
install -m 0600 -o root -g root "$candidate" "$backup_dir/outer-nginx.candidate.conf"
inode_before=$(stat -c '%i' "$active_config")
cp -- "$candidate" "$active_config"
chown 0:10002 "$active_config"
chmod 0640 "$active_config"
[[ $(stat -c '%i' "$active_config") == "$inode_before" ]] || fail ACTIVE_INODE_CHANGED
[[ $(sha256sum "$active_config" | awk '{print $1}') == "$candidate_sha" ]] || fail HOST_READBACK
if ! docker exec "$edge" nginx -t >/dev/null 2>&1; then
  cp -- "$backup_dir/outer-nginx.before.conf" "$active_config"
  chown 0:10002 "$active_config"
  chmod 0640 "$active_config"
  fail ACTIVE_NGINX_TEST_RESTORED_FILE
fi
docker exec "$edge" nginx -s reload
[[ $(docker exec "$edge" sha256sum /etc/nginx/nginx.conf | awk '{print $1}') == "$candidate_sha" ]] || fail RUNTIME_READBACK
[[ $(docker inspect "$edge" --format '{{.State.Status}}|{{.State.Health.Status}}') == running\|healthy ]] || fail EDGE_HEALTH
printf '%s  %s\n' "$candidate_sha" "$active_config" >"$backup_dir/APPLIED.sha256"
chmod 0600 "$backup_dir/APPLIED.sha256"
printf 'EDGE_APPLY=PASS\nBACKUP_DIR=%s\nACTIVE_SHA256=%s\nEDGE_RECREATED=NO\n' \
  "$backup_dir" "$candidate_sha"
