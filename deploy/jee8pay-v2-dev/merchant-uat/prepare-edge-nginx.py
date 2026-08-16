#!/usr/bin/env python3
import argparse
import hashlib
import ipaddress
import os
from pathlib import Path


DEFAULT_SOURCE = Path("/opt/jee8pay-v2-dev/public-callback/nginx.proposed.conf")
DEFAULT_TARGET = Path("/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf")
EXPECTED_SOURCE_SHA256 = "2dadcef48ac7d992f343d03bdf7a7f6b0e1213e58bdcf136d2c646c5d728d9bb"
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


def replace_once(text: str, old: str, new: str, error: str) -> str:
    if text.count(old) != 1:
        raise SystemExit(error)
    return text.replace(old, new, 1)


def render(source: Path, origin_mode: str, query_exception: str) -> str:
    source_bytes = source.read_bytes()
    if hashlib.sha256(source_bytes).hexdigest() != EXPECTED_SOURCE_SHA256:
        raise SystemExit("PREPARE=FAIL_CURRENT_EDGE_BASELINE_CHANGED")
    text = source_bytes.decode("utf-8")
    if "api-v2-dev.nnviopp.com" in text:
        raise SystemExit("PREPARE=FAIL_HOSTNAME_ALREADY_PRESENT")

    ipv4_ranges = read_ranges(SCRIPT_DIR / "cloudflare-ips-v4.txt", 4)
    ipv6_ranges = read_ranges(SCRIPT_DIR / "cloudflare-ips-v6.txt", 6)
    if query_exception != "none" and query_exception != AUTHORIZED_QUERY_EXCEPTION:
        raise SystemExit("PREPARE=FAIL_QUERY_EXCEPTION_NOT_AUTHORIZED")

    log_anchor = "  log_format edge '$request_method $host $uri $status $body_bytes_sent $request_time';"
    log_replacement = log_anchor + """
  log_format cf01 escape=json '{"ts":"$time_iso8601","host":"$host","method":"$request_method","uri":"$uri","status":$status,"client":"$remote_addr","peer":"$realip_remote_addr","cf_connecting_ip":"$http_cf_connecting_ip","cf_ray":"$http_cf_ray","request_id":"$request_id","upstream":"$upstream_addr","upstream_status":"$upstream_status","request_time":"$request_time"}';"""
    text = replace_once(text, log_anchor, log_replacement, "PREPARE=FAIL_LOG_FORMAT_ANCHOR")

    map_anchor = """  map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
  }
"""
    geo_lines = ["", "  geo $realip_remote_addr $api_v2_cf_peer {", "    default 0;"]
    geo_lines.extend(f"    {item} 1;" for item in ipv4_ranges + ipv6_ranges)
    geo_lines.extend(["  }", ""])
    text = replace_once(text, map_anchor, map_anchor + "\n".join(geo_lines),
                        "PREPARE=FAIL_MAP_ANCHOR")

    upstream_anchor = """  upstream jee8pay_v2_callback {
    server jee8pay-v2-callback:8080;
    keepalive 4;
  }
"""
    upstream_replacement = upstream_anchor + """
  upstream jee8pay_v2_merchant_api {
    server jee8pay-v2-merchant-api:8080;
    keepalive 8;
  }
"""
    text = replace_once(text, upstream_anchor, upstream_replacement,
                        "PREPARE=FAIL_UPSTREAM_ANCHOR")

    real_ip_lines = [f"    set_real_ip_from {item};" for item in ipv4_ranges + ipv6_ranges]
    real_ip_lines.extend([
        "    real_ip_header CF-Connecting-IP;",
        "    real_ip_recursive off;",
    ])
    real_ip = "\n".join(real_ip_lines)
    origin_gate = "" if origin_mode == "dns-only" else """

    if ($api_v2_cf_peer = 0) {
      return 403;
    }"""

    http_server = f"""

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
"""
    http_anchor = """  server {
    listen 443 ssl;
    server_name sandbox-api.nnviopp.com;
"""
    text = replace_once(text, http_anchor, http_server + "\n" + http_anchor,
                        "PREPARE=FAIL_HTTP_TARGET_ANCHOR")

    query_allow = ""
    if query_exception != "none":
        query_allow = f"      allow {query_exception};\n"
    merchant_server = f"""

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
      allow 34.92.245.74;
      allow 34.92.52.162;
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
      allow 34.92.245.74;
      allow 34.92.52.162;
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
"""
    admin_merchant_server = f"""

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
"""
    if not text.endswith("}\n"):
        raise SystemExit("PREPARE=FAIL_HTTP_TAIL_ANCHOR")
    return text[:-2] + merchant_server + admin_merchant_server + "}\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_TARGET)
    parser.add_argument("--origin-mode", choices=("dns-only", "proxied"), required=True)
    parser.add_argument("--query-exception", default="none")
    args = parser.parse_args()

    text = render(args.source, args.origin_mode, args.query_exception)
    temporary = args.output.with_name(f".{args.output.name}.tmp")
    temporary.write_text(text, encoding="utf-8")
    if os.geteuid() == 0:
        os.chown(temporary, 0, 10002)
        os.chmod(temporary, 0o640)
    temporary.replace(args.output)
    print("PREPARE=PASS")
    print(f"ORIGIN_MODE={args.origin_mode}")
    print(f"QUERY_EXCEPTION={args.query_exception}")
    print("V1_SOURCE_MODIFIED=NO")
    print("PROPOSED_SHA256=" + hashlib.sha256(text.encode("utf-8")).hexdigest())


if __name__ == "__main__":
    main()
