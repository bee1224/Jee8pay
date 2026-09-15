# Sandbox Edge Recovery and Restart Persistence

## Scope and current state

JEE-N01 recovered `nnviopp-sandbox-edge` on `server1.nnviopp.com` and replaced the prior runtime-only network/config attachment with a canonical Compose overlay. This runbook covers the Sandbox edge, its stable V2 transit network, ingress config, healthcheck and edge-only lifecycle validation. It does not authorize application、database、Provider、Production or Cloudflare changes.

> **2026-08-23 更新（V1 退役）**：edge 已由 V1 專案（`/opt/payment/payment-service-sandbox`）移出，改為 V2 standalone compose（`/opt/jee8pay-v2-dev/edge/compose.edge.yaml`，project `jee8pay-v2-dev-edge`）。V1 networks（`nnviopp-sandbox_edge` / `_edge-public`）已刪除；edge 只掛 `jee8pay-v2-dev-edge-transit` + `jee8pay-v2-dev-network`（transit 為 internal，port 發布需同時掛非 internal 的 v2 network）。N01 overlay 機制（`public-callback/compose.edge-overlay.yaml`）與 V1 baseline 已退役。

Current canonical inputs:

```text
Edge compose = /opt/jee8pay-v2-dev/edge/compose.edge.yaml
Edge project = jee8pay-v2-dev-edge
Edge container = nnviopp-sandbox-edge（沿用歷史名，cert deploy hook 參照）
Ingress config = /opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf
Ingress config SHA256 = 7a393f332a6830c932a4e51b1754165af2be652b61a31640edcfbd79ca328ea4
Ingress config owner/mode = 0:10002 0640
Config generator = /opt/jee8pay-v2-dev/merchant-uat/prepare-edge-nginx.py（自足純 V2，無 V1 baseline 依賴）
Allowlist dir = /opt/jee8pay-v2-dev/edge-allowlist（host cron 每分鐘產生 uat.conf）
Transit network = jee8pay-v2-dev-edge-transit
V2 network = jee8pay-v2-dev-network
Restart policy = unless-stopped
```

The config checksum differs from reviewed final checksum `0786d6bcba356279f3065f2b33ef97fac84f998e1ff035c901a3a45108acf7f6` by one logging-only change: `$remote_addr` was prepended to the edge access log. The root-only semantic diff is `/opt/jee8pay-v2-dev/state/n01/final-config-semantic.diff`. Routes、upstreams、methods and the two-IP UAT allowlist did not change.

**2026-08-21 update**：Provider 改名與新增上游後，edge callback 已由 `/api/pay/notify/ccat` 改為 `/api/pay/notify/{ryo,jay,chi}` 三條 route，ingress config SHA256 更新為 `cb1500d31110f06e5211089976ac8436329567ba007ef854f4baceaaf24e56b6`（allowlist include ×2 保留，renderer 已更新為 include 版本；`bin/reconcile-sandbox-edge` / `bin/validate-sandbox-edge` 的 `expected_config_sha` 已同步）。

## Root cause closed by N01

Docker journal evidence identified stale endpoint `9c7bb585fd5e` for deleted temporary network `jee-d01-route-test` (`0f732423…`). The edge had been manually connected to that network during route testing. Docker attempted to restore the deleted network ID after host restart and left the edge at `Exited(255)`.

V2 transit and ingress config had also been attached to the running edge outside its Compose provenance. Config was a bind mount from a container-local `/tmp` file in the running mount namespace. Container restart therefore restored the image's V1 baseline config and removed the V2 callback/Create/Query routes.

N01 removes both lifecycle defects:

- the overlay declares `jee8pay-v2-dev-edge-transit` by stable external name;
- edge recreation contains exactly the two V1 networks and the declared V2 transit network;
- every attached network ID resolves to an existing Docker network;
- the reviewed ingress config is a durable host file mounted read-only by Compose;
- the container's Compose config-file label includes the overlay;
- `unless-stopped` restores the edge after an unplanned Nginx PID 1 exit and Docker/host restart;
- the old hot-apply scripts are superseded and must not be used.

## Canonical reconciliation

The command is edge-only and requires an explicit Sandbox approval variable:

```bash
sudo env SANDBOX_EDGE_RECONCILE_APPROVED=YES \
  /opt/jee8pay-v2-dev/bin/reconcile-sandbox-edge
```

The helper validates host identity、config/compose checksums and ownership、route/allowlist invariants、stable transit availability and both V2 ingress health endpoints before running Compose. It uses `--no-deps --no-build --force-recreate sandbox-edge` on the standalone `jee8pay-v2-dev-edge` project; it does not restart application containers or remove orphans.

Do not run `docker network connect` for edge routing. Do not copy config into the container or bind mount a `/tmp` file. Do not render the full Compose model with the secret env file to terminal output; inspect only the selected edge fields.

## Read-only validation

```bash
sudo /opt/jee8pay-v2-dev/bin/validate-sandbox-edge
```

The validator checks:

- edge running/healthy and `unless-stopped`;
- active config checksum and read-only durable mount;
- canonical Compose provenance（project `jee8pay-v2-dev-edge`、config file `edge/compose.edge.yaml`）;
- exact network set（`jee8pay-v2-dev-edge-transit` + `jee8pay-v2-dev-network`）and resolvable network IDs;
- `nginx -t`;
- local 80/443 listening sockets;
- both V2 ingress health endpoints（callback + merchant-api）;
- V2 core `11/11` and V1 containers absent;
- exact Create、Query and 黑貓 PAY（RYO/JAY/CHI/JHD）callback routes;
- config 不含任何 V1 hostname/upstream 參照;
- UAT allowlist unchanged and Production IP absent.

Latest root-only evidence is `/opt/jee8pay-v2-dev/state/v1-retirement-20260822-222906/validation-after.txt`. Recovery-time evidence was `/opt/jee8pay-v2-dev/state/n01/forensic-before.txt`（N01 歷史）。

## External readiness gate

The edge healthcheck is now a necessary External readiness signal. It runs `nginx -t`, verifies listening sockets for 80 and 443, checks `/edge-health`, and checks the callback and Merchant API ingress health endpoints. Application `11/11 healthy` alone is not External Ready.

Public route contract remains:

```text
api-v2-dev.nnviopp.com POST /api/pay/unifiedOrder
api-v2-dev.nnviopp.com POST /api/pay/query
ccat-v2-dev.nnviopp.com /api/pay/notify/ryo
ccat-v2-dev.nnviopp.com /api/pay/notify/jay
ccat-v2-dev.nnviopp.com /api/pay/notify/chi
```

The API host exposes no Manager、Merchant UI or Cashier path. UAT allowlist remains only `34.92.245.74` and `34.92.52.162`; `35.220.239.87` is not present.

Before any real Create retry, the external Merchant must send one signed Query for a non-existing order from the same server that will later send Create. Correlate the declared source with the `$remote_addr` value in `nnviopp-sandbox-edge` ingress logs. Stop at the first DNS、TCP、TLS、source IP、allowlist、reverse proxy、backend、Merchant auth/sign or application-response divergence. A Query preflight must not create an order or call the Provider.

## Restart acceptance

N01 performed two canonical edge-only recreates. It also terminated only edge PID 1 and observed Docker restart it automatically with `RestartCount 0 → 1`; the config checksum、network set、health and 80/443 listeners survived. No application container start time changed.

The full host reboot was not repeated during N01. A host reboot smoke remains a future maintenance acceptance; until it is run, report `HOST_REBOOT_FULLY_TESTED=NO` without weakening the declarative persistence evidence.
