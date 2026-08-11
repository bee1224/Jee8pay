# Technical Debt Register

Status vocabulary：`Open`、`In Progress`、`Resolved`、`Accepted`。

| ID | Debt | Severity | Status | Scope | Evidence | Exit Condition |
| --- | --- | --- | --- | --- | --- | --- |
| TD-001 | Provider credential at-rest protection | Unrated | Open | `t_pay_interface_config.if_params` | [`PayInterfaceConfig.ifParams`](../../jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/entity/PayInterfaceConfig.java#L82) is configuration JSON; the manager controller calls [`deSenData()`](../../jeepay/jeepay-manager/src/main/java/com/jeequan/jeepay/mgr/ctrl/merchant/MchPayInterfaceConfigController.java#L116) while preparing a response, which does not prove DB field-level encryption. No formal threat assessment is recorded. | Complete a separate security design, implementation, and verification of credential at-rest protection. |

本 register 不宣稱目前一定沒有其他 storage-layer protection；結論是「尚未證明有 field-level encryption」。JEE-G01 不修復此 debt。
