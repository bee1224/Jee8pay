# Project file tree

Generated from `/mnt/c/Users/tim.huang/Documents/Jee8pay` on 2026-08-16.

Excluded generated/metadata directories: `.git`, `node_modules`, `vendor`, `dist`, `build`, `target`, `.idea`, and `.vscode`.

Regenerate with:

```bash
tree -a -I '.git|node_modules|vendor|dist|build|target|.idea|.vscode' --dirsfirst .
```

```text
.
├── .agents
│   ├── skills
│   │   ├── git-delivery
│   │   │   ├── agents
│   │   │   │   └── openai.yaml
│   │   │   └── SKILL.md
│   │   └── jeepay-provider-development
│   │       └── SKILL.md
│   └── tmp
│       ├── n01r1-continuation
│       │   └── final-medium-report.txt
│       ├── d01-blackbox-results-20260815.txt
│       └── run-d01-blackbox.py
├── .codex
│   └── config.toml
├── deploy
│   ├── jee8pay-v2-dev
│   │   ├── .secrets
│   │   ├── artifacts
│   │   │   ├── sql
│   │   │   │   └── init.sql
│   │   │   ├── ui-cashier
│   │   │   │   ├── assets
│   │   │   │   │   ├── Alipay-b499c351.css
│   │   │   │   │   ├── Alipay-ddd74962.js
│   │   │   │   │   ├── Cashier-d4d1a3b7.js
│   │   │   │   │   ├── Error-616e46bd.css
│   │   │   │   │   ├── Error-c5f54b97.js
│   │   │   │   │   ├── Hub-12a29291.css
│   │   │   │   │   ├── Hub-dd892559.js
│   │   │   │   │   ├── Oauth2Callback-3efe3b41.js
│   │   │   │   │   ├── S-f63feb6b.svg
│   │   │   │   │   ├── WeChatSansSS-Bold-41710157.ttf
│   │   │   │   │   ├── Wxpay-46507991.js
│   │   │   │   │   ├── Wxpay-ded61352.css
│   │   │   │   │   ├── Ysfpay-60b34adf.css
│   │   │   │   │   ├── Ysfpay-ee39b3ce.js
│   │   │   │   │   ├── api-669d0d7b.js
│   │   │   │   │   ├── error-a2400a96.svg
│   │   │   │   │   ├── index-291e8efe.css
│   │   │   │   │   ├── index-c6178d44.js
│   │   │   │   │   ├── wx-d01ab358.svg
│   │   │   │   │   ├── ysf-fe0ceebe.jpg
│   │   │   │   │   └── zfb-2f9e1442.jpeg
│   │   │   │   ├── favicon.ico
│   │   │   │   └── index.html
│   │   │   ├── ui-manager
│   │   │   │   ├── assets
│   │   │   │   │   ├── 404.cd1a2daf.svg
│   │   │   │   │   ├── 404.f677d1c6.js
│   │   │   │   │   ├── Analysis.0969540b.css
│   │   │   │   │   ├── Analysis.2d1c18d0.js
│   │   │   │   │   ├── Badge.5ccac945.js
│   │   │   │   │   ├── EntPage.b7a48ba8.js
│   │   │   │   │   ├── Group.384b07b1.js
│   │   │   │   │   ├── IsvList.022149ec.js
│   │   │   │   │   ├── IsvList.0679ddfb.css
│   │   │   │   │   ├── JeepayCard.38beb612.js
│   │   │   │   │   ├── JeepayCard.aadc8955.css
│   │   │   │   │   ├── JeepayTable.33df05d8.js
│   │   │   │   │   ├── JeepayTable.caf4a94b.css
│   │   │   │   │   ├── JeepayTableColState.069d3bc3.js
│   │   │   │   │   ├── JeepayTableColumns.vue_vue_type_script_lang.7cab347f.css
│   │   │   │   │   ├── JeepayTableColumns.vue_vue_type_script_lang.ec04ce14.js
│   │   │   │   │   ├── JeepayTextUp.28a14f52.js
│   │   │   │   │   ├── JeepayTextUp.ce597964.css
│   │   │   │   │   ├── JeepayUpload.96420dc1.js
│   │   │   │   │   ├── List.285f74e4.js
│   │   │   │   │   ├── List.48c20357.js
│   │   │   │   │   ├── List.d27341cc.css
│   │   │   │   │   ├── List.dcacd29c.js
│   │   │   │   │   ├── List.e6024f81.css
│   │   │   │   │   ├── MchList.53747eb3.css
│   │   │   │   │   ├── MchList.ff7d0e76.js
│   │   │   │   │   ├── MchNotifyList.a5afdbee.js
│   │   │   │   │   ├── PayOrderList.8e3e09a5.css
│   │   │   │   │   ├── PayOrderList.c1a4d566.js
│   │   │   │   │   ├── RefundOrderList.07c8945b.js
│   │   │   │   │   ├── RefundOrderList.40082279.css
│   │   │   │   │   ├── RolePage.64a3f0f3.js
│   │   │   │   │   ├── SysConfig.012db407.js
│   │   │   │   │   ├── SysLog.e0ce6b2f.js
│   │   │   │   │   ├── SysUserPage.1c45b846.js
│   │   │   │   │   ├── TabPane.43fa8453.js
│   │   │   │   │   ├── TransferOrderList.33488a9f.css
│   │   │   │   │   ├── TransferOrderList.7b8810bb.js
│   │   │   │   │   ├── UserinfoPage.6ededfd9.css
│   │   │   │   │   ├── UserinfoPage.b557a19d.js
│   │   │   │   │   ├── add-icon-hover.f92dc310.svg
│   │   │   │   │   ├── add-icon.bdbbe2d3.svg
│   │   │   │   │   ├── background.12aecf48.svg
│   │   │   │   │   ├── bootstrap-icons.476adf42.woff2
│   │   │   │   │   ├── bootstrap-icons.bb1de989.woff
│   │   │   │   │   ├── dayjs.007de484.js
│   │   │   │   │   ├── empty.cc1bea71.svg
│   │   │   │   │   ├── favicon.db8e62f3.ico
│   │   │   │   │   ├── index.3556c4bc.js
│   │   │   │   │   ├── index.370b9573.css
│   │   │   │   │   ├── index.8ae799d6.js
│   │   │   │   │   ├── index.b6fd963a.js
│   │   │   │   │   ├── index.cfc27556.js
│   │   │   │   │   ├── index.d63f213e.js
│   │   │   │   │   ├── index.e447018d.js
│   │   │   │   │   ├── index.eadfa88f.js
│   │   │   │   │   ├── index.fd3319f9.js
│   │   │   │   │   ├── jeepay.e180c5c7.svg
│   │   │   │   │   ├── logo-j.612b880b.svg
│   │   │   │   │   ├── logo.070cbf2b.svg
│   │   │   │   │   ├── manage.4b139a94.js
│   │   │   │   │   ├── moment.40bc58bf.js
│   │   │   │   │   ├── more.3d1a3462.svg
│   │   │   │   │   ├── operate.ebec203e.svg
│   │   │   │   │   └── useRefs.461c258b.js
│   │   │   │   └── index.html
│   │   │   ├── ui-merchant
│   │   │   │   ├── assets
│   │   │   │   │   ├── 403.27c3e2bf.js
│   │   │   │   │   ├── 403.b7b8bff3.svg
│   │   │   │   │   ├── 404.34857be5.js
│   │   │   │   │   ├── 404.cd1a2daf.svg
│   │   │   │   │   ├── 500.f4af831f.svg
│   │   │   │   │   ├── 500.f9037ef6.js
│   │   │   │   │   ├── Analysis.4ef6d8e2.js
│   │   │   │   │   ├── Analysis.97047235.css
│   │   │   │   │   ├── ChannelUserModal.6f5e6627.js
│   │   │   │   │   ├── ChannelUserModal.9f76165f.css
│   │   │   │   │   ├── DivisionReceiverGroupPage.dff58829.js
│   │   │   │   │   ├── DivisionReceiverPage.79497dd8.js
│   │   │   │   │   ├── DivisionRecordPage.452384f7.js
│   │   │   │   │   ├── JeepayTextUp.ba686124.js
│   │   │   │   │   ├── JeepayTextUp.eba82935.css
│   │   │   │   │   ├── JeepayUpload.aab5e02a.js
│   │   │   │   │   ├── List.211d4bd4.js
│   │   │   │   │   ├── List.c1ce5708.css
│   │   │   │   │   ├── MchTransferPage.09fce2ee.css
│   │   │   │   │   ├── MchTransferPage.48449dab.js
│   │   │   │   │   ├── PayOrderList.044f091f.js
│   │   │   │   │   ├── PayOrderList.f247aeca.css
│   │   │   │   │   ├── PayTest.63f30da8.css
│   │   │   │   │   ├── PayTest.ee8a5039.js
│   │   │   │   │   ├── RefundOrderList.2cf92e7f.js
│   │   │   │   │   ├── RefundOrderList.fbf76502.css
│   │   │   │   │   ├── RolePage.bf116724.js
│   │   │   │   │   ├── SysUserPage.447b132f.js
│   │   │   │   │   ├── TransferOrderList.1ed48881.js
│   │   │   │   │   ├── TransferOrderList.b0368e95.css
│   │   │   │   │   ├── UserinfoPage.83b78319.css
│   │   │   │   │   ├── UserinfoPage.9a47f12c.js
│   │   │   │   │   ├── add-icon.bdbbe2d3.svg
│   │   │   │   │   ├── ali_app.d95204fd.svg
│   │   │   │   │   ├── ali_bar.ae8ad12d.svg
│   │   │   │   │   ├── ali_jsapi.3313770d.svg
│   │   │   │   │   ├── ali_pc.79c144c7.svg
│   │   │   │   │   ├── ali_qr.2ac2aff2.svg
│   │   │   │   │   ├── ali_wap.a96bc28e.svg
│   │   │   │   │   ├── auto_bar.fe9f7181.svg
│   │   │   │   │   ├── background.12aecf48.svg
│   │   │   │   │   ├── bootstrap-icons.476adf42.woff2
│   │   │   │   │   ├── bootstrap-icons.bb1de989.woff
│   │   │   │   │   ├── empty.cc1bea71.svg
│   │   │   │   │   ├── favicon.db8e62f3.ico
│   │   │   │   │   ├── index.0c7a5709.css
│   │   │   │   │   ├── index.2cd5f568.js
│   │   │   │   │   ├── jeepay.e180c5c7.svg
│   │   │   │   │   ├── logo-j.612b880b.svg
│   │   │   │   │   ├── logo.070cbf2b.svg
│   │   │   │   │   ├── manage.4d31c1b2.js
│   │   │   │   │   ├── more.3d1a3462.svg
│   │   │   │   │   ├── operate.ebec203e.svg
│   │   │   │   │   ├── pp_pc.d45d0279.svg
│   │   │   │   │   ├── qr_cashier.b2e47514.svg
│   │   │   │   │   ├── reconnecting-websocket.97937d47.js
│   │   │   │   │   ├── scan.0364cbce.svg
│   │   │   │   │   ├── wx_app.55df45ee.svg
│   │   │   │   │   ├── wx_bar.2cc064e8.svg
│   │   │   │   │   ├── wx_h5.d9e7c5b8.svg
│   │   │   │   │   ├── wx_jsapi.19339d08.svg
│   │   │   │   │   └── wx_native.c32e17bc.svg
│   │   │   │   ├── imgs
│   │   │   │   │   ├── defava_f.png
│   │   │   │   │   ├── defava_m.png
│   │   │   │   │   ├── favicon.ico
│   │   │   │   │   └── logo.svg
│   │   │   │   └── index.html
│   │   │   ├── SHA256SUMS
│   │   │   ├── jeepay-manager.jar
│   │   │   ├── jeepay-merchant.jar
│   │   │   └── jeepay-payment.jar
│   │   ├── config
│   │   │   ├── application.yml
│   │   │   ├── callback-ingress.conf
│   │   │   ├── nginx-default.conf.template
│   │   │   └── rocketmq-broker.conf
│   │   ├── merchant-uat
│   │   │   ├── apply-edge-hot
│   │   │   ├── cloudflare-ips-v4.txt
│   │   │   ├── cloudflare-ips-v6.txt
│   │   │   ├── compose.yaml
│   │   │   ├── nginx.conf
│   │   │   ├── prepare-edge-nginx.py
│   │   │   ├── provision-merchant
│   │   │   └── rollback-edge-hot
│   │   ├── public-callback
│   │   │   ├── apply-edge-hot
│   │   │   ├── compose.edge-overlay.yaml
│   │   │   ├── prepare-edge-nginx.py
│   │   │   └── rollback-edge-hot
│   │   ├── scripts
│   │   │   ├── capture-sandbox-edge-state.sh
│   │   │   ├── manage-sandbox-api-v2-edge.sh
│   │   │   ├── manage-sandbox-ccat-v2-dns.sh
│   │   │   ├── manage-sandbox-merchant-uat-dns.sh
│   │   │   ├── monitor-uat.sh
│   │   │   ├── reconcile-sandbox-edge.sh
│   │   │   ├── refresh-cloudflare-ip-ranges.sh
│   │   │   └── validate-sandbox-edge.sh
│   │   ├── .gitignore
│   │   ├── DEPLOYMENT-MANIFEST.sha256
│   │   ├── Dockerfile.backend
│   │   ├── Dockerfile.ui
│   │   └── compose.yml
│   └── jee8pay-v2-production
│       ├── artifacts
│       │   ├── sql
│       │   │   └── init.sql
│       │   ├── ui-cashier
│       │   │   ├── assets
│       │   │   │   ├── Alipay-b499c351.css
│       │   │   │   ├── Alipay-ddd74962.js
│       │   │   │   ├── Cashier-d4d1a3b7.js
│       │   │   │   ├── Error-616e46bd.css
│       │   │   │   ├── Error-c5f54b97.js
│       │   │   │   ├── Hub-12a29291.css
│       │   │   │   ├── Hub-dd892559.js
│       │   │   │   ├── Oauth2Callback-3efe3b41.js
│       │   │   │   ├── S-f63feb6b.svg
│       │   │   │   ├── WeChatSansSS-Bold-41710157.ttf
│       │   │   │   ├── Wxpay-46507991.js
│       │   │   │   ├── Wxpay-ded61352.css
│       │   │   │   ├── Ysfpay-60b34adf.css
│       │   │   │   ├── Ysfpay-ee39b3ce.js
│       │   │   │   ├── api-669d0d7b.js
│       │   │   │   ├── error-a2400a96.svg
│       │   │   │   ├── index-291e8efe.css
│       │   │   │   ├── index-c6178d44.js
│       │   │   │   ├── wx-d01ab358.svg
│       │   │   │   ├── ysf-fe0ceebe.jpg
│       │   │   │   └── zfb-2f9e1442.jpeg
│       │   │   ├── favicon.ico
│       │   │   └── index.html
│       │   ├── ui-manager
│       │   │   ├── assets
│       │   │   │   ├── 404.cd1a2daf.svg
│       │   │   │   ├── 404.f677d1c6.js
│       │   │   │   ├── Analysis.0969540b.css
│       │   │   │   ├── Analysis.2d1c18d0.js
│       │   │   │   ├── Badge.5ccac945.js
│       │   │   │   ├── EntPage.b7a48ba8.js
│       │   │   │   ├── Group.384b07b1.js
│       │   │   │   ├── IsvList.022149ec.js
│       │   │   │   ├── IsvList.0679ddfb.css
│       │   │   │   ├── JeepayCard.38beb612.js
│       │   │   │   ├── JeepayCard.aadc8955.css
│       │   │   │   ├── JeepayTable.33df05d8.js
│       │   │   │   ├── JeepayTable.caf4a94b.css
│       │   │   │   ├── JeepayTableColState.069d3bc3.js
│       │   │   │   ├── JeepayTableColumns.vue_vue_type_script_lang.7cab347f.css
│       │   │   │   ├── JeepayTableColumns.vue_vue_type_script_lang.ec04ce14.js
│       │   │   │   ├── JeepayTextUp.28a14f52.js
│       │   │   │   ├── JeepayTextUp.ce597964.css
│       │   │   │   ├── JeepayUpload.96420dc1.js
│       │   │   │   ├── List.285f74e4.js
│       │   │   │   ├── List.48c20357.js
│       │   │   │   ├── List.d27341cc.css
│       │   │   │   ├── List.dcacd29c.js
│       │   │   │   ├── List.e6024f81.css
│       │   │   │   ├── MchList.53747eb3.css
│       │   │   │   ├── MchList.ff7d0e76.js
│       │   │   │   ├── MchNotifyList.a5afdbee.js
│       │   │   │   ├── PayOrderList.8e3e09a5.css
│       │   │   │   ├── PayOrderList.c1a4d566.js
│       │   │   │   ├── RefundOrderList.07c8945b.js
│       │   │   │   ├── RefundOrderList.40082279.css
│       │   │   │   ├── RolePage.64a3f0f3.js
│       │   │   │   ├── SysConfig.012db407.js
│       │   │   │   ├── SysLog.e0ce6b2f.js
│       │   │   │   ├── SysUserPage.1c45b846.js
│       │   │   │   ├── TabPane.43fa8453.js
│       │   │   │   ├── TransferOrderList.33488a9f.css
│       │   │   │   ├── TransferOrderList.7b8810bb.js
│       │   │   │   ├── UserinfoPage.6ededfd9.css
│       │   │   │   ├── UserinfoPage.b557a19d.js
│       │   │   │   ├── add-icon-hover.f92dc310.svg
│       │   │   │   ├── add-icon.bdbbe2d3.svg
│       │   │   │   ├── background.12aecf48.svg
│       │   │   │   ├── bootstrap-icons.476adf42.woff2
│       │   │   │   ├── bootstrap-icons.bb1de989.woff
│       │   │   │   ├── dayjs.007de484.js
│       │   │   │   ├── empty.cc1bea71.svg
│       │   │   │   ├── favicon.db8e62f3.ico
│       │   │   │   ├── index.3556c4bc.js
│       │   │   │   ├── index.370b9573.css
│       │   │   │   ├── index.8ae799d6.js
│       │   │   │   ├── index.b6fd963a.js
│       │   │   │   ├── index.cfc27556.js
│       │   │   │   ├── index.d63f213e.js
│       │   │   │   ├── index.e447018d.js
│       │   │   │   ├── index.eadfa88f.js
│       │   │   │   ├── index.fd3319f9.js
│       │   │   │   ├── jeepay.e180c5c7.svg
│       │   │   │   ├── logo-j.612b880b.svg
│       │   │   │   ├── logo.070cbf2b.svg
│       │   │   │   ├── manage.4b139a94.js
│       │   │   │   ├── moment.40bc58bf.js
│       │   │   │   ├── more.3d1a3462.svg
│       │   │   │   ├── operate.ebec203e.svg
│       │   │   │   └── useRefs.461c258b.js
│       │   │   └── index.html
│       │   ├── ui-merchant
│       │   │   ├── assets
│       │   │   │   ├── 403.27c3e2bf.js
│       │   │   │   ├── 403.b7b8bff3.svg
│       │   │   │   ├── 404.34857be5.js
│       │   │   │   ├── 404.cd1a2daf.svg
│       │   │   │   ├── 500.f4af831f.svg
│       │   │   │   ├── 500.f9037ef6.js
│       │   │   │   ├── Analysis.4ef6d8e2.js
│       │   │   │   ├── Analysis.97047235.css
│       │   │   │   ├── ChannelUserModal.6f5e6627.js
│       │   │   │   ├── ChannelUserModal.9f76165f.css
│       │   │   │   ├── DivisionReceiverGroupPage.dff58829.js
│       │   │   │   ├── DivisionReceiverPage.79497dd8.js
│       │   │   │   ├── DivisionRecordPage.452384f7.js
│       │   │   │   ├── JeepayTextUp.ba686124.js
│       │   │   │   ├── JeepayTextUp.eba82935.css
│       │   │   │   ├── JeepayUpload.aab5e02a.js
│       │   │   │   ├── List.211d4bd4.js
│       │   │   │   ├── List.c1ce5708.css
│       │   │   │   ├── MchTransferPage.09fce2ee.css
│       │   │   │   ├── MchTransferPage.48449dab.js
│       │   │   │   ├── PayOrderList.044f091f.js
│       │   │   │   ├── PayOrderList.f247aeca.css
│       │   │   │   ├── PayTest.63f30da8.css
│       │   │   │   ├── PayTest.ee8a5039.js
│       │   │   │   ├── RefundOrderList.2cf92e7f.js
│       │   │   │   ├── RefundOrderList.fbf76502.css
│       │   │   │   ├── RolePage.bf116724.js
│       │   │   │   ├── SysUserPage.447b132f.js
│       │   │   │   ├── TransferOrderList.1ed48881.js
│       │   │   │   ├── TransferOrderList.b0368e95.css
│       │   │   │   ├── UserinfoPage.83b78319.css
│       │   │   │   ├── UserinfoPage.9a47f12c.js
│       │   │   │   ├── add-icon.bdbbe2d3.svg
│       │   │   │   ├── ali_app.d95204fd.svg
│       │   │   │   ├── ali_bar.ae8ad12d.svg
│       │   │   │   ├── ali_jsapi.3313770d.svg
│       │   │   │   ├── ali_pc.79c144c7.svg
│       │   │   │   ├── ali_qr.2ac2aff2.svg
│       │   │   │   ├── ali_wap.a96bc28e.svg
│       │   │   │   ├── auto_bar.fe9f7181.svg
│       │   │   │   ├── background.12aecf48.svg
│       │   │   │   ├── bootstrap-icons.476adf42.woff2
│       │   │   │   ├── bootstrap-icons.bb1de989.woff
│       │   │   │   ├── empty.cc1bea71.svg
│       │   │   │   ├── favicon.db8e62f3.ico
│       │   │   │   ├── index.0c7a5709.css
│       │   │   │   ├── index.2cd5f568.js
│       │   │   │   ├── jeepay.e180c5c7.svg
│       │   │   │   ├── logo-j.612b880b.svg
│       │   │   │   ├── logo.070cbf2b.svg
│       │   │   │   ├── manage.4d31c1b2.js
│       │   │   │   ├── more.3d1a3462.svg
│       │   │   │   ├── operate.ebec203e.svg
│       │   │   │   ├── pp_pc.d45d0279.svg
│       │   │   │   ├── qr_cashier.b2e47514.svg
│       │   │   │   ├── reconnecting-websocket.97937d47.js
│       │   │   │   ├── scan.0364cbce.svg
│       │   │   │   ├── wx_app.55df45ee.svg
│       │   │   │   ├── wx_bar.2cc064e8.svg
│       │   │   │   ├── wx_h5.d9e7c5b8.svg
│       │   │   │   ├── wx_jsapi.19339d08.svg
│       │   │   │   └── wx_native.c32e17bc.svg
│       │   │   ├── imgs
│       │   │   │   ├── defava_f.png
│       │   │   │   ├── defava_m.png
│       │   │   │   ├── favicon.ico
│       │   │   │   └── logo.svg
│       │   │   └── index.html
│       │   ├── jeepay-manager.jar
│       │   ├── jeepay-merchant.jar
│       │   └── jeepay-payment.jar
│       ├── config
│       │   ├── application.yml
│       │   ├── callback-ingress.conf
│       │   ├── nginx-default.conf.template
│       │   ├── production-bootstrap.sql
│       │   └── rocketmq-broker.conf
│       ├── scripts
│       │   ├── install-v2-infrastructure-secrets
│       │   └── populate-v2-ccat-secret
│       ├── .gitignore
│       ├── DEPLOYMENT-MANIFEST.sha256
│       ├── Dockerfile.backend
│       ├── Dockerfile.ui
│       ├── SOURCE
│       └── compose.yml
├── docs
│   ├── architecture
│   │   ├── README.md
│   │   ├── environment-contract.md
│   │   ├── provider-extension-model.md
│   │   ├── source-provenance.md
│   │   └── taiwan-platform-baseline.md
│   ├── debt
│   │   ├── README.md
│   │   └── technical-debt-register.md
│   ├── decisions
│   │   ├── ADR-0001-jeepay-as-platform-core.md
│   │   ├── ADR-0002-native-provider-extension-contract.md
│   │   ├── ADR-0003-single-root-monorepo.md
│   │   ├── ADR-0004-twd-platform-default.md
│   │   ├── ADR-0005-environment-isolation.md
│   │   ├── ADR-0006-taipei-platform-timezone.md
│   │   └── README.md
│   ├── integration
│   │   ├── merchant-uat
│   │   │   ├── examples
│   │   │   │   ├── create-vector.json
│   │   │   │   ├── notify-vector.json
│   │   │   │   ├── run-d01-blackbox.py
│   │   │   │   ├── unified-order-success.json
│   │   │   │   └── verify_vectors.py
│   │   │   ├── JEE-EC01R1-external-consumer-closure.md
│   │   │   ├── README.md
│   │   │   └── UAT-START-NOTICE.md
│   │   └── README.md
│   ├── operations
│   │   ├── README.md
│   │   ├── ccat-v2-development.md
│   │   ├── ccat-v2-production-candidate.md
│   │   ├── git-delivery-governance.md
│   │   ├── merchant-uat-frontend-operator-map.md
│   │   └── sandbox-edge-recovery.md
│   ├── providers
│   │   ├── ccat
│   │   │   ├── JEE-E05-external-create-investigation.md
│   │   │   ├── JEE-P05-create-failure-semantics.md
│   │   │   ├── JEE-P05R1-i07-blocker-closure.md
│   │   │   ├── README.md
│   │   │   ├── contract-evidence.md
│   │   │   └── provider-design.md
│   │   └── README.md
│   └── README.md
├── jeepay
│   ├── .github
│   │   └── workflows
│   │       └── ci.yml
│   ├── conf
│   │   ├── devCommons
│   │   │   └── config
│   │   │       └── application.yml
│   │   ├── manager
│   │   │   └── application.yml
│   │   ├── merchant
│   │   │   └── application.yml
│   │   ├── payment
│   │   │   └── application.yml
│   │   └── readme.txt
│   ├── docker
│   │   ├── activemq
│   │   │   ├── Dockerfile
│   │   │   └── activemq.xml
│   │   ├── rabbitmq
│   │   │   ├── Dockerfile
│   │   │   ├── limits.ejs
│   │   │   └── users.ejs
│   │   ├── rocketmq
│   │   │   └── broker
│   │   │       └── conf
│   │   │           ├── broker.conf
│   │   │           └── broker.conf.template
│   │   ├── build-docker-starter.sh
│   │   ├── nginx.sh
│   │   ├── publish-dockerhub.sh
│   │   ├── publish-swr.sh
│   │   ├── push-to-docker.md
│   │   ├── run-dockerhub-mcp.sh
│   │   └── sync-swr-thirdparty.sh
│   ├── docs
│   │   ├── deploy
│   │   │   ├── baota.md
│   │   │   ├── compose.md
│   │   │   ├── https.md
│   │   │   ├── publish.md
│   │   │   ├── shell.md
│   │   │   └── troubleshooting.md
│   │   ├── install
│   │   │   ├── include
│   │   │   │   ├── my.cnf
│   │   │   │   ├── nginx.conf
│   │   │   │   └── redis.conf
│   │   │   ├── config.sh
│   │   │   ├── install.sh
│   │   │   ├── test_install_rocketmq.sh
│   │   │   ├── test_swr_defaults.sh
│   │   │   ├── test_uninstall_and_publish.sh
│   │   │   └── uninstall.sh
│   │   ├── script
│   │   │   └── app.sh
│   │   ├── sql
│   │   │   ├── init.sql
│   │   │   └── patch.sql
│   │   ├── features.md
│   │   ├── project-structure.md
│   │   └── screenshots.md
│   ├── jeepay-components
│   │   ├── jeepay-components-mq
│   │   │   ├── src
│   │   │   │   ├── main
│   │   │   │   │   ├── java
│   │   │   │   │   │   └── com
│   │   │   │   │   │       └── jeequan
│   │   │   │   │   │           └── jeepay
│   │   │   │   │   │               └── components
│   │   │   │   │   │                   └── mq
│   │   │   │   │   │                       ├── constant
│   │   │   │   │   │                       │   ├── MQSendTypeEnum.java
│   │   │   │   │   │                       │   └── MQVenderCS.java
│   │   │   │   │   │                       ├── executor
│   │   │   │   │   │                       │   └── MqThreadExecutor.java
│   │   │   │   │   │                       ├── model
│   │   │   │   │   │                       │   ├── AbstractMQ.java
│   │   │   │   │   │                       │   ├── CleanMchLoginAuthCacheMQ.java
│   │   │   │   │   │                       │   ├── PayOrderDivisionMQ.java
│   │   │   │   │   │                       │   ├── PayOrderMchNotifyMQ.java
│   │   │   │   │   │                       │   ├── PayOrderReissueMQ.java
│   │   │   │   │   │                       │   ├── ResetAppConfigMQ.java
│   │   │   │   │   │                       │   └── ResetIsvMchAppInfoConfigMQ.java
│   │   │   │   │   │                       └── vender
│   │   │   │   │   │                           ├── activemq
│   │   │   │   │   │                           │   ├── receive
│   │   │   │   │   │                           │   │   ├── CleanMchLoginAuthCacheActiveMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderDivisionActiveMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderMchNotifyActiveMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderReissueActiveMQReceiver.java
│   │   │   │   │   │                           │   │   ├── ResetAppConfigActiveMQReceiver.java
│   │   │   │   │   │                           │   │   └── ResetIsvMchAppInfoActiveMQReceiver.java
│   │   │   │   │   │                           │   ├── ActiveMQConfig.java
│   │   │   │   │   │                           │   └── ActiveMQSender.java
│   │   │   │   │   │                           ├── aliyunrocketmq
│   │   │   │   │   │                           │   ├── receive
│   │   │   │   │   │                           │   │   ├── CleanMchLoginAuthCacheAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderDivisionAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderMchNotifyAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderReissueAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── ResetAppConfigAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   │   └── ResetIsvMchAppInfoAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   ├── AbstractAliYunRocketMQReceiver.java
│   │   │   │   │   │                           │   ├── AliYunRocketMQFactory.java
│   │   │   │   │   │                           │   └── AliYunRocketMQSender.java
│   │   │   │   │   │                           ├── rabbitmq
│   │   │   │   │   │                           │   ├── receive
│   │   │   │   │   │                           │   │   ├── CleanMchLoginAuthCacheRabbitMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderDivisionRabbitMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderMchNotifyRabbitMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderReissueRabbitMQReceiver.java
│   │   │   │   │   │                           │   │   ├── ResetAppConfigRabbitMQReceiver.java
│   │   │   │   │   │                           │   │   └── ResetIsvMchAppInfoRabbitMQReceiver.java
│   │   │   │   │   │                           │   ├── RabbitMQBeanProcessor.java
│   │   │   │   │   │                           │   ├── RabbitMQConfig.java
│   │   │   │   │   │                           │   └── RabbitMQSender.java
│   │   │   │   │   │                           ├── rocketmq
│   │   │   │   │   │                           │   ├── config
│   │   │   │   │   │                           │   │   ├── JeepayRocketMqAutoConfiguration.java
│   │   │   │   │   │                           │   │   └── JeepayRocketMqEnvironmentPostProcessor.java
│   │   │   │   │   │                           │   ├── receive
│   │   │   │   │   │                           │   │   ├── CleanMchLoginAuthCacheRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderDivisionRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderMchNotifyRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── PayOrderReissueRocketMQReceiver.java
│   │   │   │   │   │                           │   │   ├── ResetAppConfigRocketMQReceiver.java
│   │   │   │   │   │                           │   │   └── ResetIsvMchAppInfoRocketMQReceiver.java
│   │   │   │   │   │                           │   └── RocketMQSender.java
│   │   │   │   │   │                           ├── IMQMsgReceiver.java
│   │   │   │   │   │                           └── IMQSender.java
│   │   │   │   │   └── resources
│   │   │   │   │       └── META-INF
│   │   │   │   │           ├── spring
│   │   │   │   │           │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   │   │   │   │           └── spring.factories
│   │   │   │   └── test
│   │   │   │       ├── java
│   │   │   │       │   └── com
│   │   │   │       │       ├── jeequan
│   │   │   │       │       │   └── jeepay
│   │   │   │       │       │       └── components
│   │   │   │       │       │           └── mq
│   │   │   │       │       │               └── vender
│   │   │   │       │       │                   └── rocketmq
│   │   │   │       │       │                       └── config
│   │   │   │       │       │                           ├── JeepayRocketMqClasspathCompatibilityTest.java
│   │   │   │       │       │                           └── JeepayRocketMqEnvironmentPostProcessorTest.java
│   │   │   │       │       └── .gitkeep
│   │   │   │       └── resources
│   │   │   │           └── .gitkeep
│   │   │   └── pom.xml
│   │   ├── jeepay-components-oss
│   │   │   ├── src
│   │   │   │   ├── main
│   │   │   │   │   └── java
│   │   │   │   │       └── com
│   │   │   │   │           └── jeequan
│   │   │   │   │               └── jeepay
│   │   │   │   │                   └── components
│   │   │   │   │                       └── oss
│   │   │   │   │                           ├── config
│   │   │   │   │                           │   ├── AliyunOssYmlConfig.java
│   │   │   │   │                           │   └── OssYmlConfig.java
│   │   │   │   │                           ├── constant
│   │   │   │   │                           │   ├── OssSavePlaceEnum.java
│   │   │   │   │                           │   └── OssServiceTypeEnum.java
│   │   │   │   │                           ├── ctrl
│   │   │   │   │                           │   └── OssFileController.java
│   │   │   │   │                           ├── model
│   │   │   │   │                           │   └── OssFileConfig.java
│   │   │   │   │                           └── service
│   │   │   │   │                               ├── AliyunOssService.java
│   │   │   │   │                               ├── IOssService.java
│   │   │   │   │                               └── LocalFileService.java
│   │   │   │   └── test
│   │   │   │       ├── java
│   │   │   │       │   └── com
│   │   │   │       │       └── .gitkeep
│   │   │   │       └── resources
│   │   │   │           └── .gitkeep
│   │   │   └── pom.xml
│   │   └── pom.xml
│   ├── jeepay-core
│   │   ├── src
│   │   │   ├── main
│   │   │   │   └── java
│   │   │   │       └── com
│   │   │   │           └── jeequan
│   │   │   │               └── jeepay
│   │   │   │                   └── core
│   │   │   │                       ├── aop
│   │   │   │                       │   └── MethodLog.java
│   │   │   │                       ├── beans
│   │   │   │                       │   └── RequestKitBean.java
│   │   │   │                       ├── cache
│   │   │   │                       │   ├── ITokenService.java
│   │   │   │                       │   └── RedisUtil.java
│   │   │   │                       ├── constants
│   │   │   │                       │   ├── ApiCodeEnum.java
│   │   │   │                       │   └── CS.java
│   │   │   │                       ├── ctrls
│   │   │   │                       │   └── AbstractCtrl.java
│   │   │   │                       ├── entity
│   │   │   │                       │   ├── IsvInfo.java
│   │   │   │                       │   ├── MchApp.java
│   │   │   │                       │   ├── MchDivisionReceiver.java
│   │   │   │                       │   ├── MchDivisionReceiverGroup.java
│   │   │   │                       │   ├── MchInfo.java
│   │   │   │                       │   ├── MchNotifyRecord.java
│   │   │   │                       │   ├── MchPayPassage.java
│   │   │   │                       │   ├── PayInterfaceConfig.java
│   │   │   │                       │   ├── PayInterfaceDefine.java
│   │   │   │                       │   ├── PayOrder.java
│   │   │   │                       │   ├── PayOrderDivisionRecord.java
│   │   │   │                       │   ├── PayWay.java
│   │   │   │                       │   ├── RefundOrder.java
│   │   │   │                       │   ├── SysConfig.java
│   │   │   │                       │   ├── SysEntitlement.java
│   │   │   │                       │   ├── SysLog.java
│   │   │   │                       │   ├── SysRole.java
│   │   │   │                       │   ├── SysRoleEntRela.java
│   │   │   │                       │   ├── SysUser.java
│   │   │   │                       │   ├── SysUserAuth.java
│   │   │   │                       │   ├── SysUserRoleRela.java
│   │   │   │                       │   └── TransferOrder.java
│   │   │   │                       ├── exception
│   │   │   │                       │   ├── BizException.java
│   │   │   │                       │   ├── BizExceptionResolver.java
│   │   │   │                       │   ├── JeepayAuthenticationException.java
│   │   │   │                       │   └── ResponseException.java
│   │   │   │                       ├── jwt
│   │   │   │                       │   ├── JWTPayload.java
│   │   │   │                       │   └── JWTUtils.java
│   │   │   │                       ├── model
│   │   │   │                       │   ├── params
│   │   │   │                       │   │   ├── alipay
│   │   │   │                       │   │   │   ├── AlipayConfig.java
│   │   │   │                       │   │   │   ├── AlipayIsvParams.java
│   │   │   │                       │   │   │   ├── AlipayIsvsubMchParams.java
│   │   │   │                       │   │   │   └── AlipayNormalMchParams.java
│   │   │   │                       │   │   ├── ccat
│   │   │   │                       │   │   │   └── CcatNormalMchParams.java
│   │   │   │                       │   │   ├── plspay
│   │   │   │                       │   │   │   ├── PlspayConfig.java
│   │   │   │                       │   │   │   └── PlspayNormalMchParams.java
│   │   │   │                       │   │   ├── pppay
│   │   │   │                       │   │   │   └── PppayNormalMchParams.java
│   │   │   │                       │   │   ├── wxpay
│   │   │   │                       │   │   │   ├── WxpayIsvParams.java
│   │   │   │                       │   │   │   ├── WxpayIsvsubMchParams.java
│   │   │   │                       │   │   │   └── WxpayNormalMchParams.java
│   │   │   │                       │   │   ├── xxpay
│   │   │   │                       │   │   │   └── XxpayNormalMchParams.java
│   │   │   │                       │   │   ├── ysf
│   │   │   │                       │   │   │   ├── YsfpayConfig.java
│   │   │   │                       │   │   │   ├── YsfpayIsvParams.java
│   │   │   │                       │   │   │   └── YsfpayIsvsubMchParams.java
│   │   │   │                       │   │   ├── IsvParams.java
│   │   │   │                       │   │   ├── IsvsubMchParams.java
│   │   │   │                       │   │   └── NormalMchParams.java
│   │   │   │                       │   ├── security
│   │   │   │                       │   │   └── JeeUserDetails.java
│   │   │   │                       │   ├── ApiPageRes.java
│   │   │   │                       │   ├── ApiRes.java
│   │   │   │                       │   ├── BaseModel.java
│   │   │   │                       │   ├── DBApplicationConfig.java
│   │   │   │                       │   ├── OriginalRes.java
│   │   │   │                       │   └── QRCodeParams.java
│   │   │   │                       ├── service
│   │   │   │                       │   ├── ICodeSysTypeManager.java
│   │   │   │                       │   ├── IMchQrcodeManager.java
│   │   │   │                       │   └── ISysConfigService.java
│   │   │   │                       └── utils
│   │   │   │                           ├── AmountUtil.java
│   │   │   │                           ├── ApiResBodyAdviceKit.java
│   │   │   │                           ├── DateKit.java
│   │   │   │                           ├── FileKit.java
│   │   │   │                           ├── JeepayKit.java
│   │   │   │                           ├── JsonKit.java
│   │   │   │                           ├── RegKit.java
│   │   │   │                           ├── SeqKit.java
│   │   │   │                           ├── SpringBeansUtil.java
│   │   │   │                           ├── StringKit.java
│   │   │   │                           └── TreeDataBuilder.java
│   │   │   └── test
│   │   │       ├── java
│   │   │       │   └── com
│   │   │       │       └── .gitkeep
│   │   │       └── resources
│   │   │           └── .gitkeep
│   │   └── pom.xml
│   ├── jeepay-manager
│   │   ├── bin
│   │   │   ├── .settings
│   │   │   │   ├── org.eclipse.core.resources.prefs
│   │   │   │   └── org.eclipse.m2e.core.prefs
│   │   │   ├── src
│   │   │   │   ├── main
│   │   │   │   │   ├── java
│   │   │   │   │   │   └── com
│   │   │   │   │   │       └── jeequan
│   │   │   │   │   │           └── jeepay
│   │   │   │   │   │               └── mgr
│   │   │   │   │   │                   ├── aop
│   │   │   │   │   │                   │   └── MethodLogAop.class
│   │   │   │   │   │                   ├── bootstrap
│   │   │   │   │   │                   │   ├── FastJsonHttpMessageConverterEx.class
│   │   │   │   │   │                   │   ├── InitRunner.class
│   │   │   │   │   │                   │   ├── JeepayMgrApplication.class
│   │   │   │   │   │                   │   └── SwaggerJsonSerializer.class
│   │   │   │   │   │                   ├── config
│   │   │   │   │   │                   │   ├── RedisConfig.class
│   │   │   │   │   │                   │   ├── SwaggerConfig.class
│   │   │   │   │   │                   │   └── SystemYmlConfig.class
│   │   │   │   │   │                   ├── ctrl
│   │   │   │   │   │                   │   ├── anon
│   │   │   │   │   │                   │   │   └── AuthController.class
│   │   │   │   │   │                   │   ├── common
│   │   │   │   │   │                   │   │   └── StaticController.class
│   │   │   │   │   │                   │   ├── config
│   │   │   │   │   │                   │   │   ├── MainChartController.class
│   │   │   │   │   │                   │   │   └── SysConfigController.class
│   │   │   │   │   │                   │   ├── isv
│   │   │   │   │   │                   │   │   ├── IsvInfoController.class
│   │   │   │   │   │                   │   │   └── IsvPayInterfaceConfigController.class
│   │   │   │   │   │                   │   ├── merchant
│   │   │   │   │   │                   │   │   ├── MchAppController.class
│   │   │   │   │   │                   │   │   ├── MchInfoController.class
│   │   │   │   │   │                   │   │   ├── MchPayInterfaceConfigController.class
│   │   │   │   │   │                   │   │   └── MchPayPassageConfigController.class
│   │   │   │   │   │                   │   ├── order
│   │   │   │   │   │                   │   │   ├── MchNotifyController.class
│   │   │   │   │   │                   │   │   ├── PayOrderController.class
│   │   │   │   │   │                   │   │   ├── RefundOrderController.class
│   │   │   │   │   │                   │   │   └── TransferOrderController.class
│   │   │   │   │   │                   │   ├── payconfig
│   │   │   │   │   │                   │   │   ├── PayInterfaceDefineController.class
│   │   │   │   │   │                   │   │   └── PayWayController.class
│   │   │   │   │   │                   │   ├── sysuser
│   │   │   │   │   │                   │   │   ├── SysEntController.class
│   │   │   │   │   │                   │   │   ├── SysLogController.class
│   │   │   │   │   │                   │   │   ├── SysRoleController.class
│   │   │   │   │   │                   │   │   ├── SysRoleEntRelaController.class
│   │   │   │   │   │                   │   │   ├── SysUserController.class
│   │   │   │   │   │                   │   │   └── SysUserRoleRelaController.class
│   │   │   │   │   │                   │   ├── CommonCtrl.class
│   │   │   │   │   │                   │   └── CurrentUserController.class
│   │   │   │   │   │                   ├── mq
│   │   │   │   │   │                   │   └── ResetAppConfigMQReceiver.class
│   │   │   │   │   │                   ├── secruity
│   │   │   │   │   │                   │   ├── JeeAuthenticationEntryPoint.class
│   │   │   │   │   │                   │   ├── JeeAuthenticationTokenFilter.class
│   │   │   │   │   │                   │   ├── JeeUserDetailsServiceImpl.class
│   │   │   │   │   │                   │   └── WebSecurityConfig.class
│   │   │   │   │   │                   ├── service
│   │   │   │   │   │                   │   ├── AuthService.class
│   │   │   │   │   │                   │   └── CodeSysTypeManager.class
│   │   │   │   │   │                   └── web
│   │   │   │   │   │                       ├── ApiResBodyAdvice.class
│   │   │   │   │   │                       ├── ApiResInterceptor.class
│   │   │   │   │   │                       ├── ApplicationContextKit.class
│   │   │   │   │   │                       └── WebmvcConfig.class
│   │   │   │   │   └── resources
│   │   │   │   │       ├── static
│   │   │   │   │       │   └── index.html
│   │   │   │   │       ├── application.yml
│   │   │   │   │       ├── banner.txt
│   │   │   │   │       └── logback-spring.xml
│   │   │   │   └── test
│   │   │   │       ├── java
│   │   │   │       │   └── .gitkeep
│   │   │   │       └── resources
│   │   │   │           └── .gitkeep
│   │   │   ├── .project
│   │   │   ├── Dockerfile
│   │   │   └── pom.xml
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java
│   │   │   │   │   └── com
│   │   │   │   │       └── jeequan
│   │   │   │   │           └── jeepay
│   │   │   │   │               └── mgr
│   │   │   │   │                   ├── aop
│   │   │   │   │                   │   └── MethodLogAop.java
│   │   │   │   │                   ├── bootstrap
│   │   │   │   │                   │   ├── FastJsonHttpMessageConverterEx.java
│   │   │   │   │                   │   ├── InitRunner.java
│   │   │   │   │                   │   ├── JeepayMgrApplication.java
│   │   │   │   │                   │   └── SwaggerJsonSerializer.java
│   │   │   │   │                   ├── config
│   │   │   │   │                   │   ├── RedisConfig.java
│   │   │   │   │                   │   ├── SwaggerConfig.java
│   │   │   │   │                   │   └── SystemYmlConfig.java
│   │   │   │   │                   ├── ctrl
│   │   │   │   │                   │   ├── anon
│   │   │   │   │                   │   │   └── AuthController.java
│   │   │   │   │                   │   ├── common
│   │   │   │   │                   │   │   └── StaticController.java
│   │   │   │   │                   │   ├── config
│   │   │   │   │                   │   │   ├── MainChartController.java
│   │   │   │   │                   │   │   └── SysConfigController.java
│   │   │   │   │                   │   ├── isv
│   │   │   │   │                   │   │   ├── IsvInfoController.java
│   │   │   │   │                   │   │   └── IsvPayInterfaceConfigController.java
│   │   │   │   │                   │   ├── merchant
│   │   │   │   │                   │   │   ├── MchAppController.java
│   │   │   │   │                   │   │   ├── MchInfoController.java
│   │   │   │   │                   │   │   ├── MchPayInterfaceConfigController.java
│   │   │   │   │                   │   │   └── MchPayPassageConfigController.java
│   │   │   │   │                   │   ├── order
│   │   │   │   │                   │   │   ├── MchNotifyController.java
│   │   │   │   │                   │   │   ├── PayOrderController.java
│   │   │   │   │                   │   │   ├── RefundOrderController.java
│   │   │   │   │                   │   │   └── TransferOrderController.java
│   │   │   │   │                   │   ├── payconfig
│   │   │   │   │                   │   │   ├── PayInterfaceDefineController.java
│   │   │   │   │                   │   │   └── PayWayController.java
│   │   │   │   │                   │   ├── sysuser
│   │   │   │   │                   │   │   ├── SysEntController.java
│   │   │   │   │                   │   │   ├── SysLogController.java
│   │   │   │   │                   │   │   ├── SysRoleController.java
│   │   │   │   │                   │   │   ├── SysRoleEntRelaController.java
│   │   │   │   │                   │   │   ├── SysUserController.java
│   │   │   │   │                   │   │   └── SysUserRoleRelaController.java
│   │   │   │   │                   │   ├── CommonCtrl.java
│   │   │   │   │                   │   └── CurrentUserController.java
│   │   │   │   │                   ├── mq
│   │   │   │   │                   │   └── ResetAppConfigMQReceiver.java
│   │   │   │   │                   ├── secruity
│   │   │   │   │                   │   ├── JeeAuthenticationEntryPoint.java
│   │   │   │   │                   │   ├── JeeAuthenticationTokenFilter.java
│   │   │   │   │                   │   ├── JeeUserDetailsServiceImpl.java
│   │   │   │   │                   │   └── WebSecurityConfig.java
│   │   │   │   │                   ├── service
│   │   │   │   │                   │   ├── AuthService.java
│   │   │   │   │                   │   └── CodeSysTypeManager.java
│   │   │   │   │                   └── web
│   │   │   │   │                       ├── ApiResBodyAdvice.java
│   │   │   │   │                       ├── ApiResInterceptor.java
│   │   │   │   │                       ├── ApplicationContextKit.java
│   │   │   │   │                       └── WebmvcConfig.java
│   │   │   │   └── resources
│   │   │   │       ├── static
│   │   │   │       │   └── index.html
│   │   │   │       ├── application.yml
│   │   │   │       ├── banner.txt
│   │   │   │       └── logback-spring.xml
│   │   │   └── test
│   │   │       ├── java
│   │   │       │   ├── com
│   │   │       │   │   └── jeequan
│   │   │       │   │       └── jeepay
│   │   │       │   │           └── mgr
│   │   │       │   │               └── ctrl
│   │   │       │   │                   └── order
│   │   │       │   │                       └── MchNotifyControllerSendTest.java
│   │   │       │   └── .gitkeep
│   │   │       └── resources
│   │   │           └── .gitkeep
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── jeepay-merchant
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java
│   │   │   │   │   └── com
│   │   │   │   │       └── jeequan
│   │   │   │   │           └── jeepay
│   │   │   │   │               └── mch
│   │   │   │   │                   ├── aop
│   │   │   │   │                   │   └── MethodLogAop.java
│   │   │   │   │                   ├── bootstrap
│   │   │   │   │                   │   ├── FastJsonHttpMessageConverterEx.java
│   │   │   │   │                   │   ├── InitRunner.java
│   │   │   │   │                   │   ├── JeepayMchApplication.java
│   │   │   │   │                   │   └── SwaggerJsonSerializer.java
│   │   │   │   │                   ├── config
│   │   │   │   │                   │   ├── RedisConfig.java
│   │   │   │   │                   │   ├── SwaggerConfig.java
│   │   │   │   │                   │   └── SystemYmlConfig.java
│   │   │   │   │                   ├── ctrl
│   │   │   │   │                   │   ├── anon
│   │   │   │   │                   │   │   └── AuthController.java
│   │   │   │   │                   │   ├── division
│   │   │   │   │                   │   │   ├── MchDivisionReceiverController.java
│   │   │   │   │                   │   │   ├── MchDivisionReceiverGroupController.java
│   │   │   │   │                   │   │   └── PayOrderDivisionRecordController.java
│   │   │   │   │                   │   ├── merchant
│   │   │   │   │                   │   │   ├── MainChartController.java
│   │   │   │   │                   │   │   ├── MchAppController.java
│   │   │   │   │                   │   │   ├── MchPayInterfaceConfigController.java
│   │   │   │   │                   │   │   └── MchPayPassageConfigController.java
│   │   │   │   │                   │   ├── order
│   │   │   │   │                   │   │   ├── PayOrderController.java
│   │   │   │   │                   │   │   ├── RefundOrderController.java
│   │   │   │   │                   │   │   └── TransferOrderController.java
│   │   │   │   │                   │   ├── payconfig
│   │   │   │   │                   │   │   └── PayWayController.java
│   │   │   │   │                   │   ├── paytest
│   │   │   │   │                   │   │   ├── PaytestController.java
│   │   │   │   │                   │   │   └── PaytestNotifyController.java
│   │   │   │   │                   │   ├── sysuser
│   │   │   │   │                   │   │   ├── SysEntController.java
│   │   │   │   │                   │   │   ├── SysRoleController.java
│   │   │   │   │                   │   │   ├── SysRoleEntRelaController.java
│   │   │   │   │                   │   │   ├── SysUserController.java
│   │   │   │   │                   │   │   └── SysUserRoleRelaController.java
│   │   │   │   │                   │   ├── transfer
│   │   │   │   │                   │   │   ├── ChannelUserIdNotifyController.java
│   │   │   │   │                   │   │   ├── MchTransferController.java
│   │   │   │   │                   │   │   └── TransferNotifyController.java
│   │   │   │   │                   │   ├── CommonCtrl.java
│   │   │   │   │                   │   └── CurrentUserController.java
│   │   │   │   │                   ├── mq
│   │   │   │   │                   │   ├── CleanMchLoginAuthCacheMQReceiver.java
│   │   │   │   │                   │   └── ResetAppConfigMQReceiver.java
│   │   │   │   │                   ├── secruity
│   │   │   │   │                   │   ├── JeeAuthenticationEntryPoint.java
│   │   │   │   │                   │   ├── JeeAuthenticationTokenFilter.java
│   │   │   │   │                   │   ├── JeeUserDetailsServiceImpl.java
│   │   │   │   │                   │   └── WebSecurityConfig.java
│   │   │   │   │                   ├── service
│   │   │   │   │                   │   ├── AuthService.java
│   │   │   │   │                   │   └── CodeSysTypeManager.java
│   │   │   │   │                   ├── web
│   │   │   │   │                   │   ├── ApiResBodyAdvice.java
│   │   │   │   │                   │   ├── ApiResInterceptor.java
│   │   │   │   │                   │   ├── ApplicationContextKit.java
│   │   │   │   │                   │   └── WebmvcConfig.java
│   │   │   │   │                   └── websocket
│   │   │   │   │                       ├── config
│   │   │   │   │                       │   └── WebSocketConfig.java
│   │   │   │   │                       └── server
│   │   │   │   │                           ├── WsChannelUserIdServer.java
│   │   │   │   │                           ├── WsPayOrderServer.java
│   │   │   │   │                           └── WsTransferOrderServer.java
│   │   │   │   └── resources
│   │   │   │       ├── static
│   │   │   │       │   └── index.html
│   │   │   │       ├── templates
│   │   │   │       │   └── channelUser
│   │   │   │       │       └── getChannelUserIdPage.ftl
│   │   │   │       ├── application.yml
│   │   │   │       ├── banner.txt
│   │   │   │       └── logback-spring.xml
│   │   │   └── test
│   │   │       ├── java
│   │   │       │   └── .gitkeep
│   │   │       └── resources
│   │   │           └── .gitkeep
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── jeepay-payment
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java
│   │   │   │   │   └── com
│   │   │   │   │       └── jeequan
│   │   │   │   │           └── jeepay
│   │   │   │   │               └── pay
│   │   │   │   │                   ├── bootstrap
│   │   │   │   │                   │   ├── FastJsonHttpMessageConverterEx.java
│   │   │   │   │                   │   ├── InitRunner.java
│   │   │   │   │                   │   ├── JeepayPayApplication.java
│   │   │   │   │                   │   └── SwaggerJsonSerializer.java
│   │   │   │   │                   ├── channel
│   │   │   │   │                   │   ├── alipay
│   │   │   │   │                   │   │   ├── ctrl
│   │   │   │   │                   │   │   │   └── AlipayBizController.java
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── AliApp.java
│   │   │   │   │                   │   │   │   ├── AliBar.java
│   │   │   │   │                   │   │   │   ├── AliJsapi.java
│   │   │   │   │                   │   │   │   ├── AliOc.java
│   │   │   │   │                   │   │   │   ├── AliPc.java
│   │   │   │   │                   │   │   │   ├── AliQr.java
│   │   │   │   │                   │   │   │   └── AliWap.java
│   │   │   │   │                   │   │   ├── AlipayChannelNoticeService.java
│   │   │   │   │                   │   │   ├── AlipayChannelUserService.java
│   │   │   │   │                   │   │   ├── AlipayDivisionRecordChannelNotifyService.java
│   │   │   │   │                   │   │   ├── AlipayDivisionService.java
│   │   │   │   │                   │   │   ├── AlipayKit.java
│   │   │   │   │                   │   │   ├── AlipayPayOrderCloseService.java
│   │   │   │   │                   │   │   ├── AlipayPayOrderQueryService.java
│   │   │   │   │                   │   │   ├── AlipayPaymentService.java
│   │   │   │   │                   │   │   ├── AlipayRefundService.java
│   │   │   │   │                   │   │   ├── AlipayTransferNoticeService.java
│   │   │   │   │                   │   │   └── AlipayTransferService.java
│   │   │   │   │                   │   ├── ccat
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   └── CcatIbon.java
│   │   │   │   │                   │   │   ├── CcatChannelNoticeService.java
│   │   │   │   │                   │   │   ├── CcatClient.java
│   │   │   │   │                   │   │   ├── CcatIbonOrderRS.java
│   │   │   │   │                   │   │   ├── CcatKit.java
│   │   │   │   │                   │   │   ├── CcatLogSanitizer.java
│   │   │   │   │                   │   │   ├── CcatMchParamsResolver.java
│   │   │   │   │                   │   │   ├── CcatPayOrderQueryService.java
│   │   │   │   │                   │   │   └── CcatPaymentService.java
│   │   │   │   │                   │   ├── plspay
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── AliApp.java
│   │   │   │   │                   │   │   │   ├── AliBar.java
│   │   │   │   │                   │   │   │   ├── AliJsapi.java
│   │   │   │   │                   │   │   │   ├── AliLite.java
│   │   │   │   │                   │   │   │   ├── AliPc.java
│   │   │   │   │                   │   │   │   ├── AliQr.java
│   │   │   │   │                   │   │   │   ├── AliWap.java
│   │   │   │   │                   │   │   │   ├── WxApp.java
│   │   │   │   │                   │   │   │   ├── WxBar.java
│   │   │   │   │                   │   │   │   ├── WxH5.java
│   │   │   │   │                   │   │   │   ├── WxJsapi.java
│   │   │   │   │                   │   │   │   ├── WxLite.java
│   │   │   │   │                   │   │   │   └── WxNative.java
│   │   │   │   │                   │   │   ├── PlspayChannelNoticeService.java
│   │   │   │   │                   │   │   ├── PlspayChannelRefundNoticeService.java
│   │   │   │   │                   │   │   ├── PlspayKit.java
│   │   │   │   │                   │   │   ├── PlspayPayOrderQueryService.java
│   │   │   │   │                   │   │   ├── PlspayPaymentService.java
│   │   │   │   │                   │   │   └── PlspayRefundService.java
│   │   │   │   │                   │   ├── pppay
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   └── PpPc.java
│   │   │   │   │                   │   │   ├── PppayChannelNoticeService.java
│   │   │   │   │                   │   │   ├── PppayChannelRefundNoticeService.java
│   │   │   │   │                   │   │   ├── PppayPayOrderQueryService.java
│   │   │   │   │                   │   │   ├── PppayPaymentService.java
│   │   │   │   │                   │   │   └── PppayRefundService.java
│   │   │   │   │                   │   ├── wxpay
│   │   │   │   │                   │   │   ├── ctrl
│   │   │   │   │                   │   │   │   └── wxpayBizController.java
│   │   │   │   │                   │   │   ├── kits
│   │   │   │   │                   │   │   │   ├── WxpayKit.java
│   │   │   │   │                   │   │   │   └── WxpayV3Util.java
│   │   │   │   │                   │   │   ├── model
│   │   │   │   │                   │   │   │   └── WxpayV3OrderRequestModel.java
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── WxApp.java
│   │   │   │   │                   │   │   │   ├── WxBar.java
│   │   │   │   │                   │   │   │   ├── WxH5.java
│   │   │   │   │                   │   │   │   ├── WxJsapi.java
│   │   │   │   │                   │   │   │   ├── WxLite.java
│   │   │   │   │                   │   │   │   └── WxNative.java
│   │   │   │   │                   │   │   ├── paywayV3
│   │   │   │   │                   │   │   │   ├── WxApp.java
│   │   │   │   │                   │   │   │   ├── WxBar.java
│   │   │   │   │                   │   │   │   ├── WxH5.java
│   │   │   │   │                   │   │   │   ├── WxJsapi.java
│   │   │   │   │                   │   │   │   ├── WxLite.java
│   │   │   │   │                   │   │   │   └── WxNative.java
│   │   │   │   │                   │   │   ├── WxpayChannelNoticeService.java
│   │   │   │   │                   │   │   ├── WxpayChannelRefundNoticeService.java
│   │   │   │   │                   │   │   ├── WxpayChannelUserService.java
│   │   │   │   │                   │   │   ├── WxpayDivisionService.java
│   │   │   │   │                   │   │   ├── WxpayPayOrderCloseService.java
│   │   │   │   │                   │   │   ├── WxpayPayOrderQueryService.java
│   │   │   │   │                   │   │   ├── WxpayPaymentService.java
│   │   │   │   │                   │   │   ├── WxpayRefundService.java
│   │   │   │   │                   │   │   ├── WxpayTransferNoticeService.java
│   │   │   │   │                   │   │   └── WxpayTransferService.java
│   │   │   │   │                   │   ├── xxpay
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── AliBar.java
│   │   │   │   │                   │   │   │   ├── AliJsapi.java
│   │   │   │   │                   │   │   │   ├── WxBar.java
│   │   │   │   │                   │   │   │   └── WxJsapi.java
│   │   │   │   │                   │   │   ├── XxpayChannelNoticeService.java
│   │   │   │   │                   │   │   ├── XxpayChannelRefundNoticeService.java
│   │   │   │   │                   │   │   ├── XxpayKit.java
│   │   │   │   │                   │   │   ├── XxpayPayOrderQueryService.java
│   │   │   │   │                   │   │   ├── XxpayPaymentService.java
│   │   │   │   │                   │   │   └── XxpayRefundService.java
│   │   │   │   │                   │   ├── ysfpay
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── AliBar.java
│   │   │   │   │                   │   │   │   ├── AliJsapi.java
│   │   │   │   │                   │   │   │   ├── WxBar.java
│   │   │   │   │                   │   │   │   ├── WxJsapi.java
│   │   │   │   │                   │   │   │   ├── YsfBar.java
│   │   │   │   │                   │   │   │   └── YsfJsapi.java
│   │   │   │   │                   │   │   ├── utils
│   │   │   │   │                   │   │   │   ├── YsfHttpUtil.java
│   │   │   │   │                   │   │   │   └── YsfSignUtils.java
│   │   │   │   │                   │   │   ├── YsfpayChannelNoticeService.java
│   │   │   │   │                   │   │   ├── YsfpayPayOrderCloseService.java
│   │   │   │   │                   │   │   ├── YsfpayPayOrderQueryService.java
│   │   │   │   │                   │   │   ├── YsfpayPaymentService.java
│   │   │   │   │                   │   │   └── YsfpayRefundService.java
│   │   │   │   │                   │   ├── AbstractChannelNoticeService.java
│   │   │   │   │                   │   ├── AbstractChannelRefundNoticeService.java
│   │   │   │   │                   │   ├── AbstractDivisionRecordChannelNotifyService.java
│   │   │   │   │                   │   ├── AbstractPaymentService.java
│   │   │   │   │                   │   ├── AbstractRefundService.java
│   │   │   │   │                   │   ├── AbstractTransferNoticeService.java
│   │   │   │   │                   │   ├── IChannelNoticeService.java
│   │   │   │   │                   │   ├── IChannelRefundNoticeService.java
│   │   │   │   │                   │   ├── IChannelUserService.java
│   │   │   │   │                   │   ├── IDivisionService.java
│   │   │   │   │                   │   ├── IPayOrderCloseService.java
│   │   │   │   │                   │   ├── IPayOrderQueryService.java
│   │   │   │   │                   │   ├── IPaymentService.java
│   │   │   │   │                   │   ├── IRefundService.java
│   │   │   │   │                   │   ├── ITransferNoticeService.java
│   │   │   │   │                   │   └── ITransferService.java
│   │   │   │   │                   ├── config
│   │   │   │   │                   │   ├── RedisConfig.java
│   │   │   │   │                   │   ├── SwaggerConfig.java
│   │   │   │   │                   │   └── SystemYmlConfig.java
│   │   │   │   │                   ├── ctrl
│   │   │   │   │                   │   ├── division
│   │   │   │   │                   │   │   ├── DivisionRecordChannelNotifyController.java
│   │   │   │   │                   │   │   ├── MchDivisionReceiverBindController.java
│   │   │   │   │                   │   │   └── PayOrderDivisionExecController.java
│   │   │   │   │                   │   ├── payorder
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── AliBarOrderController.java
│   │   │   │   │                   │   │   │   ├── AliJsapiOrderController.java
│   │   │   │   │                   │   │   │   ├── YsfBarOrderController.java
│   │   │   │   │                   │   │   │   └── YsfJsapiOrderController.java
│   │   │   │   │                   │   │   ├── AbstractPayOrderController.java
│   │   │   │   │                   │   │   ├── ChannelNoticeController.java
│   │   │   │   │                   │   │   ├── CloseOrderController.java
│   │   │   │   │                   │   │   ├── QueryOrderController.java
│   │   │   │   │                   │   │   └── UnifiedOrderController.java
│   │   │   │   │                   │   ├── qr
│   │   │   │   │                   │   │   ├── ChannelUserIdController.java
│   │   │   │   │                   │   │   └── QrCashierController.java
│   │   │   │   │                   │   ├── refund
│   │   │   │   │                   │   │   ├── ChannelRefundNoticeController.java
│   │   │   │   │                   │   │   ├── QueryRefundOrderController.java
│   │   │   │   │                   │   │   └── RefundOrderController.java
│   │   │   │   │                   │   ├── scanimg
│   │   │   │   │                   │   │   └── ScanImgController.java
│   │   │   │   │                   │   ├── transfer
│   │   │   │   │                   │   │   ├── QueryTransferOrderController.java
│   │   │   │   │                   │   │   ├── TransferNoticeController.java
│   │   │   │   │                   │   │   └── TransferOrderController.java
│   │   │   │   │                   │   ├── ApiController.java
│   │   │   │   │                   │   └── CommonController.java
│   │   │   │   │                   ├── exception
│   │   │   │   │                   │   └── ChannelException.java
│   │   │   │   │                   ├── model
│   │   │   │   │                   │   ├── AlipayClientWrapper.java
│   │   │   │   │                   │   ├── IsvConfigContext.java
│   │   │   │   │                   │   ├── MchAppConfigContext.java
│   │   │   │   │                   │   ├── MchInfoConfigContext.java
│   │   │   │   │                   │   ├── PaypalWrapper.java
│   │   │   │   │                   │   └── WxServiceWrapper.java
│   │   │   │   │                   ├── mq
│   │   │   │   │                   │   ├── PayOrderDivisionMQReceiver.java
│   │   │   │   │                   │   ├── PayOrderMchNotifyMQReceiver.java
│   │   │   │   │                   │   ├── PayOrderReissueMQReceiver.java
│   │   │   │   │                   │   ├── ResetAppConfigMQReceiver.java
│   │   │   │   │                   │   └── ResetIsvMchAppInfoMQReceiver.java
│   │   │   │   │                   ├── rqrs
│   │   │   │   │                   │   ├── division
│   │   │   │   │                   │   │   ├── DivisionReceiverBindRQ.java
│   │   │   │   │                   │   │   ├── DivisionReceiverBindRS.java
│   │   │   │   │                   │   │   ├── PayOrderDivisionExecRQ.java
│   │   │   │   │                   │   │   └── PayOrderDivisionExecRS.java
│   │   │   │   │                   │   ├── msg
│   │   │   │   │                   │   │   ├── ChannelRetMsg.java
│   │   │   │   │                   │   │   └── DivisionChannelNotifyModel.java
│   │   │   │   │                   │   ├── payorder
│   │   │   │   │                   │   │   ├── payway
│   │   │   │   │                   │   │   │   ├── AliAppOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliAppOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliBarOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliBarOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliJsapiOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliJsapiOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliLiteOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliLiteOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliOcOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliOcOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliPcOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliPcOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliQrOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliQrOrderRS.java
│   │   │   │   │                   │   │   │   ├── AliWapOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AliWapOrderRS.java
│   │   │   │   │                   │   │   │   ├── AutoBarOrderRQ.java
│   │   │   │   │                   │   │   │   ├── AutoBarOrderRS.java
│   │   │   │   │                   │   │   │   ├── PPPcOrderRQ.java
│   │   │   │   │                   │   │   │   ├── PPPcOrderRS.java
│   │   │   │   │                   │   │   │   ├── QrCashierOrderRQ.java
│   │   │   │   │                   │   │   │   ├── QrCashierOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpAppOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpAppOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpB2bOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpB2bOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpBarOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpBarOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpJsapiOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpJsapiOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpPcOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpPcOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpQrOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpQrOrderRS.java
│   │   │   │   │                   │   │   │   ├── UpWapOrderRQ.java
│   │   │   │   │                   │   │   │   ├── UpWapOrderRS.java
│   │   │   │   │                   │   │   │   ├── WxAppOrderRQ.java
│   │   │   │   │                   │   │   │   ├── WxAppOrderRS.java
│   │   │   │   │                   │   │   │   ├── WxBarOrderRQ.java
│   │   │   │   │                   │   │   │   ├── WxBarOrderRS.java
│   │   │   │   │                   │   │   │   ├── WxH5OrderRQ.java
│   │   │   │   │                   │   │   │   ├── WxH5OrderRS.java
│   │   │   │   │                   │   │   │   ├── WxJsapiOrderRQ.java
│   │   │   │   │                   │   │   │   ├── WxJsapiOrderRS.java
│   │   │   │   │                   │   │   │   ├── WxLiteOrderRQ.java
│   │   │   │   │                   │   │   │   ├── WxLiteOrderRS.java
│   │   │   │   │                   │   │   │   ├── WxNativeOrderRQ.java
│   │   │   │   │                   │   │   │   ├── WxNativeOrderRS.java
│   │   │   │   │                   │   │   │   ├── YsfBarOrderRQ.java
│   │   │   │   │                   │   │   │   ├── YsfBarOrderRS.java
│   │   │   │   │                   │   │   │   ├── YsfJsapiOrderRQ.java
│   │   │   │   │                   │   │   │   └── YsfJsapiOrderRS.java
│   │   │   │   │                   │   │   ├── ClosePayOrderRQ.java
│   │   │   │   │                   │   │   ├── ClosePayOrderRS.java
│   │   │   │   │                   │   │   ├── CommonPayDataRQ.java
│   │   │   │   │                   │   │   ├── CommonPayDataRS.java
│   │   │   │   │                   │   │   ├── QueryPayOrderRQ.java
│   │   │   │   │                   │   │   ├── QueryPayOrderRS.java
│   │   │   │   │                   │   │   ├── UnifiedOrderRQ.java
│   │   │   │   │                   │   │   └── UnifiedOrderRS.java
│   │   │   │   │                   │   ├── refund
│   │   │   │   │                   │   │   ├── QueryRefundOrderRQ.java
│   │   │   │   │                   │   │   ├── QueryRefundOrderRS.java
│   │   │   │   │                   │   │   ├── RefundOrderRQ.java
│   │   │   │   │                   │   │   └── RefundOrderRS.java
│   │   │   │   │                   │   ├── transfer
│   │   │   │   │                   │   │   ├── QueryTransferOrderRQ.java
│   │   │   │   │                   │   │   ├── QueryTransferOrderRS.java
│   │   │   │   │                   │   │   ├── TransferOrderRQ.java
│   │   │   │   │                   │   │   └── TransferOrderRS.java
│   │   │   │   │                   │   ├── AbstractMchAppRQ.java
│   │   │   │   │                   │   ├── AbstractRQ.java
│   │   │   │   │                   │   ├── AbstractRS.java
│   │   │   │   │                   │   └── ChannelUserIdRQ.java
│   │   │   │   │                   ├── service
│   │   │   │   │                   │   ├── ChannelOrderReissueService.java
│   │   │   │   │                   │   ├── CodeSysTypeManager.java
│   │   │   │   │                   │   ├── ConfigContextQueryService.java
│   │   │   │   │                   │   ├── ConfigContextService.java
│   │   │   │   │                   │   ├── PayMchNotifyService.java
│   │   │   │   │                   │   ├── PayOrderDivisionProcessService.java
│   │   │   │   │                   │   ├── PayOrderProcessService.java
│   │   │   │   │                   │   ├── RefundOrderProcessService.java
│   │   │   │   │                   │   ├── TransferOrderReissueService.java
│   │   │   │   │                   │   └── ValidateService.java
│   │   │   │   │                   ├── task
│   │   │   │   │                   │   ├── PayOrderDivisionRecordReissueTask.java
│   │   │   │   │                   │   ├── PayOrderExpiredTask.java
│   │   │   │   │                   │   ├── PayOrderReissueTask.java
│   │   │   │   │                   │   ├── RefundOrderExpiredTask.java
│   │   │   │   │                   │   ├── RefundOrderReissueTask.java
│   │   │   │   │                   │   └── TransferOrderReissueTask.java
│   │   │   │   │                   └── util
│   │   │   │   │                       ├── ApiResBuilder.java
│   │   │   │   │                       ├── ChannelCertConfigKitBean.java
│   │   │   │   │                       ├── CodeImgUtil.java
│   │   │   │   │                       └── PaywayUtil.java
│   │   │   │   └── resources
│   │   │   │       ├── markdown
│   │   │   │       │   └── doc
│   │   │   │       │       ├── api1.md
│   │   │   │       │       ├── api2.md
│   │   │   │       │       ├── api3.md
│   │   │   │       │       ├── api4.md
│   │   │   │       │       └── api5.md
│   │   │   │       ├── static
│   │   │   │       │   └── cashier
│   │   │   │       │       ├── css
│   │   │   │       │       │   ├── app.b0d6b471.css
│   │   │   │       │       │   ├── chunk-0a8a1f2c.8266418e.css
│   │   │   │       │       │   ├── chunk-206ff9e2.841b0312.css
│   │   │   │       │       │   ├── chunk-65dd90fc.841b0312.css
│   │   │   │       │       │   ├── chunk-74b110af.841b0312.css
│   │   │   │       │       │   └── chunk-93b06108.d5ba12fa.css
│   │   │   │       │       ├── fonts
│   │   │   │       │       │   └── WeChatSansSS-Bold.245dd277.ttf
│   │   │   │       │       ├── img
│   │   │   │       │       │   ├── S.1db749bc.svg
│   │   │   │       │       │   ├── error.5bc34a8a.svg
│   │   │   │       │       │   ├── wx.ec067f2f.svg
│   │   │   │       │       │   ├── ysf.dbdf047a.jpg
│   │   │   │       │       │   └── zfb.f9f04ed3.jpeg
│   │   │   │       │       ├── js
│   │   │   │       │       │   ├── app.cebd468e.js
│   │   │   │       │       │   ├── app.cebd468e.js.map
│   │   │   │       │       │   ├── chunk-0a8a1f2c.9bd81b70.js
│   │   │   │       │       │   ├── chunk-0a8a1f2c.9bd81b70.js.map
│   │   │   │       │       │   ├── chunk-206ff9e2.c0881330.js
│   │   │   │       │       │   ├── chunk-206ff9e2.c0881330.js.map
│   │   │   │       │       │   ├── chunk-235ac4ce.e3ca4e4c.js
│   │   │   │       │       │   ├── chunk-235ac4ce.e3ca4e4c.js.map
│   │   │   │       │       │   ├── chunk-2d20f936.1ce40f42.js
│   │   │   │       │       │   ├── chunk-2d20f936.1ce40f42.js.map
│   │   │   │       │       │   ├── chunk-2d22c085.ef49138c.js
│   │   │   │       │       │   ├── chunk-2d22c085.ef49138c.js.map
│   │   │   │       │       │   ├── chunk-65dd90fc.b4aa0c91.js
│   │   │   │       │       │   ├── chunk-65dd90fc.b4aa0c91.js.map
│   │   │   │       │       │   ├── chunk-74b110af.a4fee9cb.js
│   │   │   │       │       │   ├── chunk-74b110af.a4fee9cb.js.map
│   │   │   │       │       │   ├── chunk-93b06108.56757d92.js
│   │   │   │       │       │   ├── chunk-93b06108.56757d92.js.map
│   │   │   │       │       │   ├── chunk-vendors.2e0d2416.js
│   │   │   │       │       │   └── chunk-vendors.2e0d2416.js.map
│   │   │   │       │       ├── favicon.ico
│   │   │   │       │       ├── index.html
│   │   │   │       │       └── readme.txt
│   │   │   │       ├── templates
│   │   │   │       │   ├── cashier
│   │   │   │       │   │   └── returnPage.ftl
│   │   │   │       │   ├── channel
│   │   │   │       │   │   ├── alipay
│   │   │   │       │   │   │   └── isvsubMchAuth.ftl
│   │   │   │       │   │   └── wxpay
│   │   │   │       │   │       └── wxTransferUserConfirm.ftl
│   │   │   │       │   └── common
│   │   │   │       │       └── toPay.ftl
│   │   │   │       ├── application.yml
│   │   │   │       ├── banner.txt
│   │   │   │       └── logback-spring.xml
│   │   │   └── test
│   │   │       ├── java
│   │   │       │   ├── com
│   │   │       │   │   └── jeequan
│   │   │       │   │       └── jeepay
│   │   │       │   │           └── pay
│   │   │       │   │               ├── channel
│   │   │       │   │               │   └── ccat
│   │   │       │   │               │       ├── payway
│   │   │       │   │               │       │   └── CcatIbonTest.java
│   │   │       │   │               │       ├── CcatArchitectureTest.java
│   │   │       │   │               │       ├── CcatChannelNoticeFlowTest.java
│   │   │       │   │               │       ├── CcatChannelNoticeServiceTest.java
│   │   │       │   │               │       ├── CcatClientTest.java
│   │   │       │   │               │       ├── CcatKitTest.java
│   │   │       │   │               │       ├── CcatLogSanitizerTest.java
│   │   │       │   │               │       ├── CcatMchParamsResolverTest.java
│   │   │       │   │               │       └── CcatPayOrderQueryServiceTest.java
│   │   │       │   │               └── contract
│   │   │       │   │                   └── MerchantUatContractTest.java
│   │   │       │   └── .gitkeep
│   │   │       └── resources
│   │   │           └── .gitkeep
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── jeepay-service
│   │   ├── src
│   │   │   ├── main
│   │   │   │   └── java
│   │   │   │       └── com
│   │   │   │           └── jeequan
│   │   │   │               └── jeepay
│   │   │   │                   └── service
│   │   │   │                       ├── impl
│   │   │   │                       │   ├── IsvInfoService.java
│   │   │   │                       │   ├── MchAppService.java
│   │   │   │                       │   ├── MchDivisionReceiverGroupService.java
│   │   │   │                       │   ├── MchDivisionReceiverService.java
│   │   │   │                       │   ├── MchInfoService.java
│   │   │   │                       │   ├── MchNotifyRecordService.java
│   │   │   │                       │   ├── MchPayPassageService.java
│   │   │   │                       │   ├── PayInterfaceConfigService.java
│   │   │   │                       │   ├── PayInterfaceDefineService.java
│   │   │   │                       │   ├── PayOrderDivisionRecordService.java
│   │   │   │                       │   ├── PayOrderService.java
│   │   │   │                       │   ├── PayWayService.java
│   │   │   │                       │   ├── RefundOrderService.java
│   │   │   │                       │   ├── SysConfigService.java
│   │   │   │                       │   ├── SysEntitlementService.java
│   │   │   │                       │   ├── SysLogService.java
│   │   │   │                       │   ├── SysRoleEntRelaService.java
│   │   │   │                       │   ├── SysRoleService.java
│   │   │   │                       │   ├── SysUserAuthService.java
│   │   │   │                       │   ├── SysUserRoleRelaService.java
│   │   │   │                       │   ├── SysUserService.java
│   │   │   │                       │   └── TransferOrderService.java
│   │   │   │                       └── mapper
│   │   │   │                           ├── IsvInfoMapper.java
│   │   │   │                           ├── IsvInfoMapper.xml
│   │   │   │                           ├── MchAppMapper.java
│   │   │   │                           ├── MchAppMapper.xml
│   │   │   │                           ├── MchDivisionReceiverGroupMapper.java
│   │   │   │                           ├── MchDivisionReceiverGroupMapper.xml
│   │   │   │                           ├── MchDivisionReceiverMapper.java
│   │   │   │                           ├── MchDivisionReceiverMapper.xml
│   │   │   │                           ├── MchInfoMapper.java
│   │   │   │                           ├── MchInfoMapper.xml
│   │   │   │                           ├── MchNotifyRecordMapper.java
│   │   │   │                           ├── MchNotifyRecordMapper.xml
│   │   │   │                           ├── MchPayPassageMapper.java
│   │   │   │                           ├── MchPayPassageMapper.xml
│   │   │   │                           ├── PayInterfaceConfigMapper.java
│   │   │   │                           ├── PayInterfaceConfigMapper.xml
│   │   │   │                           ├── PayInterfaceDefineMapper.java
│   │   │   │                           ├── PayInterfaceDefineMapper.xml
│   │   │   │                           ├── PayOrderDivisionRecordMapper.java
│   │   │   │                           ├── PayOrderDivisionRecordMapper.xml
│   │   │   │                           ├── PayOrderMapper.java
│   │   │   │                           ├── PayOrderMapper.xml
│   │   │   │                           ├── PayWayMapper.java
│   │   │   │                           ├── PayWayMapper.xml
│   │   │   │                           ├── RefundOrderMapper.java
│   │   │   │                           ├── RefundOrderMapper.xml
│   │   │   │                           ├── SysConfigMapper.java
│   │   │   │                           ├── SysConfigMapper.xml
│   │   │   │                           ├── SysEntitlementMapper.java
│   │   │   │                           ├── SysEntitlementMapper.xml
│   │   │   │                           ├── SysLogMapper.java
│   │   │   │                           ├── SysLogMapper.xml
│   │   │   │                           ├── SysRoleEntRelaMapper.java
│   │   │   │                           ├── SysRoleEntRelaMapper.xml
│   │   │   │                           ├── SysRoleMapper.java
│   │   │   │                           ├── SysRoleMapper.xml
│   │   │   │                           ├── SysUserAuthMapper.java
│   │   │   │                           ├── SysUserAuthMapper.xml
│   │   │   │                           ├── SysUserMapper.java
│   │   │   │                           ├── SysUserMapper.xml
│   │   │   │                           ├── SysUserRoleRelaMapper.java
│   │   │   │                           ├── SysUserRoleRelaMapper.xml
│   │   │   │                           ├── TransferOrderMapper.java
│   │   │   │                           └── TransferOrderMapper.xml
│   │   │   └── test
│   │   │       ├── java
│   │   │       │   └── com
│   │   │       │       └── .gitkeep
│   │   │       └── resources
│   │   │           └── .gitkeep
│   │   └── pom.xml
│   ├── jeepay-z-codegen
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java
│   │   │   │   │   └── com
│   │   │   │   │       └── gen
│   │   │   │   │           └── MainGen.java
│   │   │   │   └── resources
│   │   │   │       └── .gitkeep
│   │   │   └── test
│   │   │       ├── java
│   │   │       │   └── .gitkeep
│   │   │       └── resources
│   │   │           └── .gitkeep
│   │   └── pom.xml
│   ├── libs
│   │   └── jeepay-sdk-java-pls-1.2.0.jar
│   ├── logs
│   │   ├── manager
│   │   │   ├── mgr.all.2026-08-12.log
│   │   │   ├── mgr.all.log
│   │   │   └── mgr.error.log
│   │   ├── merchant
│   │   │   ├── mch.all.2026-08-12.log
│   │   │   ├── mch.all.log
│   │   │   └── mch.error.log
│   │   └── payment
│   │       ├── pay.all.2026-08-12.log
│   │       ├── pay.all.log
│   │       └── pay.error.log
│   ├── .dockerignore
│   ├── .env.example
│   ├── .gitignore
│   ├── CLAUDE.md
│   ├── CONTRIBUTING.md
│   ├── LICENSE
│   ├── README.md
│   ├── docker-compose.baota.yml
│   ├── docker-compose.swr-smoke.yml
│   ├── docker-compose.swr-test.yml
│   ├── docker-compose.yml
│   ├── pom.xml
│   ├── upgrade.md
│   └── version.md
├── jeepay-ui
│   ├── jeepay-ui-cashier
│   │   ├── public
│   │   │   └── favicon.ico
│   │   ├── src
│   │   │   ├── api
│   │   │   │   └── api.js
│   │   │   ├── assets
│   │   │   │   ├── icon
│   │   │   │   │   ├── S.svg
│   │   │   │   │   ├── error.svg
│   │   │   │   │   └── wx.svg
│   │   │   │   ├── images
│   │   │   │   │   ├── empty.svg
│   │   │   │   │   ├── loading.gif
│   │   │   │   │   ├── ysf.jpg
│   │   │   │   │   └── zfb.jpeg
│   │   │   │   └── wx-zt
│   │   │   │       ├── WeChatSansSS-Bold.ttf
│   │   │   │       ├── WeChatSansSS-Light.ttf
│   │   │   │       ├── WeChatSansSS-Medium.ttf
│   │   │   │       ├── WeChatSansSS-Regular.ttf
│   │   │   │       ├── WeChatSansStd-Bold.ttf
│   │   │   │       ├── WeChatSansStd-Light.ttf
│   │   │   │       ├── WeChatSansStd-Medium.ttf
│   │   │   │       └── WeChatSansStd-Regular.ttf
│   │   │   ├── config
│   │   │   │   ├── index.js
│   │   │   │   └── rem.js
│   │   │   ├── http
│   │   │   │   ├── HttpRequest.js
│   │   │   │   └── request.js
│   │   │   ├── router
│   │   │   │   └── index.js
│   │   │   ├── utils
│   │   │   │   ├── channelUserId.js
│   │   │   │   └── wayCode.js
│   │   │   ├── views
│   │   │   │   ├── dialog
│   │   │   │   │   ├── dialog.vue
│   │   │   │   │   └── index.js
│   │   │   │   ├── keyboard
│   │   │   │   │   └── keyboard.vue
│   │   │   │   ├── payway
│   │   │   │   │   ├── Alipay.vue
│   │   │   │   │   ├── Wxpay.vue
│   │   │   │   │   ├── Ysfpay.vue
│   │   │   │   │   └── pay.css
│   │   │   │   ├── Cashier.vue
│   │   │   │   ├── Error.vue
│   │   │   │   ├── Hub.vue
│   │   │   │   └── Oauth2Callback.vue
│   │   │   ├── App.vue
│   │   │   └── main.js
│   │   ├── .env
│   │   ├── .env.development
│   │   ├── .gitignore
│   │   ├── README.md
│   │   ├── index.html
│   │   ├── package-lock.json
│   │   ├── package.json
│   │   └── vite.config.js
│   ├── jeepay-ui-manager
│   │   ├── imgs
│   │   │   ├── defava_f.png
│   │   │   ├── defava_m.png
│   │   │   ├── favicon.ico
│   │   │   └── logo.svg
│   │   ├── src
│   │   │   ├── api
│   │   │   │   ├── login.js
│   │   │   │   └── manage.js
│   │   │   ├── assets
│   │   │   │   ├── styles
│   │   │   │   │   ├── color.css
│   │   │   │   │   └── color.less
│   │   │   │   ├── svg
│   │   │   │   │   ├── 403.svg
│   │   │   │   │   ├── 404.svg
│   │   │   │   │   ├── 500.svg
│   │   │   │   │   ├── add-icon-hover.svg
│   │   │   │   │   ├── add-icon.svg
│   │   │   │   │   ├── background.svg
│   │   │   │   │   ├── backgroundold.svg
│   │   │   │   │   ├── code.svg
│   │   │   │   │   ├── empty.svg
│   │   │   │   │   ├── jeepay.svg
│   │   │   │   │   ├── lock.svg
│   │   │   │   │   ├── mini-logo.svg
│   │   │   │   │   ├── more.svg
│   │   │   │   │   ├── operate.svg
│   │   │   │   │   ├── scroll_down.svg
│   │   │   │   │   ├── scroll_left.svg
│   │   │   │   │   ├── scroll_right.svg
│   │   │   │   │   ├── scroll_up.svg
│   │   │   │   │   ├── select-code.svg
│   │   │   │   │   ├── select-lock.svg
│   │   │   │   │   ├── select-user.svg
│   │   │   │   │   └── user.svg
│   │   │   │   ├── logo-j.svg
│   │   │   │   └── logo.svg
│   │   │   ├── components
│   │   │   │   ├── GlobalFooter
│   │   │   │   │   └── index.vue
│   │   │   │   ├── GlobalHeader
│   │   │   │   │   ├── AvatarDropdown.vue
│   │   │   │   │   └── RightContent.vue
│   │   │   │   ├── GlobalLoad
│   │   │   │   │   └── GlobalLoad.vue
│   │   │   │   ├── JeepayCard
│   │   │   │   │   └── JeepayCard.vue
│   │   │   │   ├── JeepayLayout
│   │   │   │   │   ├── JeepayLayout.vue
│   │   │   │   │   └── SubMenu.vue
│   │   │   │   ├── JeepayTable
│   │   │   │   │   ├── JeepayDrChildren.vue
│   │   │   │   │   ├── JeepayMenu.vue
│   │   │   │   │   ├── JeepayTable.vue
│   │   │   │   │   ├── JeepayTableColState.vue
│   │   │   │   │   └── JeepayTableColumns.vue
│   │   │   │   ├── JeepayTextUp
│   │   │   │   │   └── JeepayTextUp.vue
│   │   │   │   ├── JeepayUpload
│   │   │   │   │   └── JeepayUpload.vue
│   │   │   │   └── NProgress
│   │   │   │       └── nprogress.less
│   │   │   ├── config
│   │   │   │   └── appConfig.js
│   │   │   ├── core
│   │   │   │   ├── bootstrap.js
│   │   │   │   ├── lazy_use.js
│   │   │   │   └── use.js
│   │   │   ├── http
│   │   │   │   ├── HttpRequest.js
│   │   │   │   └── request.js
│   │   │   ├── layouts
│   │   │   │   ├── BasicLayout.less
│   │   │   │   ├── BasicLayout.vue
│   │   │   │   ├── BlankLayout.vue
│   │   │   │   ├── PageView.vue
│   │   │   │   ├── RouteView.vue
│   │   │   │   ├── UserLayout.vue
│   │   │   │   └── index.js
│   │   │   ├── less
│   │   │   │   ├── color.css
│   │   │   │   └── color.less
│   │   │   ├── router
│   │   │   │   ├── generator-routers.js
│   │   │   │   └── index.js
│   │   │   ├── store
│   │   │   │   └── modules
│   │   │   │       └── user.ts
│   │   │   ├── utils
│   │   │   │   ├── domUtil.js
│   │   │   │   ├── filter.js
│   │   │   │   ├── infoBox.js
│   │   │   │   ├── jeepayStorageWrapper.js
│   │   │   │   ├── screenLog.js
│   │   │   │   ├── throttle.js
│   │   │   │   ├── util.js
│   │   │   │   └── utils.less
│   │   │   ├── views
│   │   │   │   ├── current
│   │   │   │   │   ├── AvatarModal.vue
│   │   │   │   │   └── UserinfoPage.vue
│   │   │   │   ├── dashboard
│   │   │   │   │   ├── Analysis.vue
│   │   │   │   │   ├── empty.vue
│   │   │   │   │   ├── index.css
│   │   │   │   │   └── index.less
│   │   │   │   ├── ent
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   └── EntPage.vue
│   │   │   │   ├── exception
│   │   │   │   │   ├── 403.vue
│   │   │   │   │   ├── 404.vue
│   │   │   │   │   └── 500.vue
│   │   │   │   ├── isv
│   │   │   │   │   ├── custom
│   │   │   │   │   │   ├── AlipayPayConfig.vue
│   │   │   │   │   │   └── WxpayPayConfig.vue
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── IsvList.vue
│   │   │   │   │   └── IsvPayIfConfigList.vue
│   │   │   │   ├── mch
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── Detail.vue
│   │   │   │   │   └── MchList.vue
│   │   │   │   ├── mchApp
│   │   │   │   │   ├── custom
│   │   │   │   │   │   ├── AlipayPayConfig.vue
│   │   │   │   │   │   └── WxpayPayConfig.vue
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── AlipayAuth.vue
│   │   │   │   │   ├── List.vue
│   │   │   │   │   ├── MchPayConfigAddOrEdit.vue
│   │   │   │   │   ├── MchPayIfConfigList.vue
│   │   │   │   │   └── MchPayPassageAddOrEdit.vue
│   │   │   │   ├── order
│   │   │   │   │   ├── notify
│   │   │   │   │   │   └── MchNotifyList.vue
│   │   │   │   │   ├── pay
│   │   │   │   │   │   ├── PayOrderList.vue
│   │   │   │   │   │   └── RefundModal.vue
│   │   │   │   │   ├── refund
│   │   │   │   │   │   └── RefundOrderList.vue
│   │   │   │   │   └── transfer
│   │   │   │   │       ├── TransferOrderDetail.vue
│   │   │   │   │       └── TransferOrderList.vue
│   │   │   │   ├── payconfig
│   │   │   │   │   ├── payIfDefine
│   │   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   │   └── List.vue
│   │   │   │   │   └── payWay
│   │   │   │   │       ├── AddOrEdit.vue
│   │   │   │   │       └── List.vue
│   │   │   │   ├── role
│   │   │   │   │   ├── Add.vue
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── RoleDist.vue
│   │   │   │   │   └── RolePage.vue
│   │   │   │   ├── sys
│   │   │   │   │   ├── config
│   │   │   │   │   │   └── SysConfig.vue
│   │   │   │   │   └── log
│   │   │   │   │       └── SysLog.vue
│   │   │   │   ├── sysuser
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── RoleDist.vue
│   │   │   │   │   └── SysUserPage.vue
│   │   │   │   └── user
│   │   │   │       └── Login.vue
│   │   │   ├── App.vue
│   │   │   ├── global.less
│   │   │   ├── icons.ts
│   │   │   ├── main.ts
│   │   │   └── router.ts
│   │   ├── tests
│   │   │   └── unit
│   │   │       └── .eslintrc.js
│   │   ├── .browserslistrc
│   │   ├── .env
│   │   ├── .env.development
│   │   ├── .eslintrc.js
│   │   ├── .prettierrc.cjs
│   │   ├── components.d.ts
│   │   ├── index.html
│   │   ├── package-lock.json
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── vite.config.ts
│   ├── jeepay-ui-merchant
│   │   ├── indexImgs
│   │   │   ├── defava_f.png
│   │   │   ├── defava_m.png
│   │   │   ├── favicon.ico
│   │   │   └── logo.svg
│   │   ├── public
│   │   │   ├── imgs
│   │   │   │   ├── defava_f.png
│   │   │   │   ├── defava_m.png
│   │   │   │   ├── favicon.ico
│   │   │   │   └── logo.svg
│   │   │   └── index.html
│   │   ├── src
│   │   │   ├── api
│   │   │   │   ├── login.js
│   │   │   │   └── manage.js
│   │   │   ├── assets
│   │   │   │   ├── images
│   │   │   │   │   └── background.png
│   │   │   │   ├── payTestImg
│   │   │   │   │   ├── ali_app.svg
│   │   │   │   │   ├── ali_bar.svg
│   │   │   │   │   ├── ali_jsapi.svg
│   │   │   │   │   ├── ali_pc.svg
│   │   │   │   │   ├── ali_qr.svg
│   │   │   │   │   ├── ali_wap.svg
│   │   │   │   │   ├── auto_bar.svg
│   │   │   │   │   ├── jee-big.svg
│   │   │   │   │   ├── jee-quan.svg
│   │   │   │   │   ├── logo.svg
│   │   │   │   │   ├── pay_h5.png
│   │   │   │   │   ├── pp_pc.svg
│   │   │   │   │   ├── qr_cashier.svg
│   │   │   │   │   ├── scan.png
│   │   │   │   │   ├── scan.svg
│   │   │   │   │   ├── top.svg
│   │   │   │   │   ├── wx_app.svg
│   │   │   │   │   ├── wx_bar.svg
│   │   │   │   │   ├── wx_h5.svg
│   │   │   │   │   ├── wx_jsapi.svg
│   │   │   │   │   └── wx_native.svg
│   │   │   │   ├── styles
│   │   │   │   │   ├── color.css
│   │   │   │   │   └── color.less
│   │   │   │   ├── svg
│   │   │   │   │   ├── 403.svg
│   │   │   │   │   ├── 404.svg
│   │   │   │   │   ├── 500.svg
│   │   │   │   │   ├── add-icon.svg
│   │   │   │   │   ├── background.svg
│   │   │   │   │   ├── backgroundold.svg
│   │   │   │   │   ├── code.svg
│   │   │   │   │   ├── empty.svg
│   │   │   │   │   ├── jeepay.svg
│   │   │   │   │   ├── lock.svg
│   │   │   │   │   ├── mini-logo.svg
│   │   │   │   │   ├── more.svg
│   │   │   │   │   ├── operate.svg
│   │   │   │   │   ├── scroll_down.svg
│   │   │   │   │   ├── scroll_left.svg
│   │   │   │   │   ├── scroll_right.svg
│   │   │   │   │   ├── scroll_up.svg
│   │   │   │   │   ├── select-code.svg
│   │   │   │   │   ├── select-lock.svg
│   │   │   │   │   ├── select-user.svg
│   │   │   │   │   └── user.svg
│   │   │   │   ├── logo-j.svg
│   │   │   │   └── logo.svg
│   │   │   ├── components
│   │   │   │   ├── ChannelUser
│   │   │   │   │   └── ChannelUserModal.vue
│   │   │   │   ├── GlobalFooter
│   │   │   │   │   └── index.vue
│   │   │   │   ├── GlobalHeader
│   │   │   │   │   ├── AvatarDropdown.vue
│   │   │   │   │   └── RightContent.vue
│   │   │   │   ├── GlobalLoad
│   │   │   │   │   └── GlobalLoad.vue
│   │   │   │   ├── JeepayCard
│   │   │   │   │   └── JeepayCard.vue
│   │   │   │   ├── JeepayLayout
│   │   │   │   │   ├── JeepayLayout.vue
│   │   │   │   │   └── SubMenu.vue
│   │   │   │   ├── JeepayTable
│   │   │   │   │   ├── JeepayDrChildren.vue
│   │   │   │   │   ├── JeepayMenu.vue
│   │   │   │   │   ├── JeepayTable.vue
│   │   │   │   │   ├── JeepayTableColState.vue
│   │   │   │   │   └── JeepayTableColumns.vue
│   │   │   │   ├── JeepayTextUp
│   │   │   │   │   └── JeepayTextUp.vue
│   │   │   │   ├── JeepayUpload
│   │   │   │   │   └── JeepayUpload.vue
│   │   │   │   └── NProgress
│   │   │   │       └── nprogress.less
│   │   │   ├── config
│   │   │   │   └── appConfig.js
│   │   │   ├── core
│   │   │   │   ├── bootstrap.js
│   │   │   │   ├── lazy_use.js
│   │   │   │   └── use.js
│   │   │   ├── http
│   │   │   │   ├── HttpRequest.js
│   │   │   │   └── request.js
│   │   │   ├── layouts
│   │   │   │   ├── BasicLayout.less
│   │   │   │   ├── BasicLayout.vue
│   │   │   │   ├── BlankLayout.vue
│   │   │   │   ├── PageView.vue
│   │   │   │   ├── RouteView.vue
│   │   │   │   ├── UserLayout.vue
│   │   │   │   └── index.js
│   │   │   ├── less
│   │   │   │   ├── color.css
│   │   │   │   └── color.less
│   │   │   ├── router
│   │   │   │   ├── generator-routers.js
│   │   │   │   └── index.js
│   │   │   ├── store
│   │   │   │   └── modules
│   │   │   │       └── user.ts
│   │   │   ├── utils
│   │   │   │   ├── domUtil.js
│   │   │   │   ├── filter.js
│   │   │   │   ├── infoBox.js
│   │   │   │   ├── jeepayStorageWrapper.js
│   │   │   │   ├── ruleGenerator.js
│   │   │   │   ├── screenLog.js
│   │   │   │   ├── throttle.js
│   │   │   │   ├── util.js
│   │   │   │   └── utils.less
│   │   │   ├── views
│   │   │   │   ├── current
│   │   │   │   │   ├── AvatarModal.vue
│   │   │   │   │   └── UserinfoPage.vue
│   │   │   │   ├── dashboard
│   │   │   │   │   ├── Analysis.vue
│   │   │   │   │   ├── empty.vue
│   │   │   │   │   ├── index.css
│   │   │   │   │   └── index.less
│   │   │   │   ├── division
│   │   │   │   │   ├── group
│   │   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   │   └── DivisionReceiverGroupPage.vue
│   │   │   │   │   ├── receiver
│   │   │   │   │   │   ├── DivisionReceiverPage.vue
│   │   │   │   │   │   ├── ReceiverAdd.vue
│   │   │   │   │   │   └── ReceiverEdit.vue
│   │   │   │   │   └── record
│   │   │   │   │       ├── Detail.vue
│   │   │   │   │       └── DivisionRecordPage.vue
│   │   │   │   ├── exception
│   │   │   │   │   ├── 403.vue
│   │   │   │   │   ├── 404.vue
│   │   │   │   │   └── 500.vue
│   │   │   │   ├── mchApp
│   │   │   │   │   ├── custom
│   │   │   │   │   │   ├── AlipayPayConfig.vue
│   │   │   │   │   │   └── WxpayPayConfig.vue
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── AlipayAuth.vue
│   │   │   │   │   ├── List.vue
│   │   │   │   │   ├── MchPayConfigAddOrEdit.vue
│   │   │   │   │   ├── MchPayIfConfigList.vue
│   │   │   │   │   └── MchPayPassageAddOrEdit.vue
│   │   │   │   ├── mchCode
│   │   │   │   │   └── MchCodePage.vue
│   │   │   │   ├── order
│   │   │   │   │   ├── pay
│   │   │   │   │   │   ├── PayOrderList.vue
│   │   │   │   │   │   └── RefundModal.vue
│   │   │   │   │   ├── refund
│   │   │   │   │   │   └── RefundOrderList.vue
│   │   │   │   │   └── transfer
│   │   │   │   │       ├── TransferOrderDetail.vue
│   │   │   │   │       └── TransferOrderList.vue
│   │   │   │   ├── payTest
│   │   │   │   │   ├── PayTest.vue
│   │   │   │   │   ├── PayTestBarCode.vue
│   │   │   │   │   ├── PayTestModal.vue
│   │   │   │   │   └── payTest.css
│   │   │   │   ├── role
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── RoleDist.vue
│   │   │   │   │   └── RolePage.vue
│   │   │   │   ├── sysuser
│   │   │   │   │   ├── AddOrEdit.vue
│   │   │   │   │   ├── RoleDist.vue
│   │   │   │   │   └── SysUserPage.vue
│   │   │   │   ├── transfer
│   │   │   │   │   ├── MchTransferPage.css
│   │   │   │   │   └── MchTransferPage.vue
│   │   │   │   └── user
│   │   │   │       └── Login.vue
│   │   │   ├── App.vue
│   │   │   ├── global.less
│   │   │   ├── icons.ts
│   │   │   ├── main.ts
│   │   │   └── router.ts
│   │   ├── .browserslistrc
│   │   ├── .env
│   │   ├── .env.development
│   │   ├── .eslintrc.js
│   │   ├── .prettierrc.cjs
│   │   ├── components.d.ts
│   │   ├── index.html
│   │   ├── package-lock.json
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── vite.config.ts
│   ├── .dockerignore
│   ├── .gitignore
│   ├── Dockerfile
│   ├── LICENSE
│   ├── README.md
│   └── default.conf.template
├── runtime
│   ├── JEE-D01-downstream-merchant-uat-report.md
│   ├── JEE-E02-ccat-development-report.md
│   ├── JEE-E04-ccat-production-candidate-deployment-report.md
│   ├── JEE-I04-ccat-live-acceptance-git-delivery-report.md
│   ├── JEE-I05-production-candidate-acceptance-git-delivery-report.md
│   ├── JEE-I06-downstream-merchant-uat-integration-git-delivery-report.md
│   ├── apply-v2-callback-edge-hot
│   ├── assemble-td011-payment-artifact.py
│   ├── ccat-token-probe-once
│   ├── create-authorized-new-ccat-order-once
│   ├── create-first-real-ccat-order-once
│   ├── populate-v2-ccat-secret
│   ├── provision-v2-ccat-config
│   └── rollback-v2-callback-edge-hot
├── tmp
│   └── talend-jeepay-v2-create-guide.png
├── .gitignore
├── AGENTS.md
├── PROJECT_TREE.md
└── README.md

483 directories, 1539 files
```
