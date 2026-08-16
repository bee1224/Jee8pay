# Platform Access（營運平台存取）

日期：2026-08-16

## 公開網址（營運平台）

| 環境 | 網址 | 說明 |
| --- | --- | --- |
| 測試（V2 Development） | `https://admin-v2-dev.nnviopp.com` | nnviopp sandbox edge → V2 manager-ui；Cloudflare proxied；登入保護 |
| 正式（V2 Production Candidate） | `https://admin-v2.lp33ing.com` | 指向 V2 Production Candidate（**edge 路由套用需 V1 edge recreate，見下**） |

> 正式網址的 DNS（proxied）與 TLS SAN 已就緒；V1 edge（`lp33ing-production-edge`）rootfs 為 read-only，
> 需在 V1 compose 加入 config mount + 重建 image 後 recreate（約 30–60 秒 V1 api/admin 中斷）才能生效。
> 此動作影響 live V1 基礎設施，需明確授權後執行（步驟見 `docs/operations/ccat-v2-production-candidate.md` 的 edge 章節）。

## 帳號

- 登入帳號：`jeepay`（Manager 超管，is_admin=1，兩個環境皆同）
- **密碼：不分大小寫**（系統以 toUpperCase 正規化）；實際值不寫入本 repo，以授權交付文件/營運密碼表為準
- 密碼驗證改動：`UpperCasePasswordEncoder`（`jeepay-service/.../service/utils/`），manager/merchant 登入共用；
  Merchant API 的 App Secret（MD5 簽名）**維持大小寫敏感**，不受影響

## 前端

- 三個前端（Manager/Merchant/Cashier）與 init.sql seed 已繁中化（zh-TW）。
- 測試環境已部署繁中 build；正式環境 UI images 已建置於 candidate release。

## 安全備註

- Manager/營運後台為高權限面，建議僅以登入（帳號+密碼+驗證碼）為閘；若需 IP allowlist 請另提供 IP 清單。
