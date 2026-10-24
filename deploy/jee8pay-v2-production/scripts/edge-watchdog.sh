#!/usr/bin/env bash
# Production edge 看門狗（root cron 每分鐘 + 開機後執行）。
# edge 是唯一對外 80/443 入口；2026-10-24 主機被 hypervisor 關機重開後 edge 未自動啟動，
# 停機 83 分鐘無人知道。本腳本：edge 停了就自動 docker start；狀態改變時用
# Daily Report 同一個 bot 通知營運群組。只在狀態改變時通知，避免每分鐘洗版。
set -uo pipefail

readonly edge=${EDGE_CONTAINER:-jee8pay-v2-production-edge}
readonly listen_ip=${EDGE_LISTEN_IP:-162.0.233.203}
readonly chat_id=${TELEGRAM_CHAT_ID:--5211710664}
readonly token_file=${TELEGRAM_TOKEN_FILE:-/opt/jee8pay-v2-production/secrets/telegram-bot-token}
readonly state_dir=${EDGE_WATCHDOG_STATE_DIR:-/var/lib/jee8pay-edge-watchdog}
# 連續幾次不健康才告警，避開 nginx reload / 剛啟動時的短暫狀態
readonly unhealthy_alert_after=3

log() { echo "$(date -Is) $*"; }

[[ $EUID -eq 0 ]] || { echo 'run as root' >&2; exit 1; }
install -d -m 0700 "$state_dir"

notify() {
  local token code
  token=$(cat "$token_file" 2>/dev/null) || { log 'telegram token unreadable'; return 1; }
  # token 經 stdin 交給 curl，不出現在 process list 或 log
  code=$(printf 'url = "https://api.telegram.org/bot%s/sendMessage"\n' "$token" |
    curl -sS -m 15 -o /dev/null -w '%{http_code}' -K - \
      --data-urlencode "chat_id=$chat_id" --data-urlencode "text=$1" 2>/dev/null)
  [[ $code == 200 ]] || log "telegram send failed http=$code"
}

edge_status() {
  local running health
  running=$(docker inspect -f '{{.State.Running}}' "$edge" 2>/dev/null) || { echo missing; return; }
  [[ $running == true ]] || { echo stopped; return; }
  health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$edge")
  if [[ $health == unhealthy ]] || ! ss -ltnH 'sport = :443' | grep -q "$listen_ip:443"; then
    echo unhealthy
  else
    echo ok
  fi
}

prev=$(cat "$state_dir/state" 2>/dev/null || echo ok)
fails=$(cat "$state_dir/fails" 2>/dev/null || echo 0)
host=$(hostname)
status=$(edge_status)

case $status in
  stopped)
    log "edge stopped, docker start"
    docker start "$edge" >/dev/null 2>&1
    sleep 20
    status=$(edge_status)
    if [[ $status == ok ]]; then
      notify "🚨 [$host] edge 已停止，看門狗已自動啟動，80/443 恢復正常。請確認停止原因（主機重開？）。"
      log 'edge auto-started'
      prev=ok # 已在上一則通知說明恢復，不再重複發「已恢復」
    else
      [[ $prev == down ]] || notify "🚨 [$host] edge 已停止，自動啟動失敗（$status），對外 80/443 中斷，需要人工處理！"
      echo down >"$state_dir/state"
      log "edge auto-start failed: $status"
      exit 1
    fi
    ;;
  missing)
    [[ $prev == down ]] || notify "🚨 [$host] 找不到 edge 容器（$edge），對外 80/443 中斷，需要人工處理！"
    echo down >"$state_dir/state"
    log 'edge container missing'
    exit 1
    ;;
  unhealthy)
    fails=$((fails + 1))
    echo "$fails" >"$state_dir/fails"
    if ((fails >= unhealthy_alert_after)) && [[ $prev != down ]]; then
      notify "⚠️ [$host] edge 在跑但連續 ${fails} 分鐘不健康（healthcheck 或 $listen_ip:443 未監聽），請檢查。"
      echo down >"$state_dir/state"
    fi
    log "edge unhealthy ($fails)"
    exit 1
    ;;
esac

# 走到這裡代表 ok
[[ $prev == down ]] && notify "✅ [$host] edge 已恢復正常（80/443）。"
echo ok >"$state_dir/state"
echo 0 >"$state_dir/fails"
