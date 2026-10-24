# Operations

本分類收錄 Taiwan Workspace-specific deployment、runtime、maintenance 與 troubleshooting 文件。

## Index

- [`git-delivery-governance.md`](git-delivery-governance.md)：Git collaboration、delivery gates、JEE-G02 integration readiness 與 JEE-I01 durable build acceptance。
- [`ccat-v2-development.md`](ccat-v2-development.md)：CCAT V2 Development runtime、isolation、resource budget、secrets、access、rollback 與 public callback Human Gate。
- [`production-edge-reboot-recovery.md`](production-edge-reboot-recovery.md)：2026-10-24 正式機重開機後 edge 未啟動的根因（`docker kill --signal` 標記手動停止）、修正與看門狗。
- [`production-resource-monitor.md`](production-resource-monitor.md)：正式機 CPU／記憶體／磁碟／容器記憶體 ≥70% 的 Telegram 警戒通知。
- [`sandbox-edge-recovery.md`](sandbox-edge-recovery.md)：Sandbox public edge recovery、canonical network/config persistence、edge-only reconcile、health 與 External readiness gate。
- [`merchant-uat-frontend-operator-map.md`](merchant-uat-frontend-operator-map.md)：JEE-D01 Manager／Merchant／Cashier actual page 與 RYO_IBON（JAY_IBON／CHI_IBON）relevance mapping。
- [`ccat-v2-production-candidate.md`](ccat-v2-production-candidate.md)：CCAT-only Production Candidate 的 source provenance、隔離 topology、resource budget、secret Human Gate、callback plan 與 rollback。
- [`platform-access.md`](platform-access.md)：營運平台公開網址（測試/正式）、登入帳號政策與前端繁中化狀態。
- [`telegram-daily-summary.md`](telegram-daily-summary.md)：每日四個黑貓 PAY Provider 成功收款統計與 Telegram 發送、secret intake、驗證方式。

上游 JeePay 的部署/安裝文件與 compose 已移除；實際部署一律以 `deploy/` 為準。
