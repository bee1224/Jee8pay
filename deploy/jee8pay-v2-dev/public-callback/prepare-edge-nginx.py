#!/usr/bin/env python3
import hashlib
import os
from pathlib import Path

SOURCE = Path("/opt/payment/payment-service-sandbox/edge/nginx.conf")
TARGET = Path("/opt/jee8pay-v2-dev/public-callback/nginx.proposed.conf")
EXPECTED_SHA256 = "10a4877269bae2e624e26554a166078bff441545a26ef87f26d1549f3fe1c3a4"

source_bytes = SOURCE.read_bytes()
if hashlib.sha256(source_bytes).hexdigest() != EXPECTED_SHA256:
    raise SystemExit("PREPARE=FAIL_V1_NGINX_BASELINE_CHANGED")
text = source_bytes.decode("utf-8")

upstream_anchor = """  upstream merchant_receiver {
    server merchant-sandbox:8281;
    keepalive 8;
  }
"""
upstream_replacement = upstream_anchor + """
  upstream jee8pay_v2_callback {
    server jee8pay-v2-callback:8080;
    keepalive 4;
  }
"""
if text.count(upstream_anchor) != 1:
    raise SystemExit("PREPARE=FAIL_UPSTREAM_ANCHOR")
text = text.replace(upstream_anchor, upstream_replacement, 1)

http_name_anchor = "    server_name sandbox-api.nnviopp.com sandbox.nnviopp.com merchant-sandbox.nnviopp.com;"
http_name_replacement = "    server_name sandbox-api.nnviopp.com sandbox.nnviopp.com merchant-sandbox.nnviopp.com ccat-v2-dev.nnviopp.com;"
if text.count(http_name_anchor) != 1:
    raise SystemExit("PREPARE=FAIL_HTTP_SERVER_ANCHOR")
text = text.replace(http_name_anchor, http_name_replacement, 1)

callback_server = """

  server {
    listen 443 ssl;
    server_name ccat-v2-dev.nnviopp.com;

    add_header Strict-Transport-Security 'max-age=31536000' always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;

    location = /api/pay/notify/ryo {
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
    }

    location = /api/pay/notify/jay {
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
    }

    location = /api/pay/notify/chi {
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
    }

    location / {
      return 404;
    }
  }
"""
if not text.endswith("}\n"):
    raise SystemExit("PREPARE=FAIL_HTTP_TAIL_ANCHOR")
text = text[:-2] + callback_server + "}\n"

temporary = TARGET.with_name(".nginx.proposed.conf.tmp")
temporary.write_text(text, encoding="utf-8")
os.chown(temporary, 0, 10002)
os.chmod(temporary, 0o640)
temporary.replace(TARGET)
print("PREPARE=PASS")
print("V1_SOURCE_MODIFIED=NO")
