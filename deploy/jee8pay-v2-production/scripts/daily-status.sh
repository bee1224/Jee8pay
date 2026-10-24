#!/usr/bin/env bash
# 每日狀態報備（root cron 每日 00:00，與收款總結同時）。只有正式機發送；PEER_ONLY=1 的主機（測試機）直接結束。
# 彙整主機、容器、主機服務、對外入口與憑證、對端主機、監控本身、資源警戒與過去 24 小時事件。
# 報備訊息記入服務看門狗的 sent 檔，由看門狗在 24 小時後自動刪除（隔天的報備取代前一天）。
set -uo pipefail

[[ -r ${WATCHDOG_CONFIG:-/etc/jee8pay-watchdog.conf} ]] && . "${WATCHDOG_CONFIG:-/etc/jee8pay-watchdog.conf}"
[[ ${PEER_ONLY:-0} == 1 ]] && exit 0

readonly projects=${WATCH_PROJECTS:-jee8pay-v2-production=/opt/jee8pay-v2-production/current/compose.yml}
readonly secret_dir=${V2_SECRET_DIR:-/opt/jee8pay-v2-production/secrets}
readonly edge_key=${EDGE_KEY:-jee8pay-v2-production/edge}
readonly listen_ip=${EDGE_LISTEN_IP:-162.0.233.203}
readonly edge_cert=${EDGE_CERT:-/etc/lp33ing-production/edge-tls/fullchain.pem}
readonly host_units=${HOST_UNITS:-docker containerd cron ssh fail2ban ufw qemu-guest-agent}
readonly peer_name=${PEER_NAME:-測試機}
readonly peer_host=${PEER_HOST:-159.198.40.128}
# 有收款總結的主機才檢查（測試機設定檔設為空）
readonly summary_container=${SUMMARY_CONTAINER-jee8pay-v2-production-manager-1}
readonly chat_id=${TELEGRAM_CHAT_ID:--5211710664}
readonly token_file=${TELEGRAM_TOKEN_FILE:-/opt/jee8pay-v2-production/secrets/telegram-bot-token}
readonly wd_state=${WATCHDOG_STATE_DIR:-/var/lib/jee8pay-service-watchdog}
readonly rm_state=${RESOURCE_MONITOR_STATE_DIR:-/var/lib/jee8pay-resource-monitor}
readonly wd_log=/var/log/jee8pay-service-watchdog.log
readonly rm_log=/var/log/jee8pay-resource-monitor.log

log() { echo "$(date -Is) $*"; }
[[ $EUID -eq 0 ]] || { echo 'run as root' >&2; exit 1; }
host="${HOST_LABEL:-正式機} $(hostname)"
# 讓同時間的收款總結先送出
[[ ${1:-} == now ]] || sleep "${DAILY_STATUS_DELAY:-30}"

ok() { [[ $1 == 0 ]] && echo '✅' || echo '⚠️'; }
pct() { awk -v a="$1" -v b="$2" 'BEGIN { printf "%d", (b > 0 ? a * 100 / b : 0) }'; }

# ---- 主機 ----
boot=$(uptime -s)
up_s=$(( $(date +%s) - $(date -d "$boot" +%s) ))
mem_total=$(awk '/^MemTotal:/{print $2}' /proc/meminfo)
mem_avail=$(awk '/^MemAvailable:/{print $2}' /proc/meminfo)
disk=$(df -P / | awk 'NR==2{print $5}')
host_line="主機：已運行 $((up_s / 86400)) 天 $((up_s % 86400 / 3600)) 小時（$boot 開機）；load $(cut -d' ' -f1-3 /proc/loadavg)；記憶體 $(pct $((mem_total - mem_avail)) "$mem_total")%；磁碟 $disk"

# ---- 容器 ----
total=0 healthy=0 bad=()
for entry in $projects; do
  proj=${entry%%=*} file=${entry#*=}
  [[ $entry == *=* ]] || file=
  svcs=
  [[ -n $file ]] && svcs=$(env V2_SECRET_DIR="$secret_dir" docker compose -p "$proj" -f "$file" config --services 2>/dev/null)
  [[ -n $svcs ]] || svcs=$(docker ps -a --filter "label=com.docker.compose.project=$proj" --format '{{.Label "com.docker.compose.service"}}')
  for s in $svcs; do
    c=$(docker ps -aq --filter "label=com.docker.compose.project=$proj" --filter "label=com.docker.compose.service=$s" | head -1)
    if [[ -z $c ]]; then total=$((total + 1)); bad+=("$s(不存在)"); continue; fi
    [[ $(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$c") =~ ^(no|)$ ]] && continue
    total=$((total + 1))
    st=$(docker inspect -f '{{.State.Status}}/{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$c")
    if [[ $st == running/healthy || $st == running/none ]]; then healthy=$((healthy + 1)); else bad+=("$s($st)"); fi
  done
done
ctr_line="$(ok ${#bad[@]}) 容器：${healthy}/${total} 正常"
((${#bad[@]})) && ctr_line+="，異常：$(IFS='、'; echo "${bad[*]}")"

# ---- 主機服務 ----
units_total=0 units_down=()
for u in $host_units; do
  systemctl list-unit-files "$u.service" --no-legend 2>/dev/null | grep -q . || continue
  units_total=$((units_total + 1))
  systemctl is-active --quiet "$u" || units_down+=("$u")
done
unit_line="$(ok ${#units_down[@]}) 主機服務：$((units_total - ${#units_down[@]}))/${units_total} 執行中"
((${#units_down[@]})) && unit_line+="，未執行：$(IFS='、'; echo "${units_down[*]}")"

# ---- 對外入口與憑證 ----
edge_bad=0
ss -ltnH 'sport = :443' | grep -q "$listen_ip:443" || edge_bad=1
[[ $(curl -s -m 8 "http://$listen_ip/edge-health") == OK ]] || edge_bad=1
cert_days=?
if end=$(openssl x509 -enddate -noout -in "$edge_cert" 2>/dev/null); then
  cert_days=$(( ($(date -d "${end#notAfter=}" +%s) - $(date +%s)) / 86400 ))
  ((cert_days < 14)) && edge_bad=1
fi
edge_line="$(ok $edge_bad) 對外入口：$([[ $edge_bad == 0 ]] && echo 正常 || echo 異常)（$listen_ip:443），憑證剩 ${cert_days} 天"

# ---- 對端主機 ----
peer_line=
if [[ -n $peer_host ]]; then
  if [[ $(curl -s -m 8 "http://$peer_host/edge-health") == OK ]]; then
    peer_line="✅ ${peer_name}（$peer_host）：連線正常"
  else
    peer_line="⚠️ ${peer_name}（$peer_host）：無回應"
  fi
fi

# ---- 監控本身 ----
fresh() { [[ -n $(find "$1" -maxdepth 0 -mmin -5 2>/dev/null) ]]; }
mon_bad=0 mon=()
if fresh "$wd_state/boot_id"; then mon+=('看門狗 ✓'); else mon+=('看門狗 ✗'); mon_bad=1; fi
if fresh "$rm_state/cpu.streak"; then mon+=('資源警戒 ✓'); else mon+=('資源警戒 ✗'); mon_bad=1; fi
if systemctl is-active --quiet jee8pay-shutdown-notify; then mon+=('關機通知 ✓'); else mon+=('關機通知 ✗'); mon_bad=1; fi
mon_line="$(ok $mon_bad) 監控：$(IFS='、'; echo "${mon[*]}")"

# ---- 資源警戒中 ----
alerts=()
for f in "$rm_state"/*; do
  n=$(basename "$f")
  [[ $n == sent || $n == *.streak ]] && continue
  alerts+=("${n#ctr-}")
done
if ((${#alerts[@]})); then
  res_line="⚠️ 資源警戒中（曾達 70%，尚未回落至 65% 以下）：$(IFS='、'; echo "${alerts[*]}" | sed "s/${CONTAINER_PREFIX:-jee8pay-v2-production}-//g")"
else
  res_line='✅ 資源：無項目超過 70%'
fi

# ---- 過去 24 小時 ----
since=$(date -d '-24 hours' +%s)
count_since() { [[ -r $1 ]] || { echo 0; return; }
  awk -v s="$since" '/ notify: / { cmd = "date -d \"" $1 "\" +%s"; cmd | getline t; close(cmd); if (t >= s) n++ } END { print n + 0 }' "$1"; }
reboots=$(journalctl --list-boots --no-pager 2>/dev/null | awk -v s="$(date -d '-24 hours' '+%F %T')" 'NR>1 { if (($4 " " $5) >= s) n++ } END { print n + 0 }')
events_line="過去 24 小時：看門狗通知 $(count_since "$wd_log") 則，重開機 ${reboots} 次"

# ---- 收款總結（僅正式機）----
summary_line=
if [[ -n $summary_container ]]; then
  logs=$(docker logs --since 25h "$summary_container" 2>&1)
  sent=$(grep -c 'daily payment summary sent' <<<"$logs")
  deleted=$(grep -c 'daily payment summary deleted' <<<"$logs")
  failed=$(grep -c 'daily payment summary delete failed' <<<"$logs")
  summary_line="$( ((sent > 0 && failed == 0)) && echo ✅ || echo ⚠️) 收款總結：24 小時內送出 ${sent} 則、自動刪除 ${deleted} 則$( ((failed)) && echo "、刪除失敗 ${failed} 則")"
  ((sent == 0)) && summary_line+="（若 manager 在 24 小時內重啟過，log 可能不完整）"
fi

msg="📋 [$host] 每日狀態報備 $(date '+%F %H:%M')
$host_line
$ctr_line
$unit_line
$edge_line"
[[ -n $peer_line ]] && msg+=$'\n'"$peer_line"
msg+=$'\n'"$mon_line"$'\n'"$res_line"
[[ -n $summary_line ]] && msg+=$'\n'"$summary_line"
msg+=$'\n'"$events_line"

# DRY_RUN=1：只印出報備內容，不發送
[[ -n ${DRY_RUN:-} ]] && { echo "$msg"; exit 0; }

token=$(cat "$token_file" 2>/dev/null) || { log 'telegram token unreadable' >&2; exit 1; }
resp=$(printf 'url = "https://api.telegram.org/bot%s/sendMessage"\n' "$token" |
  curl -sS -m 15 -w '\n%{http_code}' -K - --data-urlencode "chat_id=$chat_id" --data-urlencode "text=$msg" 2>/dev/null)
[[ ${resp##*$'\n'} == 200 ]] || { log "telegram send failed http=${resp##*$'\n'}"; exit 1; }
id=$(grep -o '"message_id":[0-9]*' <<<"$resp" | head -1 | cut -d: -f2)
# 交給服務看門狗在 24 小時後刪除
[[ -n $id ]] && install -d -m 0700 "$wd_state" && echo "$(date +%s) $id" >>"$wd_state/sent"
log "daily status sent"
