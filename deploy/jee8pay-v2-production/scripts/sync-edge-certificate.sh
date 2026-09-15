#!/usr/bin/env bash
# V2-owned certbot deploy hook：把 renewed sandbox edge cert 同步到
# /etc/lp33ing-production/edge-tls 並 HUP edge（V1 退役後由本檔取代
# /opt/payment/payment-service/payment-service/scripts/sync-production-edge-certificate.sh）。
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo 'run as root' >&2
  exit 1
fi

readonly live_dir=/etc/letsencrypt/live/jee8pay-v2-production-edge
readonly target_dir=/etc/lp33ing-production/edge-tls
readonly edge_container=jee8pay-v2-production-edge

test -r "$live_dir/fullchain.pem"
test -r "$live_dir/privkey.pem"

install -d -o root -g 10002 -m 0750 "$target_dir"
install -o root -g 10002 -m 0640 "$live_dir/fullchain.pem" "$target_dir/fullchain.pem"
install -o root -g 10002 -m 0640 "$live_dir/privkey.pem" "$target_dir/privkey.pem"

if docker inspect "$edge_container" >/dev/null 2>&1 &&
  [[ $(docker inspect --format '{{.State.Running}}' "$edge_container") == true ]]; then
  docker kill --signal HUP "$edge_container" >/dev/null
fi

stat -c '%a %U:%G %n' "$target_dir/fullchain.pem" "$target_dir/privkey.pem"
