#!/usr/bin/env bash
# Production 資源警戒（root cron 每分鐘）：主機 CPU / 記憶體 / swap / 磁碟空間 / inode，
# 以及每個容器記憶體（相對 mem_limit），達 THRESHOLD% 即以 Daily Report 同一個 bot 通知營運群組。
# 只在「進入警戒」「解除」時通知；持續超標每 REMIND_HOURS 小時提醒一次。
# 解除門檻比警戒低 5 個百分點，避免數值在 70% 上下抖動時反覆通知。
set -uo pipefail

# 主機差異（測試機）放在設定檔覆寫；正式機無設定檔，使用下列預設值
[[ -r ${WATCHDOG_CONFIG:-/etc/jee8pay-watchdog.conf} ]] && . "${WATCHDOG_CONFIG:-/etc/jee8pay-watchdog.conf}"
# PEER_ONLY=1 的主機（測試機）不發資源警戒
[[ ${PEER_ONLY:-0} == 1 ]] && exit 0

readonly threshold=${THRESHOLD:-70}
readonly clear_below=$((threshold - 5))
# CPU 為瞬間值，連續幾分鐘超標才算，避開排程/GC 的短暫尖峰
readonly cpu_sustain=${CPU_SUSTAIN_MINUTES:-3}
readonly remind_seconds=$(( ${REMIND_HOURS:-6} * 3600 ))
readonly chat_id=${TELEGRAM_CHAT_ID:--5211710664}
readonly token_file=${TELEGRAM_TOKEN_FILE:-/opt/jee8pay-v2-production/secrets/telegram-bot-token}
readonly state_dir=${RESOURCE_MONITOR_STATE_DIR:-/var/lib/jee8pay-resource-monitor}
# 要監測的容器（名稱 regex）與通知時去掉的名稱前綴
readonly container_match=${CONTAINER_MATCH:-^jee8pay-v2-production}
readonly container_prefix=${CONTAINER_PREFIX:-jee8pay-v2-production}
# 警戒／解除通知發出後自動刪除的時間（Telegram 只允許 bot 刪除 48 小時內的訊息）
readonly auto_delete_seconds=$(( ${AUTO_DELETE_HOURS:-24} * 3600 ))

log() { echo "$(date -Is) $*"; }

[[ $EUID -eq 0 ]] || { echo 'run as root' >&2; exit 1; }
install -d -m 0700 "$state_dir"
host="${HOST_LABEL:-正式機} $(hostname)"

# tg <method> <curl 參數...>：輸出 response body，最後一行為 HTTP code
tg() {
  local method=$1 token
  shift
  token=$(cat "$token_file" 2>/dev/null) || { log 'telegram token unreadable' >&2; return 1; }
  # token 經 stdin 交給 curl，不出現在 process list 或 log
  printf 'url = "https://api.telegram.org/bot%s/%s"\n' "$token" "$method" |
    curl -sS -m 15 -w '\n%{http_code}' -K - --data-urlencode "chat_id=$chat_id" "$@" 2>/dev/null
}

# 發出的警戒記在 sent 檔（<epoch> <message_id>），滿 AUTO_DELETE_HOURS 後自動刪除
notify() {
  local resp id
  resp=$(tg sendMessage --data-urlencode "text=$1")
  [[ ${resp##*$'\n'} == 200 ]] || { log "telegram send failed http=${resp##*$'\n'}"; return 1; }
  id=$(grep -o '"message_id":[0-9]*' <<<"$resp" | head -1 | cut -d: -f2)
  [[ -n $id ]] && echo "$(date +%s) $id" >>"$state_dir/sent"
  return 0
}

purge_old_alerts() {
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

# check <key> <label> <percent 整數> [detail]
# state 檔內容：「alert <開始 epoch> <上次通知 epoch>」或不存在（正常）
check() {
  local key=$1 label=$2 pct=$3 detail=${4:-} f="$state_dir/$1" now
  now=$(date +%s)
  if [[ -f $f ]]; then
    local _ since last
    read -r _ since last <"$f"
    if ((pct < clear_below)); then
      notify "✅ [$host] $label 已回落至 ${pct}%（警戒持續 $(( (now - since) / 60 )) 分鐘）。" && rm -f "$f"
    elif ((now - last >= remind_seconds)); then
      notify "⚠️ [$host] $label 仍在 ${pct}%（≥${threshold}% 已 $(( (now - since) / 3600 )) 小時）${detail:+，$detail}" &&
        echo "alert $since $now" >"$f"
    fi
  elif ((pct >= threshold)); then
    notify "⚠️ [$host] $label 達 ${pct}%，超過 ${threshold}% 警戒線${detail:+（$detail）}。" &&
      echo "alert $now $now" >"$f"
  fi
}

purge_old_alerts

# ---- 主機 CPU：/proc/stat 取 5 秒區間使用率 ----
read -r _ u1 n1 s1 i1 w1 q1 sq1 st1 _ </proc/stat
sleep 5
read -r _ u2 n2 s2 i2 w2 q2 sq2 st2 _ </proc/stat
busy=$(( (u2 + n2 + s2 + q2 + sq2 + st2) - (u1 + n1 + s1 + q1 + sq1 + st1) ))
total=$(( busy + (i2 + w2) - (i1 + w1) ))
cpu=$(( total > 0 ? busy * 100 / total : 0 ))
cpu_streak=$(cat "$state_dir/cpu.streak" 2>/dev/null || echo 0)
if ((cpu >= threshold)); then cpu_streak=$((cpu_streak + 1)); else cpu_streak=0; fi
echo "$cpu_streak" >"$state_dir/cpu.streak"
# 未連續超標前不進入判斷，避免單次尖峰觸發；已在警戒中則每次照實際值判斷是否解除
if ((cpu_streak >= cpu_sustain)) || [[ -f $state_dir/host-cpu ]]; then
  check host-cpu "主機 CPU" "$cpu" "load $(cut -d' ' -f1-3 /proc/loadavg)"
fi

# ---- 主機記憶體 / swap ----
mem_total=$(awk '/^MemTotal:/{print $2}' /proc/meminfo)
mem_avail=$(awk '/^MemAvailable:/{print $2}' /proc/meminfo)
check host-mem "主機記憶體" $(( (mem_total - mem_avail) * 100 / mem_total )) \
  "可用 $((mem_avail / 1024))MiB / $((mem_total / 1024))MiB"
swap_total=$(awk '/^SwapTotal:/{print $2}' /proc/meminfo)
if ((swap_total > 0)); then
  swap_free=$(awk '/^SwapFree:/{print $2}' /proc/meminfo)
  check host-swap "主機 swap" $(( (swap_total - swap_free) * 100 / swap_total ))
fi

# ---- 磁碟空間與 inode（實體檔案系統）----
while read -r mount pct avail; do
  key="disk-$(echo "$mount" | tr '/' '_')"
  check "$key" "磁碟 $mount" "${pct%\%}" "剩餘 $avail"
done < <(df -hP -x tmpfs -x devtmpfs -x overlay -x squashfs -x efivarfs | awk 'NR>1{print $6, $5, $4}')
while read -r mount pct; do
  [[ $pct == - ]] && continue
  check "inode-$(echo "$mount" | tr '/' '_')" "磁碟 inode $mount" "${pct%\%}"
done < <(df -iP -x tmpfs -x devtmpfs -x overlay -x squashfs -x efivarfs | awk 'NR>1{print $6, $5}')

# ---- 容器記憶體（相對 mem_limit；docker stats 已扣除可回收的 page cache）----
while read -r name memperc usage; do
  pct=${memperc%\%}
  check "ctr-$name" "容器 ${name#"$container_prefix"-} 記憶體" "${pct%.*}" "$usage"
done < <(docker stats --no-stream --format '{{.Name}} {{.MemPerc}} {{.MemUsage}}' 2>/dev/null |
  awk -v m="$container_match" '$1 ~ m {print $1, $2, $3 "/" $5}')

exit 0
