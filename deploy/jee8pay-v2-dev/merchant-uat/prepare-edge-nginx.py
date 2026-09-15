#!/usr/bin/env python3
"""Render the canonical V2-only Sandbox edge nginx config.

Standalone generator: no V1 baseline dependency. V1 hostnames
(sandbox-api/sandbox/merchant-sandbox.nnviopp.com) were retired 2026-08-23;
the edge serves only V2 routes:
  ccat-v2-dev.nnviopp.com   /api/pay/notify/{ryo,jay,chi} -> jee8pay_v2_callback
  api-v2-dev.nnviopp.com    /api/pay/unifiedOrder, /api/pay/query -> jee8pay_v2_merchant_api
  admin-v2-dev.nnviopp.com  / (admin UI) -> jee8pay_v2_merchant_api
"""
import argparse
import hashlib
import ipaddress
import os
from pathlib import Path

DEFAULT_TARGET = Path("/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf")
AUTHORIZED_QUERY_EXCEPTION = "1.165.244.234"
SCRIPT_DIR = Path(__file__).resolve().parent


def read_ranges(path: Path, version: int) -> list[str]:
    ranges = [line.strip() for line in path.read_text(encoding="utf-8").splitlines()
              if line.strip() and not line.lstrip().startswith("#")]
    if not ranges or len(ranges) != len(set(ranges)):
        raise SystemExit(f"PREPARE=FAIL_IPV{version}_RANGES_EMPTY_OR_DUPLICATE")
    networks = [ipaddress.ip_network(item, strict=True) for item in ranges]
    if any(network.version != version or network.prefixlen == 0 for network in networks):
        raise SystemExit(f"PREPARE=FAIL_IPV{version}_RANGE_INVALID")
    return ranges


def render(origin_mode: str, query_exception: str) -> str:
    if query_exception != "none" and query_exception != AUTHORIZED_QUERY_EXCEPTION:
        raise SystemExit("PREPARE=FAIL_QUERY_EXCEPTION_NOT_AUTHORIZED")

    ipv4_ranges = read_ranges(SCRIPT_DIR / "cloudflare-ips-v4.txt", 4)
    ipv6_ranges = read_ranges(SCRIPT_DIR / "cloudflare-ips-v6.txt", 6)
    geo_lines = ["  geo $realip_remote_addr $api_v2_cf_peer {", "    default 0;"]
    geo_lines.extend(f"    {item} 1;" for item in ipv4_ranges + ipv6_ranges)
    geo_lines.append("  }")
    real_ip = "\n".join(
        [f"    set_real_ip_from {item};" for item in ipv4_ranges + ipv6_ranges]
        + ["    real_ip_header CF-Connecting-IP;", "    real_ip_recursive off;"]
    )
    origin_gate = "" if origin_mode == "dns-only" else """
    if ($api_v2_cf_peer = 0) {
      return 403;
    }"""

    query_allow = ""
    if query_exception != "none":
        query_allow = f"      allow {query_exception};\n"

    return f"""pid /tmp/nginx.pid;
worker_processes auto;
error_log /dev/stderr warn;

events {{
  worker_connections 1024;
}}

http {{
  include /etc/nginx/mime.types;
  default_type application/octet-stream;
  server_tokens off;

  log_format edge '$request_method $host $uri $status $body_bytes_sent $request_time';
  log_format cf01 escape=json '{{"ts":"$time_iso8601","host":"$host","method":"$request_method","uri":"$uri","status":$status,"client":"$remote_addr","peer":"$realip_remote_addr","cf_connecting_ip":"$http_cf_connecting_ip","cf_ray":"$http_cf_ray","request_id":"$request_id","upstream":"$upstream_addr","upstream_status":"$upstream_status","request_time":"$request_time"}}';
  access_log /dev/stdout edge;

  sendfile on;
  keepalive_timeout 30s;
  client_body_timeout 15s;
  client_header_timeout 15s;
  client_max_body_size 12m;

  ssl_protocols TLSv1.2 TLSv1.3;
  ssl_session_cache shared:SSL:10m;
  ssl_session_timeout 10m;
  ssl_session_tickets off;
  ssl_certificate /etc/nginx/tls/fullchain.pem;
  ssl_certificate_key /etc/nginx/tls/privkey.pem;

  map $http_upgrade $connection_upgrade {{
    default upgrade;
    '' close;
  }}

{chr(10).join(geo_lines)}

  upstream jee8pay_v2_callback {{
    server jee8pay-v2-callback:8080;
    keepalive 4;
  }}

  upstream jee8pay_v2_merchant_api {{
    server jee8pay-v2-merchant-api:8080;
    keepalive 8;
  }}

  server {{
    listen 80 default_server;
    server_name _;

    location = /edge-health {{
      access_log off;
      default_type text/plain;
      return 200 'OK';
    }}

    location / {{
      return 444;
    }}
  }}

  server {{
    listen 443 ssl default_server;
    server_name _;
    return 444;
  }}

  server {{
    listen 80;
    server_name ccat-v2-dev.nnviopp.com;
    return 301 https://$host$request_uri;
  }}

  server {{
    listen 80;
    server_name api-v2-dev.nnviopp.com;
{real_ip}
    access_log /dev/stdout cf01;
{origin_gate}

    return 301 https://$host$request_uri;
  }}

  server {{
    listen 80;
    server_name admin-v2-dev.nnviopp.com;
{real_ip}
    access_log /dev/stdout cf01;
{origin_gate}

    return 301 https://$host$request_uri;
  }}

  server {{
    listen 443 ssl;
    server_name ccat-v2-dev.nnviopp.com;

    add_header Strict-Transport-Security 'max-age=31536000' always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;

    location = /api/pay/notify/ryo {{
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 60s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_callback;
    }}

    location = /api/pay/notify/jay {{
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 60s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_callback;
    }}

    location = /api/pay/notify/chi {{
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 60s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_callback;
    }}

    location / {{
      return 404;
    }}
  }}

  server {{
    listen 443 ssl;
    server_name api-v2-dev.nnviopp.com;
{real_ip}
    access_log /dev/stdout cf01;
{origin_gate}

    add_header Strict-Transport-Security 'max-age=31536000' always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;

    location = /api/pay/unifiedOrder {{
      # 白名單由 host cron 依 allowlist.json 產生（/etc/nginx/allowlist/uat.conf），儲存後約 1 分鐘內自動套用
      include /etc/nginx/allowlist/uat.conf;
      deny all;
      limit_except POST {{ deny all; }}
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $remote_addr;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Request-ID $request_id;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 150s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_merchant_api;
    }}

    location = /api/pay/query {{
      # 白名單由 host cron 依 allowlist.json 產生（/etc/nginx/allowlist/uat.conf），儲存後約 1 分鐘內自動套用
      include /etc/nginx/allowlist/uat.conf;
{query_allow}      deny all;
      limit_except POST {{ deny all; }}
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $remote_addr;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Request-ID $request_id;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 60s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_merchant_api;
    }}

    location / {{
      return 404;
    }}
  }}

  server {{
    listen 443 ssl;
    server_name admin-v2-dev.nnviopp.com;
{real_ip}
    access_log /dev/stdout cf01;
{origin_gate}

    add_header Strict-Transport-Security 'max-age=31536000' always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;

    location / {{
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $remote_addr;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header X-Request-ID $request_id;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 120s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_merchant_api;
    }}
  }}
}}
"""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=DEFAULT_TARGET)
    parser.add_argument("--origin-mode", choices=("dns-only", "proxied"), required=True)
    parser.add_argument("--query-exception", default="none")
    args = parser.parse_args()

    text = render(args.origin_mode, args.query_exception)
    temporary = args.output.with_name(f".{args.output.name}.tmp")
    temporary.write_text(text, encoding="utf-8")
    if os.geteuid() == 0:
        os.chown(temporary, 0, 10002)
        os.chmod(temporary, 0o640)
    temporary.replace(args.output)
    print("PREPARE=PASS")
    print(f"ORIGIN_MODE={args.origin_mode}")
    print(f"QUERY_EXCEPTION={args.query_exception}")
    print("V1_SOURCE_DEPENDENCY=REMOVED")
    print("PROPOSED_SHA256=" + hashlib.sha256(text.encode("utf-8")).hexdigest())


if __name__ == "__main__":
    main()
