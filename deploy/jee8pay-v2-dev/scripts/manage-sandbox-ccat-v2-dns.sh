#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo 'usage: manage-sandbox-ccat-v2-dns.sh <plan|apply|rollback> <cloudflare-token-ini> <backup-json>' >&2
  exit 2
fi

readonly action=$1
readonly token_file=$2
readonly backup_file=$3
readonly zone_name=nnviopp.com
readonly record_name=ccat-v2-dev.nnviopp.com
readonly record_ip=159.198.40.128
readonly api_root=https://api.cloudflare.com/client/v4

[[ $action == plan || $action == apply || $action == rollback ]] || {
  echo 'action must be plan, apply, or rollback' >&2
  exit 2
}

fail() {
  echo "Sandbox CCAT V2 DNS operation failed: $*" >&2
  exit 1
}

[[ $(id -u) -eq 0 ]] || fail 'must run as root'
[[ -f $token_file ]] || fail 'Cloudflare token file is missing'
[[ $(stat -c '%u:%g:%a' "$token_file") == '0:0:600' ]] ||
  fail 'Cloudflare token file must be root:root mode 0600'
command -v curl >/dev/null || fail 'curl is required'
command -v jq >/dev/null || fail 'jq is required'

token=$(sed -n 's/^[[:space:]]*dns_cloudflare_api_token[[:space:]]*=[[:space:]]*//p' "$token_file")
[[ -n $token && $token != *$'\n'* ]] || fail 'token file must contain exactly one token value'

readonly temporary_dir=$(mktemp -d /run/jee8pay-v2-dns.XXXXXX)
readonly auth_header=$temporary_dir/authorization.header
cleanup() {
  rm -rf -- "$temporary_dir"
}
trap cleanup EXIT
umask 077
printf 'Authorization: Bearer %s\n' "$token" >"$auth_header"
unset token

api() {
  curl -fsS --max-time 20 --retry 0 \
    --header @"$auth_header" \
    --header 'Content-Type: application/json' \
    "$@"
}

zone_response=$temporary_dir/zone.json
api "$api_root/zones?name=$zone_name&status=active" >"$zone_response"
jq -e '.success == true and (.result | length) == 1' "$zone_response" >/dev/null ||
  fail 'expected exactly one active Sandbox zone'
zone_id=$(jq -r '.result[0].id' "$zone_response")
[[ $zone_id =~ ^[0-9a-f]{32}$ ]] || fail 'zone ID is invalid'

record_response=$temporary_dir/record.json
api "$api_root/zones/$zone_id/dns_records?type=A&name=$record_name" >"$record_response"
jq -e '.success == true and (.result | length) <= 1' "$record_response" >/dev/null ||
  fail 'target hostname has an ambiguous record set'
record_count=$(jq '.result | length' "$record_response")

if [[ $action == plan ]]; then
  if [[ $record_count -eq 0 ]]; then
    echo "dns_plan name=$record_name action=create target=$record_ip proxied=false ttl=300"
  else
    jq -r '.result[0] | "dns_plan name=" + .name + " action=blocked_existing content=" + .content + " proxied=" + (.proxied|tostring) + " ttl=" + (.ttl|tostring)' "$record_response"
  fi
  exit 0
fi

if [[ $action == apply ]]; then
  [[ ${SANDBOX_CCAT_V2_DNS_CHANGE_APPROVED:-} == YES ]] ||
    fail 'set SANDBOX_CCAT_V2_DNS_CHANGE_APPROVED=YES only after explicit approval'
  [[ $record_count -eq 0 ]] || fail 'target hostname already exists; refusing to overwrite it'
  [[ ! -e $backup_file ]] || fail 'backup path already exists; refusing to overwrite it'
  backup_dir=$(dirname -- "$backup_file")
  [[ -d $backup_dir ]] || fail 'backup directory does not exist'
  backup_temp=$(mktemp "$backup_dir/.ccat-v2-dns-backup.XXXXXX")
  jq --arg zone_id "$zone_id" --arg name "$record_name" \
    '{zone_id:$zone_id,captured_at:(now|todateiso8601),name:$name,records:.result}' \
    "$record_response" >"$backup_temp"
  chmod 0600 "$backup_temp"
  mv -- "$backup_temp" "$backup_file"

  payload=$temporary_dir/apply-payload.json
  result=$temporary_dir/apply-result.json
  jq -n --arg name "$record_name" --arg content "$record_ip" \
    '{type:"A",name:$name,content:$content,ttl:300,proxied:false}' >"$payload"
  api --request POST --data @"$payload" \
    "$api_root/zones/$zone_id/dns_records" >"$result"
  jq -e --arg name "$record_name" --arg content "$record_ip" \
    '.success == true and .result.type == "A" and .result.name == $name and .result.content == $content and .result.proxied == false and .result.ttl == 300' \
    "$result" >/dev/null || fail 'record was not created exactly'
  echo "dns_applied name=$record_name target=$record_ip proxied=false ttl=300"
  echo "dns_backup=$backup_file"
  exit 0
fi

[[ ${SANDBOX_CCAT_V2_DNS_ROLLBACK_APPROVED:-} == YES ]] ||
  fail 'set SANDBOX_CCAT_V2_DNS_ROLLBACK_APPROVED=YES after selecting the exact backup'
[[ -f $backup_file ]] || fail 'backup file is missing'
[[ $(stat -c '%u:%g:%a' "$backup_file") == '0:0:600' ]] ||
  fail 'backup file must be root:root mode 0600'
jq -e --arg zone_id "$zone_id" --arg name "$record_name" \
  '.zone_id == $zone_id and .name == $name and (.records | length) == 0' \
  "$backup_file" >/dev/null || fail 'backup is not the expected absent-record state'
[[ $record_count -eq 1 ]] || fail 'rollback expected exactly one current record'
jq -e --arg name "$record_name" --arg content "$record_ip" \
  '.result[0].name == $name and .result[0].content == $content and .result[0].proxied == false' \
  "$record_response" >/dev/null || fail 'current record is not the E02-created record'
record_id=$(jq -r '.result[0].id' "$record_response")
result=$temporary_dir/rollback-result.json
api --request DELETE "$api_root/zones/$zone_id/dns_records/$record_id" >"$result"
jq -e '.success == true' "$result" >/dev/null || fail 'rollback delete failed'
echo "dns_rolled_back name=$record_name state=absent"
