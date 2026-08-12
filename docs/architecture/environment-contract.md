# Development / Production Environment Contract

## Existing configuration model

JeePay loads service defaults from classpath `application.yml` and supports Spring Boot external `application.yml` override locations. Deployable examples under `jeepay/conf/` carry datasource、Redis、MQ、logging and `isys` settings. `SystemYmlConfig` binds application-level `isys` values; DB-backed `DBApplicationConfig.paySiteUrl` generates payment/callback URLs. Frontend builds use per-project `.env` files and the root Nginx template.

No complete Spring-profile-based Development/Production binding, physical host inventory, or Provider environment assignment exists in this Workspace. Public upstream development defaults and placeholders are examples, not proof of secure Production configuration.

## Logical platform environments

### Development

Development is for local coding、unit tests、offline integration and explicitly controlled Provider testing. It must not implicitly use Production Provider credentials/endpoints、Production DB、Production Redis/MQ、Production callback host or real merchant traffic.

### Production

Production is for real merchants、real Providers and real money. It requires explicit Production configuration、credential injection、database、Redis/MQ、public callback origin and Provider account/environment assignment.

## Provider connectivity

Platform environment and Provider connectivity are separate dimensions. A Development platform may be `Disabled`、use an official `Sandbox/Test`, or use separately approved `Controlled Live`; Production connectivity must be explicitly `Production`. `Controlled Live` is not invented for CCAT and cannot be selected until the Provider contract/account assignment exists.

Reuse the existing Provider `if_params` model for an evidence-backed endpoint/environment selector. Do not add a new global Provider registry or runtime abstraction solely for symmetry.

## Fail-closed rules

- Missing Development config must not fall back to Production.
- Missing Production config must not fall back to Development/Test.
- Endpoint、credential and declared connectivity mode must belong to the same Provider environment; mismatch fails startup or the affected Provider operation.
- Missing Provider config produces a safe error before any outbound request.
- Callback URLs must derive from the explicitly bound environment origin.
- Production secrets must come from an external secret-injection mechanism and never from source control.
- Example datasource/MQ passwords and placeholders are not Production authorization.

## Physical binding status

```text
DEVELOPMENT_RUNTIME_BINDING = JEE-E02 V2 ISOLATED RUNTIME DEPLOYED
DEVELOPMENT_PROVIDER_CREDENTIAL_BINDING = PASS; CACHE ENABLED/DISABLED VERIFIED (TD-011 RESOLVED)
DEVELOPMENT_PUBLIC_CALLBACK_BINDING = PASS (V2-ONLY HOST/PATH; EDGE RESTART GUARD REQUIRED)
PRODUCTION_RUNTIME_BINDING = DEFERRED
```

The V2 Development runtime is bound to `server1.nnviopp.com` as isolated Compose project `jee8pay-v2-dev`; operational details are in [`../operations/ccat-v2-development.md`](../operations/ccat-v2-development.md). Its CCAT connectivity is explicitly `PRODUCTION` using a V2-only secure source; exactly one standalone Token authentication passed. The first controlled order attempt exposed TD-011: with `isys.cache-config=false`, the adapter read an empty context map instead of JeePay's native cache-aware Provider-param source. The deployed Provider-local resolver now uses `ConfigContextQueryService.queryNormalMchParams` for Create、Query and APN; cache-enabled/disabled and fail-closed regressions pass while `t_pay_interface_config.if_params` remains the single source of truth. A separately authorized new TWD 40 order then completed Create、human payment、validated APN、native SUCCESS、Merchant Notify and Query reconciliation. The V2-only public APN hostname routes only `/api/pay/notify/ccat`, and the V2 application origin derives that exact URL. Because the approved zero-stop edge binding is runtime-mounted, it must be revalidated after any edge restart (TD-010). Platform Production remains outside JEE-E02 and no Platform Production values are inferred.
