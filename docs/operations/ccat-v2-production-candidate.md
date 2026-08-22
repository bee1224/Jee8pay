# CCAT V2 Production Candidate

> **RENAME NOTE（2026-08-20）**：本文記錄 Production Candidate 的歷史部署狀態。JeePay Provider 已由 `ccat` 改名為 `ryo`（`CCAT_IBON` → `RYO_IBON`），並新增 `jay` / `chi`；重新部署後 callback 路徑為 `/api/pay/notify/ryo`、`/api/pay/notify/jay`、`/api/pay/notify/chi`，secret intake 腳本為 `populate-v2-ryo-secret`（及 `populate-v2-jay-secret` / `populate-v2-chi-secret`）。文中 `ccat-v2.lp33ing.com` 域名保留為歷史命名。

> **V1 RETIREMENT（2026-08-23）**：V2 Production 已完成公開 edge 切換並退役 V1（`payment-service` 四方聚合支付）。現況：
> - 公開 edge（`lp33ing-production-edge`）已改為**純 V2 edge**：移除 V1 的 `api.lp33ing.com` / `admin.lp33ing.com` server blocks，保留 `admin-v2` / `api-v2`，新增 `ccat-v2.lp33ing.com` callback 三條路由（`/api/pay/notify/ryo|jay|chi` → `callback-ingress:8080` → payment）。
> - DNS：新增 `ccat-v2.lp33ing.com` A record（162.0.233.203, proxied=false, TTL 300）；**已刪除 V1 的 `api` / `admin` / `pilot-callback` records**（backup: `state/dns-before-v1-remove-20260823-060846.json`）；cert SAN 已收斂為 3 個 V2 域名（admin-v2/api-v2/ccat-v2）。
> - V1 容器已停止：`payment-api`、`payment-admin`、`mysql`、`callback-egress-proxy`、`ccat-egress-proxy`（edge 保留為 V2 入口）。V1 DB（`payment_production`）已封存於 `/opt/jee8pay-v2-production/state/v1-payment-production-20260823-055935.sql`。
> - RYO pilot：正式環境 `P2091285666526339074`（TWD 40, RYO_IBON）Create 成功，回傳 ibon paymentCode `CCAT624203770661`（expire 2026-08-30）。
> - **edge 收編**：`deploy/jee8pay-v2-production/compose.yml` 已加入 `edge` service 定義（nginx:alpine, 162.0.233.203:80/443, config/edge-nginx.conf）並**已完成接管**：`docker compose -p lp33ing-production down` 後 V1 compose 全清（0 容器、networks 移除），`jee8pay-v2-production-edge` 由 V2 compose 管理（restart=unless-stopped, 僅 attach `jee8pay-v2-production-network`），admin-v2/api-v2/ccat-v2 路由實測正常。
> - **仍未完成**：正式付款 APN 全流程（pilot 已到出單，未付款）。`jay` / `chi` credentials 已綁定（populate + db rows）。
> - 完整 V1 retirement gap 分析見下文「V1 retirement gap」。

## Current binding

JEE-E04 binds source `654df8b6b1ed01b03612e8dff204ae146730261c` to
`server1.lp33ing.com` as isolated Compose project `jee8pay-v2-production`.
JEE-I05 independently accepts the Candidate's provenance、runtime health、V1/V2
isolation、rollback and V1 non-interference; credential binding、public callback
activation、pilot transactions and cutover remain unstarted Human Gates.
The runtime root is `/opt/jee8pay-v2-production/`; it does not use
`/opt/payment/`, V1 databases, V1 volumes, V1 networks, or public ports 80/443.

The active release is
`/opt/jee8pay-v2-production/releases/654df8b6b1ed-32db2fda` through the
`current` symlink. The transfer archive SHA-256 is
`32db2fdaf881b1883745a8f644e9ff848b6fa51db1cab129829f432d21947171`;
the 172-entry deployment manifest verifies completely. The running payment
JAR SHA-256 is
`134c78229b7ac25a15e358ea3d4e1e7d284da526bea3577a43da18c70ddcd94c`,
the same TD-011 artifact accepted by JEE-I04.

## Topology and access

| Service | Internal endpoint | Host bind | Exposure |
| --- | --- | --- | --- |
| Payment | `payment:9216` | `127.0.0.1:29216` | Loopback / SSH tunnel |
| Manager | `manager:9217` | `127.0.0.1:29217` | Loopback / SSH tunnel |
| Merchant | `merchant:9218` | `127.0.0.1:29218` | Loopback / SSH tunnel |
| Cashier | `cashier:80` | `127.0.0.1:29226` | Loopback / SSH tunnel |
| Manager UI | `manager-ui:80` | `127.0.0.1:29227` | Loopback / SSH tunnel |
| Merchant UI | `merchant-ui:80` | `127.0.0.1:29228` | Loopback / SSH tunnel |
| Callback ingress | `callback-ingress:8080` | none | V2 internal networks only |
| MariaDB | `db:3306` | none | V2 network only |
| Redis | `redis:6379` | none | V2 network only |
| RocketMQ | `mq-namesrv:9876`, broker ports | none | V2 network only |

Operator access uses SSH forwarding; no candidate endpoint is generally public:

```bash
ssh -N lp33ing-production \
  -L 29216:127.0.0.1:29216 \
  -L 29226:127.0.0.1:29226 \
  -L 29227:127.0.0.1:29227 \
  -L 29228:127.0.0.1:29228
```

## Isolation and resource budget

Compose hard limits total 3264 MiB including the one-shot volume initializer
and callback ingress. E04 required at least 1536 MiB host/V1 reserve and
started services in DB → Redis → MQ nameserver → MQ broker → Payment → Manager
→ Merchant → UIs → callback order. The completed stack had about 3108 MiB
`MemAvailable`, no swap, no OOM and no restart loop; V1 remained 6/6 healthy.

Persistent resources are named `jee8pay-v2-production-*`. The fresh database
is `jee8pay_v2_production` on `db:3306`, backed only by
`jee8pay-v2-production-db-data`. Initial verification found 23 tables, the
`ccat` interface and `CCAT_IBON` PayWay, zero PayOrders, zero Merchants and zero
interface-config rows. No V1 or Development data was imported. The upstream
example manager login is removed by the Production-only bootstrap; operator
provisioning is a separate controlled action.

## Runtime and Provider gate

The runtime explicitly sets `PLATFORM_ENVIRONMENT=PRODUCTION` and
`CCAT_PROVIDER_ENVIRONMENT=PRODUCTION`. External configuration keeps
`isys.cache-config=false`; the deployed TD-011 resolver is the exact I04
artifact, so Create, Query and APN use native cache-aware Provider params
loading. Internal Payment/Manager/Merchant and all three UIs return HTTP 200;
an invalid CCAT APN reaches the native callback route and fails closed with
HTTP 400.

Production DNS, TCP 443, certificate verification and time synchronization
for `https://cocs.4128888card.com.tw/` pass from the candidate environment.
No `/Token`, `CvsOrderAppend` or transactional Query was called by E04.

## Secrets and Human Gate

Infrastructure secrets are generated independently under
`/opt/jee8pay-v2-production/secrets/`: the directory is root-owned mode `0700`
and files are mode `0600` with only the minimum runtime owner. They are not
copied from Development or V1. Exact-value and token-pattern scans found no
candidate log exposure.

The V2-only CCAT intake is intentionally unpopulated. Enter Production
credentials only from the operator's own terminal:

```bash
ssh -tt lp33ing-production \
  'sudo -n /opt/jee8pay-v2-production/bin/populate-v2-ccat-secret'
```

The helper requires an interactive TTY, suppresses password echo, accepts only
explicit `PRODUCTION`, writes root-owned mode-`0600` files and never prints a
value. Do not paste credentials into chat. Native
`t_pay_interface_config.if_params` binding remains deferred until an approved
Production Merchant/Application/CCAT passage exists; E04 does not create a test
Merchant or migrate a V1 Merchant. Token authentication remains deferred until
that binding and a fresh redaction check pass.

## Production callback plan

Proposed hostname and exact route:

```text
https://ccat-v2.lp33ing.com/api/pay/notify/ccat
```

Current Production DNS contains only `admin.lp33ing.com`, `api.lp33ing.com`
and `pilot-callback.lp33ing.com`; the candidate hostname is absent. The current
certificate SAN contains those same three names. The additive plan is:

1. Create a DNS-only A record `ccat-v2.lp33ing.com → 162.0.233.203`, TTL 300.
2. Add `ccat-v2.lp33ing.com` to the existing Production edge certificate SANs.
3. Attach only the Production edge to external network
   `jee8pay-v2-production-edge-transit`.
4. Add upstream `jee8pay-v2-production-callback:8080` and an exact-path server
   block for `/api/pay/notify/ccat`; every other candidate-host path returns 404.

This is a plan only. E04 performed no Cloudflare, certificate, edge, Provider
portal or public-routing mutation. The application `paySiteUrl` is prepared for
the candidate hostname, but no order can be created because no Merchant,
passage or CCAT config exists.

Validation after separately authorized activation:

```bash
getent ahostsv4 ccat-v2.lp33ing.com
openssl s_client -connect 162.0.233.203:443 \
  -servername ccat-v2.lp33ing.com </dev/null 2>/dev/null \
  | openssl x509 -noout -ext subjectAltName
curl -sS -o /dev/null -w '%{http_code}\n' \
  https://ccat-v2.lp33ing.com/
curl -sS -o /dev/null -w '%{http_code}\n' \
  -H 'Content-Type: application/json' --data '{}' \
  https://ccat-v2.lp33ing.com/api/pay/notify/ccat
curl -sS -o /dev/null -w '%{http_code}\n' https://api.lp33ing.com/
curl -sS -o /dev/null -w '%{http_code}\n' https://admin.lp33ing.com/
```

Routing rollback removes only the candidate server/upstream and edge network
attachment, restores the prior three-SAN certificate, then deletes only the
new candidate DNS record using its captured absent pre-state. V1 hostname,
upstream and database ownership never change.

## Candidate inspection and rollback

```bash
cd /opt/jee8pay-v2-production/current
export V2_SECRET_DIR=/opt/jee8pay-v2-production/secrets
sudo -E docker compose -p jee8pay-v2-production ps
sudo docker stats --no-stream \
  --filter label=com.docker.compose.project=jee8pay-v2-production
sudo docker inspect --format \
  '{{.Name}} restarts={{.RestartCount}} oom={{.State.OOMKilled}}' \
  $(sudo docker ps -q \
    --filter label=com.docker.compose.project=jee8pay-v2-production)
```

Return to a V1-only running state without changing V1:

```bash
cd /opt/jee8pay-v2-production/current
export V2_SECRET_DIR=/opt/jee8pay-v2-production/secrets
sudo -E docker compose -p jee8pay-v2-production stop
sudo -E docker compose -p jee8pay-v2-production down
```

`down` removes only V2 containers and networks and preserves V2 named volumes.
Removing those volumes is a separate destructive V2-only action and is not
part of routine rollback. V1 needs no restart, database restore or route
restore because E04 never changed them.

## V1 retirement gap（2026-08-23 盤點）

V1（Go `payment-service` 四方聚合支付）與 V2（JeePay Java）是兩套獨立平台。2026-08-23 完成 edge 切換與 V1 容器退役後，剩餘事項：

| 項目 | 狀態 |
| --- | --- |
| V1 公開入口（api/admin.lp33ing.com） | 已關閉（edge 移除 server blocks，回 000；DNS records 已刪） |
| V1 containers（api/admin/mysql/egress proxies） | 已停止；`docker compose -p lp33ing-production down` 已全清；DB 封存於 `state/v1-payment-production-20260823-055935.sql` |
| `jee8pay-v2-production-edge` | V2 compose 管理（nginx:alpine, 162.0.233.203:80/443）；admin-v2/api-v2/ccat-v2 路由正常 |
| jay/chi credentials 綁定 | 已完成（populate secret + db rows） |
| 正式付款 APN 全流程 | pilot 已到 Create 出單（`P2091285666526339074`）；真實付款 + APN + Merchant Notify 未驗證 |
| NewebPay | V2 deferred（`docs/providers/README.md`）；V1 曾 Sandbox verified，若業務需要須在 V2 另建 adapter |
| 代付（payout）/ 結算模型 | V2 fail-closed（`無此轉帳通道介面`）；需業務決策是否在 V2 實作 |
| host reboot 後 V2 復原 | 未測（debt D1-D4） |
| V1 edge 後續 | 已完成：edge 由 V2 compose 接管（`jee8pay-v2-production-edge`），V1 compose down 全清 |

