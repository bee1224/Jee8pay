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
ENVIRONMENT_RUNTIME_BINDING = DEFERRED UNTIL PHYSICAL ENVIRONMENT SPEC IS PROVIDED
PHYSICAL_ENVIRONMENT_CONFIGURATION = DEFERRED
```

Required later for both Development and Production: domain、VPS/host、DB、Redis/MQ、callback host and Provider connectivity mode. Provider binding additionally requires environment/account assignment and secure credential injection. No values are inferred in this document.
