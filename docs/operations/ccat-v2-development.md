# CCAT V2 Development Runtime

## Current binding

JEE-E02 binds source `1f313e776d03c2383adff5aa96b9aac9b78efedc` to Development VPS `server1.nnviopp.com` as Compose project `jee8pay-v2-dev`. The runtime is under `/opt/jee8pay-v2-dev/`; it does not use `/opt/payment/`, V1 databases, V1 volumes, V1 application networks, or public ports 80/443.

Runtime source and deployment inputs are in [`deploy/jee8pay-v2-dev/`](../../deploy/jee8pay-v2-dev/). Generated JAR/UI artifacts and local secrets are ignored. The deployed release records source and artifact checksums in `/opt/jee8pay-v2-dev/SOURCE` and `/opt/jee8pay-v2-dev/current/DEPLOYMENT-MANIFEST.sha256`.

## Topology and access

| Service | Internal endpoint | Host bind | Purpose |
| --- | --- | --- | --- |
| Payment | `payment:9216` | `127.0.0.1:19216` | Payment API and Provider callback runtime |
| Manager | `manager:9217` | `127.0.0.1:19217` | Manager backend |
| Merchant | `merchant:9218` | `127.0.0.1:19218` | Merchant backend and V2 test receiver |
| Cashier | `cashier:80` | `127.0.0.1:19226` | Cashier UI |
| Manager UI | `manager-ui:80` | `127.0.0.1:19227` | Manager UI |
| Merchant UI | `merchant-ui:80` | `127.0.0.1:19228` | Merchant UI |
| Callback ingress | `jee8pay-v2-callback:8080` | none | Exact CCAT APN path only |
| DB / Redis / RocketMQ | Compose-internal only | none | V2-only state and messaging |

Use SSH forwarding for operator access:

```bash
ssh -N nnviopp-sandbox \
  -L 19216:127.0.0.1:19216 \
  -L 19226:127.0.0.1:19226 \
  -L 19227:127.0.0.1:19227 \
  -L 19228:127.0.0.1:19228
```

## Resource budget and staged start

Compose hard limits total 3264 MiB including the one-shot volume initializer and callback ingress. The deployment gate requires at least 1536 MiB host/V1 reserve. Check `MemAvailable`、container restart/OOM state and V1 health after each phase.

```bash
cd /opt/jee8pay-v2-dev/current
export V2_SECRET_DIR=/opt/jee8pay-v2-dev/secrets
docker compose -p jee8pay-v2-dev -f compose.yml up -d db redis mq-namesrv
docker compose -p jee8pay-v2-dev -f compose.yml up -d mq-broker
docker compose -p jee8pay-v2-dev -f compose.yml up -d payment
docker compose -p jee8pay-v2-dev -f compose.yml up -d manager merchant
docker compose -p jee8pay-v2-dev -f compose.yml up -d cashier manager-ui merchant-ui callback-ingress
```

The VPS CPU cannot run the repository's arm64 SWR MySQL/JRE images or current `mysql:8.0` requiring x86-64-v2. V2 therefore uses host-validated amd64 `mariadb:10.11` and official multi-arch `eclipse-temurin:17-jre-jammy`. This is artifact compatibility, not V1 database reuse.

## Secrets and Provider gate

Infrastructure/JWT/test-app secrets are V2-only files under `/opt/jee8pay-v2-dev/secrets`, mode `0600`; values never belong in Git or reports. App-readable secrets use fixed UID/GID `10001`; the DB root secret remains root-owned. CCAT credentials are bound through native `t_pay_interface_config.if_params` with exactly `environment`、`custId` and `apiPassword`.

Missing or ambiguous CCAT credentials leave the Provider gate closed. Never copy values from V1、conversation、logs、shell history or documentation. Token probing is limited to one non-transactional request only after environment/account scope and redaction are verified.

The V2-only intake is `/opt/jee8pay-v2-dev/secrets/ccat-provider/`, owned by `root:root` with mode `0700`. Its `environment`、`custId` and `apiPassword` files are mode `0600`. Populate or rotate it only with:

```bash
ssh -tt nnviopp-sandbox 'sudo -n /opt/jee8pay-v2-dev/bin/populate-v2-ccat-secret'
```

The root-only helper reads values from the TTY、suppresses password echo and confirmation、and never accepts a secret in argv or prints one. Select exactly `TEST` or `PRODUCTION`; a Development platform does not imply a CCAT environment. The intake is never mounted into an application container or committed to Git.

JEE-E02 validated a `PRODUCTION` intake、provisioned exactly one `APP_E02_CCAT_DEV / ccat` native config row and completed exactly one successful standalone Token authentication on 2026-08-13. The one-shot marker is `/opt/jee8pay-v2-dev/state/ccat-token-probe-attempted`; it prevents a second probe. Post-probe exact-value and token-pattern scans found no V2 log exposure. `t_pay_interface_config.if_params` at-rest protection remains TD-001.

The first authorized TWD 40 unified-order attempt created local PayOrder `P2087588849919840258` in `INIT` but did not reach CCAT. Root cause TD-011 was a Provider integration defect: Create、Query and APN read the context's cache-populated map instead of the native cache-aware `ConfigContextQueryService.queryNormalMchParams` source. A Provider-local resolver now uses that native query in all three paths, so both cache modes retain `t_pay_interface_config.if_params` as the single source of truth and missing/malformed/wrong-bound params fail closed.

TD-011 regression evidence is 53/53 CCAT tests、57/57 backend tests、compile/package PASS. V2 payment release `1f313e776d03c2383adff5aa96b9aac9b78efedc-td011-134c78229b7a` deploys artifact SHA-256 `134c78229b7ac25a15e358ea3d4e1e7d284da526bea3577a43da18c70ddcd94c`; its 169-entry manifest verifies completely. Runtime remains `isys.cache-config=false`、CCAT config validation PASS、V2 11/11 healthy and exact-secret/token log hits 0.

The old INIT order is intentionally retained unchanged as a test artifact. Do not recover、delete or update it and do not add lifecycle functionality for it. TD-012 remains nonblocking because native reissue does not automatically Create for INIT. A separately authorized new native order may proceed without treating the artifact as a runtime blocker.

After the TD-011 deployment and a fresh minimal preflight, E02 invoked the native Merchant unified-order endpoint exactly once for one newly authorized TWD 40 order. PayOrder `P2087602494821605377` reached native `ING` / WAITING and returned a valid ibon payment instruction expiring 2026-08-20. The root-only one-shot marker and response are under `/opt/jee8pay-v2-dev/state/ccat-authorized-new-order-*`; do not submit another Create.

The human payment completed on 2026-08-13. CCAT first sent status `A`, which passed validation and retained WAITING, then status `B`, which passed checksum、account、order、transaction、amount and authenticated Query reconciliation. Native `ChannelNoticeController` changed the order from `ING` to `SUCCESS`, stored Provider transaction reference `2026081300245913` and returned the CCAT `OK` ACK. Native Merchant Notify created exactly one record; its first MQ delivery received `SUCCESS` and required no retry. Final V1 and V2 health remained 11/11 each, and post-payment exact-secret/token log scans remained zero.

## Public callback binding

Bound callback URL:

```text
https://ccat-v2-dev.nnviopp.com/api/pay/notify/ccat
```

JEE-E02 applied the explicitly approved additive Sandbox control-plane delta on 2026-08-13. The hostname is a DNS-only A record to `159.198.40.128` with TTL 300. Certificate `nnviopp-sandbox-edge` contains the prior three names plus the new hostname. Only the exact APN path reaches `jee8pay-v2-callback:8080`; root and nested paths return `404`. Existing V1 host smoke remains `404 / 200 / 404`, both V1 and V2 remain 11/11 healthy, and the V1 edge was never stopped or recreated.

Applied evidence:

```text
edge/nginx.conf SHA256 = 10a4877269bae2e624e26554a166078bff441545a26ef87f26d1549f3fe1c3a4
compose.sandbox-edge.yaml SHA256 = 492531f744a3109d663d7094e7dfe526618fc0f4bb3a957a92865a0d513aceda
certificate SANs = ccat-v2-dev.nnviopp.com, merchant-sandbox.nnviopp.com, sandbox-api.nnviopp.com, sandbox.nnviopp.com
V2 upstream = jee8pay-v2-callback:8080
exact APN path = /api/pay/notify/ccat
public exact-path invalid probe = HTTP 400, same response as direct V2 transit probe
wrong-path probes = HTTP 404
V1 edge restart count = 0
```

The pre-apply DNS and routing plans both passed. Root-only snapshots of the absent DNS state, original V1 edge inputs, certificate/SANs, container state and checksums are under `/opt/jee8pay-v2-dev/state/public-callback-preapply-20260813/`. The prior V2 `paySiteUrl` is captured separately. No secret value is stored in this documentation.

P04 sends `apn_url` dynamically on each `CvsOrderAppend`: `CcatIbon.pay` builds the Append request with `getNotifyUrl()`, and the native URL is `DBApplicationConfig.paySiteUrl + /api/pay/notify/ccat`. V2-only `paySiteUrl` is now `https://ccat-v2-dev.nnviopp.com`; no CCAT contractual-member portal mutation is required.

Because the authorization prohibited stopping or recreating the existing V1 edge, the route is active through a zero-stop bind mount in the existing container mount namespace plus a graceful Nginx HUP. The root-only marker is `/opt/jee8pay-v2-dev/state/public-callback-edge-hot-applied`. The V2-owned Compose overlay remains the prepared durable form, but it was not used to recreate the container. An edge container restart/recreation removes the hot route; therefore verify the marker, current config hash and public callback before every real E2E attempt. This operational restart guard is tracked as TD-010.

Read-only validation:

```bash
getent ahostsv4 ccat-v2-dev.nnviopp.com
openssl s_client -connect 159.198.40.128:443 -servername ccat-v2-dev.nnviopp.com </dev/null 2>/dev/null \
  | openssl x509 -noout -ext subjectAltName
curl -sS -o /dev/null -w '%{http_code}\n' https://ccat-v2-dev.nnviopp.com/
curl -sS -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' \
  --data '{}' https://ccat-v2-dev.nnviopp.com/api/pay/notify/ccat
curl -sS -o /dev/null -w '%{http_code}\n' https://sandbox-api.nnviopp.com/
curl -sS -o /dev/null -w '%{http_code}\n' https://sandbox.nnviopp.com/
curl -sS -o /dev/null -w '%{http_code}\n' https://merchant-sandbox.nnviopp.com/
```

Rollback, in reverse ownership order:

```bash
# Restore the V2-only origin first.
cd /opt/jee8pay-v2-dev/current
sudo docker compose -p jee8pay-v2-dev exec -T db sh -lc \
  'export MYSQL_PWD="$(cat /run/secrets/db-root-password)"; exec mariadb -uroot' <<'SQL'
UPDATE jee8pay_v2_dev.t_sys_config
SET config_val = 'http://127.0.0.1:9216'
WHERE config_key = 'paySiteUrl'
  AND config_val = 'https://ccat-v2-dev.nnviopp.com';
SQL

# Remove the hot config mount, gracefully reload the original config and detach transit.
sudo /opt/jee8pay-v2-dev/bin/rollback-v2-callback-edge-hot

# Restore the original three-SAN certificate.
sudo certbot certonly --non-interactive --cert-name nnviopp-sandbox-edge \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/nnviopp-sandbox/cloudflare-token.ini \
  --dns-cloudflare-propagation-seconds 30 \
  --pre-hook /bin/true --post-hook /bin/true \
  --deploy-hook /opt/payment/payment-service-sandbox/scripts/sync-sandbox-edge-certificate.sh \
  --force-renewal \
  -d merchant-sandbox.nnviopp.com \
  -d sandbox-api.nnviopp.com \
  -d sandbox.nnviopp.com

# Delete only the E02-created DNS record using the captured absent pre-state.
sudo env SANDBOX_CCAT_V2_DNS_ROLLBACK_APPROVED=YES \
  /opt/jee8pay-v2-dev/bin/manage-sandbox-ccat-v2-dns.sh \
  rollback /etc/nnviopp-sandbox/cloudflare-token.ini \
  /opt/jee8pay-v2-dev/state/ccat-v2-dns-backup.json
```

## Merchant Notify receiver

V2 uses the native Merchant test receiver:

```text
http://merchant:9218/api/anon/paytestNotify/payOrder
```

It validates the native JeePay signature against the synthetic V2 app and returns exact `SUCCESS`. The deployment raises both JeePay signing-helper loggers to WARN because upstream INFO logging can include signing preimages; source-level remediation remains TD-009.

## Rollback and inspection

Rollback only the TD-011 payment artifact without touching V1 or V2 stateful services:

```bash
sudo ln -s /opt/jee8pay-v2-dev/releases/1f313e776d03c2383adff5aa96b9aac9b78efedc \
  /opt/jee8pay-v2-dev/current.rollback
sudo mv -T /opt/jee8pay-v2-dev/current.rollback /opt/jee8pay-v2-dev/current
cd /opt/jee8pay-v2-dev/current
export V2_SECRET_DIR=/opt/jee8pay-v2-dev/secrets
sudo -E docker compose -p jee8pay-v2-dev -f compose.yml up -d --no-deps payment
```

Stopping V2 preserves its volumes and does not touch V1:

```bash
cd /opt/jee8pay-v2-dev/current
export V2_SECRET_DIR=/opt/jee8pay-v2-dev/secrets
docker compose -p jee8pay-v2-dev -f compose.yml stop
```

Inspect without printing environment values:

```bash
docker compose -p jee8pay-v2-dev -f /opt/jee8pay-v2-dev/current/compose.yml ps
docker stats --no-stream --filter label=com.docker.compose.project=jee8pay-v2-dev
docker inspect --format '{{.Name}} restarts={{.RestartCount}} oom={{.State.OOMKilled}}' \
  $(docker ps -q --filter label=com.docker.compose.project=jee8pay-v2-dev)
```
