#!/usr/bin/env bash
# Production 服務看門狗（取代 edge-watchdog.sh）。
#   service-watchdog.sh            root cron 每分鐘 + 開機後執行：檢查 compose 全部服務與主機服務
#   service-watchdog.sh shutdown   由 jee8pay-shutdown-notify.service 在關機/重開機開始時執行
#
# 2026-10-24 主機被 hypervisor 關機重開後 edge 未自動啟動，停機 83 分鐘無人知道。
# - 容器停止：依相依順序自動 docker start（資料層先、edge 最後），通知結果
# - 容器連續 3 分鐘 unhealthy、主機服務未執行：通知
# - 偵測到重開機：發一則總結（上次是否正常關機、全部檢查點結果）
# 只在狀態改變時通知；通知 24 小時後自動刪除，事件紀錄保留在 log。
set -uo pipefail

readonly project=${COMPOSE_PROJECT:-jee8pay-v2-production}
readonly compose_file=${COMPOSE_FILE_PATH:-/opt/jee8pay-v2-production/current/compose.yml}
readonly secret_dir=${V2_SECRET_DIR:-/opt/jee8pay-v2-production/secrets}
readonly edge_service=edge
readonly listen_ip=${EDGE_LISTEN_IP:-162.0.233.203}
readonly host_units=${HOST_UNITS:-docker containerd cron ssh fail2ban ufw qemu-guest-agent}
# 啟動順序：資料層先、edge 最後，其餘照 compose 順序
readonly start_first="db redis mq-namesrv mq-broker"
readonly start_last="edge"
readonly chat_id=${TELEGRAM_CHAT_ID:--5211710664}
readonly token_file=${TELEGRAM_TOKEN_FILE:-/opt/jee8pay-v2-production/secrets/telegram-bot-token}
readonly state_dir=${WATCHDOG_STATE_DIR:-/var/lib/jee8pay-service-watchdog}
# 連續幾次不健康才告警，避開 reload / 剛啟動時的短暫狀態
readonly unhealthy_alert_after=3
# 通知發出後自動刪除的時間（Telegram 只允許 bot 刪除 48 小時內的訊息）
readonly auto_delete_seconds=$(( ${AUTO_DELETE_HOURS:-24} * 3600 ))

log() { echo "$(date -Is) $*"; }

[[ $EUID -eq 0 ]] || { echo 'run as root' >&2; exit 1; }
install -d -m 0700 "$state_dir" "$state_dir/svc" "$state_dir/unit"
host=$(hostname)

# tg <method> <curl 參數...>：輸出 response body，最後一行為 HTTP code
tg() {
  local method=$1 token
  shift
  token=$(cat "$token_file" 2>/dev/null) || { log 'telegram token unreadable'; return 1; }
  # token 經 stdin 交給 curl，不出現在 process list 或 log
  printf 'url = "https://api.telegram.org/bot%s/%s"\n' "$token" "$method" |
    curl -sS -m 10 -w '\n%{http_code}' -K - --data-urlencode "chat_id=$chat_id" "$@" 2>/dev/null
}

# 發出的通知記在 sent 檔（<epoch> <message_id>），滿 AUTO_DELETE_HOURS 後自動刪除
notify() {
  local resp id
  log "notify: $1"
  resp=$(tg sendMessage --data-urlencode "text=$1")
  [[ ${resp##*$'\n'} == 200 ]] || { log "telegram send failed http=${resp##*$'\n'}"; return 1; }
  id=$(grep -o '"message_id":[0-9]*' <<<"$resp" | head -1 | cut -d: -f2)
  [[ -n $id ]] && echo "$(date +%s) $id" >>"$state_dir/sent"
  return 0
}

purge_old_notices() {
  local f="$state_dir/sent" now keep ts id resp
  [[ -s $f ]] || return 0
  now=$(date +%s)
  keep=$(mktemp)
  while read -r ts id; do
    if ((now - ts < auto_delete_seconds)); then
      echo "$ts $id" >>"$keep"
      continue
    fi
    resp=$(tg deleteMessage --data-urlencode "message_id=$id")
    # 失敗且還在 Telegram 48 小時可刪範圍內就保留重試；超過就放棄（已刪不掉）
    if [[ ${resp##*$'\n'} != 200 ]] && ((now - ts < 47 * 3600)) && ! grep -q 'message to delete not found' <<<"$resp"; then
      echo "$ts $id" >>"$keep"
      log "delete message $id failed, will retry"
    fi
  done <"$f"
  mv "$keep" "$f"
}

# ---------------- 關機通知 ----------------
if [[ ${1:-} == shutdown ]]; then
  kind=關機
  systemctl list-jobs --no-legend 2>/dev/null | grep -q 'reboot.target' && kind=重新開機
  # logind 會記錄關機來源，例如「System is powering down (hypervisor initiated shutdown).」
  reason=$(journalctl -b --since '-3 min' --no-pager -o cat 2>/dev/null |
    grep -E 'System is (powering down|rebooting|halting)' | tail -1)
  date -Is >"$state_dir/clean-shutdown"
  notify "🔌 [$host] 主機正在${kind}（$(date '+%F %T')）。${reason:+系統訊息：$reason }開機後看門狗會回報各服務狀態。"
  exit 0
fi

purge_old_notices

# ---------------- 容器 ----------------
services=$(env V2_SECRET_DIR="$secret_dir" docker compose -p "$project" -f "$compose_file" config --services 2>/dev/null)
[[ -n $services ]] || services=$(docker ps -a --filter "label=com.docker.compose.project=$project" \
  --format '{{.Label "com.docker.compose.service"}}' 2>/dev/null)
ordered=()
for s in $start_first; do grep -qx "$s" <<<"$services" && ordered+=("$s"); done
for s in $services; do [[ " $start_first $start_last " == *" $s "* ]] || ordered+=("$s"); done
for s in $start_last; do grep -qx "$s" <<<"$services" && ordered+=("$s"); done

container_of() {
  docker ps -aq --filter "label=com.docker.compose.project=$project" \
    --filter "label=com.docker.compose.service=$1" 2>/dev/null | head -1
}

svc_status() {
  local c running health
  c=$(container_of "$1")
  [[ -n $c ]] || { echo missing; return; }
  # restart 政策為 no 的是一次性工作（例如 volume-init），不列入檢查
  [[ $(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$c") =~ ^(no|)$ ]] && { echo oneshot; return; }
  running=$(docker inspect -f '{{.State.Running}}' "$c")
  [[ $running == true ]] || { echo stopped; return; }
  health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$c")
  if [[ $health == unhealthy ]]; then echo unhealthy; return; fi
  if [[ $1 == "$edge_service" ]] && ! ss -ltnH 'sport = :443' | grep -q "$listen_ip:443"; then echo unhealthy; return; fi
  echo ok
}

started=() start_failed=() missing=() unhealthy_new=() recovered=() checked=0 healthy=0

# 先依順序啟動停止的容器，再統一複查
to_start=()
for s in "${ordered[@]}"; do
  [[ $(svc_status "$s") == stopped ]] && to_start+=("$s")
done
for s in "${to_start[@]}"; do
  log "service $s stopped, docker start"
  docker start "$(container_of "$s")" >/dev/null 2>&1
  sleep 3
done
((${#to_start[@]} > 0)) && sleep 30

for s in "${ordered[@]}"; do
  st=$(svc_status "$s")
  [[ $st == oneshot ]] && continue
  checked=$((checked + 1))
  prev=$(cat "$state_dir/svc/$s" 2>/dev/null || echo ok)
  was_started=false
  [[ " ${to_start[*]} " == *" $s "* ]] && was_started=true
  case $st in
    ok)
      healthy=$((healthy + 1))
      if $was_started; then started+=("$s"); elif [[ $prev == down ]]; then recovered+=("$s"); fi
      echo ok >"$state_dir/svc/$s"; echo 0 >"$state_dir/svc/$s.fails"
      ;;
    stopped)
      [[ $prev == down ]] || start_failed+=("$s")
      echo down >"$state_dir/svc/$s"
      ;;
    missing)
      [[ $prev == down ]] || missing+=("$s")
      echo down >"$state_dir/svc/$s"
      ;;
    unhealthy)
      fails=$(( $(cat "$state_dir/svc/$s.fails" 2>/dev/null || echo 0) + 1 ))
      echo "$fails" >"$state_dir/svc/$s.fails"
      if ((fails >= unhealthy_alert_after)) && [[ $prev != down ]]; then
        unhealthy_new+=("$s"); echo down >"$state_dir/svc/$s"
      fi
      ;;
  esac
done

# ---------------- 主機服務 ----------------
units_down=() units_recovered=() units_down_now=()
for u in $host_units; do
  systemctl list-unit-files "$u.service" --no-legend 2>/dev/null | grep -q . || continue
  prev=$(cat "$state_dir/unit/$u" 2>/dev/null || echo ok)
  if systemctl is-active --quiet "$u"; then
    [[ $prev == down ]] && units_recovered+=("$u")
    echo ok >"$state_dir/unit/$u"
  else
    units_down_now+=("$u")
    [[ $prev == down ]] || units_down+=("$u")
    echo down >"$state_dir/unit/$u"
  fi
done

join() { local IFS='、'; echo "$*"; }
lines=()
((${#started[@]})) && lines+=("🚨 容器已停止，已自動啟動：$(join "${started[@]}")")
((${#start_failed[@]})) && lines+=("🚨 容器停止且自動啟動失敗：$(join "${start_failed[@]}")，需要人工處理！")
((${#missing[@]})) && lines+=("🚨 找不到容器：$(join "${missing[@]}")，需要人工處理！")
((${#unhealthy_new[@]})) && lines+=("⚠️ 容器連續 ${unhealthy_alert_after} 分鐘不健康：$(join "${unhealthy_new[@]}")")
((${#units_down[@]})) && lines+=("⚠️ 主機服務未執行：$(join "${units_down[@]}")")
((${#recovered[@]})) && lines+=("✅ 容器已恢復：$(join "${recovered[@]}")")
((${#units_recovered[@]})) && lines+=("✅ 主機服務已恢復：$(join "${units_recovered[@]}")")

# ---------------- 重開機總結 ----------------
boot_id=$(cat /proc/sys/kernel/random/boot_id)
last_boot=$(cat "$state_dir/boot_id" 2>/dev/null || true)
echo "$boot_id" >"$state_dir/boot_id"
if [[ -n $last_boot && $last_boot != "$boot_id" ]]; then
  if [[ -f $state_dir/clean-shutdown ]]; then
    last="上次為正常關機（$(cat "$state_dir/clean-shutdown")）"
  else
    last="未偵測到正常關機通知（可能斷電、強制關機或關機通知未送出）"
  fi
  rm -f "$state_dir/clean-shutdown"
  units_total=0
  for u in $host_units; do systemctl list-unit-files "$u.service" --no-legend 2>/dev/null | grep -q . && units_total=$((units_total + 1)); done
  summary="🔄 [$host] 主機已重新開機（開機於 $(uptime -s)）。$last。
容器：${healthy}/${checked} 正常；主機服務：$((units_total - ${#units_down_now[@]}))/${units_total} 執行中。"
  ((${#lines[@]})) && summary+=$'\n'"$(printf '%s\n' "${lines[@]}")"
  notify "$summary"
elif ((${#lines[@]})); then
  notify "[$host] 服務看門狗
$(printf '%s\n' "${lines[@]}")"
fi

exit 0
