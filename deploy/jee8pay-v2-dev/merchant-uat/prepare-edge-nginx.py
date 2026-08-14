#!/usr/bin/env python3
import hashlib
import os
from pathlib import Path

SOURCE = Path("/opt/jee8pay-v2-dev/public-callback/nginx.proposed.conf")
TARGET = Path("/opt/jee8pay-v2-dev/merchant-uat/nginx.proposed.conf")
EXPECTED_SHA256 = "2dadcef48ac7d992f343d03bdf7a7f6b0e1213e58bdcf136d2c646c5d728d9bb"

source_bytes = SOURCE.read_bytes()
if hashlib.sha256(source_bytes).hexdigest() != EXPECTED_SHA256:
    raise SystemExit("PREPARE=FAIL_CURRENT_EDGE_BASELINE_CHANGED")
text = source_bytes.decode("utf-8")

if "api-v2-dev.nnviopp.com" in text:
    raise SystemExit("PREPARE=FAIL_HOSTNAME_ALREADY_PRESENT")

log_anchor = "  log_format edge '$request_method $host $uri $status $body_bytes_sent $request_time';"
log_replacement = "  log_format edge '$remote_addr $request_method $host $uri $status $body_bytes_sent $request_time';"
if text.count(log_anchor) != 1:
    raise SystemExit("PREPARE=FAIL_LOG_FORMAT_ANCHOR")
text = text.replace(log_anchor, log_replacement, 1)

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
if text.count(upstream_anchor) != 1:
    raise SystemExit("PREPARE=FAIL_UPSTREAM_ANCHOR")
text = text.replace(upstream_anchor, upstream_replacement, 1)

http_name_anchor = (
    "    server_name sandbox-api.nnviopp.com sandbox.nnviopp.com "
    "merchant-sandbox.nnviopp.com ccat-v2-dev.nnviopp.com;"
)
http_name_replacement = http_name_anchor[:-1] + " api-v2-dev.nnviopp.com;"
if text.count(http_name_anchor) != 1:
    raise SystemExit("PREPARE=FAIL_HTTP_SERVER_ANCHOR")
text = text.replace(http_name_anchor, http_name_replacement, 1)

merchant_server = """

  server {
    listen 443 ssl;
    server_name api-v2-dev.nnviopp.com;

    add_header Strict-Transport-Security 'max-age=31536000' always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;

    location = /api/pay/unifiedOrder {
      allow 34.92.245.74;
      allow 34.92.52.162;
      deny all;
      limit_except POST { deny all; }
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $remote_addr;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 150s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_merchant_api;
    }

    location = /api/pay/query {
      allow 34.92.245.74;
      allow 34.92.52.162;
      deny all;
      limit_except POST { deny all; }
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $remote_addr;
      proxy_set_header X-Forwarded-Proto https;
      proxy_set_header Connection '';
      proxy_connect_timeout 5s;
      proxy_read_timeout 60s;
      proxy_send_timeout 60s;
      proxy_pass http://jee8pay_v2_merchant_api;
    }

    location / {
      return 404;
    }
  }
"""
if not text.endswith("}\n"):
    raise SystemExit("PREPARE=FAIL_HTTP_TAIL_ANCHOR")
text = text[:-2] + merchant_server + "}\n"

temporary = TARGET.with_name(".nginx.proposed.conf.tmp")
temporary.write_text(text, encoding="utf-8")
os.chown(temporary, 0, 10002)
os.chmod(temporary, 0o640)
temporary.replace(TARGET)
print("PREPARE=PASS")
print("V1_SOURCE_MODIFIED=NO")
print("PROPOSED_SHA256=" + hashlib.sha256(text.encode("utf-8")).hexdigest())
