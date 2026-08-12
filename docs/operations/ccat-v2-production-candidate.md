# CCAT V2 Production Candidate

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
