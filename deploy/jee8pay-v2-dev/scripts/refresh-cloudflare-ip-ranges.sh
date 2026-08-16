#!/usr/bin/env bash
set -euo pipefail

[[ $# -eq 1 && ( $1 == check || $1 == render ) ]] || {
  echo 'usage: refresh-cloudflare-ip-ranges.sh <check|render>' >&2
  exit 2
}

readonly action=$1
readonly root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
readonly v4_target="$root_dir/merchant-uat/cloudflare-ips-v4.txt"
readonly v6_target="$root_dir/merchant-uat/cloudflare-ips-v6.txt"
readonly temporary_dir=$(mktemp -d /tmp/jee-cf01-cloudflare-ranges.XXXXXX)
trap 'rm -rf -- "$temporary_dir"' EXIT

curl -fsS --max-time 20 --retry 0 https://www.cloudflare.com/ips-v4 -o "$temporary_dir/ips-v4"
curl -fsS --max-time 20 --retry 0 https://www.cloudflare.com/ips-v6 -o "$temporary_dir/ips-v6"

python3 - "$temporary_dir/ips-v4" "$temporary_dir/ips-v6" <<'PY'
import ipaddress
import pathlib
import sys

for version, name in ((4, sys.argv[1]), (6, sys.argv[2])):
    lines = [line.strip() for line in pathlib.Path(name).read_text().splitlines() if line.strip()]
    networks = [ipaddress.ip_network(line, strict=True) for line in lines]
    if not lines or len(lines) != len(set(lines)):
        raise SystemExit(f"IPV{version}_RANGES_EMPTY_OR_DUPLICATE")
    if any(network.version != version or network.prefixlen == 0 for network in networks):
        raise SystemExit(f"IPV{version}_RANGE_INVALID")
PY

fresh_v4=$(sed '/^[[:space:]]*$/d' "$temporary_dir/ips-v4")
fresh_v6=$(sed '/^[[:space:]]*$/d' "$temporary_dir/ips-v6")
pinned_v4=$(sed '/^[[:space:]]*#/d;/^[[:space:]]*$/d' "$v4_target")
pinned_v6=$(sed '/^[[:space:]]*#/d;/^[[:space:]]*$/d' "$v6_target")
v4_hash=$(sha256sum "$temporary_dir/ips-v4" | awk '{print $1}')
v6_hash=$(sha256sum "$temporary_dir/ips-v6" | awk '{print $1}')

if [[ $action == check ]]; then
  [[ $fresh_v4 == "$pinned_v4" ]] || { echo 'CLOUDFLARE_IPV4_RANGES=DRIFT'; exit 1; }
  [[ $fresh_v6 == "$pinned_v6" ]] || { echo 'CLOUDFLARE_IPV6_RANGES=DRIFT'; exit 1; }
  printf 'CLOUDFLARE_IP_RANGES=PASS\nIPV4_SHA256=%s\nIPV6_SHA256=%s\n' "$v4_hash" "$v6_hash"
  exit 0
fi

[[ ${CLOUDFLARE_IP_RANGE_UPDATE_APPROVED:-} == YES ]] || {
  echo 'CLOUDFLARE_IP_RANGES=FAIL_EXPLICIT_APPROVAL_REQUIRED' >&2
  exit 2
}
retrieved_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
for version in 4 6; do
  source_file="$temporary_dir/ips-v$version"
  target_var="v${version}_target"
  target_file=${!target_var}
  hash_var="v${version}_hash"
  raw_hash=${!hash_var}
  output="$temporary_dir/cloudflare-ips-v$version.txt"
  printf '# source=https://www.cloudflare.com/ips-v%s\n# retrieved_at=%s\n# raw_sha256=%s\n' \
    "$version" "$retrieved_at" "$raw_hash" >"$output"
  sed '/^[[:space:]]*$/d' "$source_file" >>"$output"
  printf '\n' >>"$output"
  install -m 0644 "$output" "$target_file"
done
printf 'CLOUDFLARE_IP_RANGES=UPDATED\nIPV4_SHA256=%s\nIPV6_SHA256=%s\n' "$v4_hash" "$v6_hash"
