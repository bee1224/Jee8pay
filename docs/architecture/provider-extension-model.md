# JeePay Provider Extension Model

本文件記錄 P01 已由 code 驗證的 extension contract；路徑均相對於 workspace root。

## Provider Resolution

```text
MchPayPassage.ifCode
→ Spring Bean
→ ${ifCode}PaymentService
```

`AbstractPayOrderController` 依 `ifCode + "PaymentService"` 取 bean：`jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/AbstractPayOrderController.java`。

## PayWay Resolution

```text
wayCode
→ PaywayUtil
→ payway/<CamelCaseWayCode>
```

reflection resolution 位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/util/PaywayUtil.java`；Provider payway 位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/<provider>/payway/`。

## Create Flow

```text
Merchant
→ UnifiedOrderController
→ AbstractPayOrderController
→ MchPayPassage
→ Provider PaymentService
→ PayWay
→ Provider
→ ChannelRetMsg
→ PayOrder
```

核心 order controller 位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/AbstractPayOrderController.java`。

## Upstream Query Flow

```text
Merchant Query
→ local PayOrder only
```

`QueryOrderController` 位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/QueryOrderController.java`，不應改為每次打 upstream。

```text
PayOrder Reissue
→ ChannelOrderReissueService
→ ${ifCode}PayOrderQueryService
→ Provider
→ ChannelRetMsg
→ local PayOrder transition
```

query bean resolution 位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/service/ChannelOrderReissueService.java`。

## Notify Flow

```text
Provider
→ ChannelNoticeController
→ Provider ChannelNoticeService
→ PayOrder state transition
→ PayOrderProcessService
→ PayMchNotifyService
→ MQ
→ Merchant
```

callback bean resolution 位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/ctrl/payorder/ChannelNoticeController.java`。Provider 只處理 Provider-specific validation、status parsing 與 ACK；Core 保有 state transition 與 merchant notify。

## Configuration Flow

```text
Manager/Merchant UI
→ PayInterfaceConfig
→ if_params JSON
→ NormalMchParams.factory()
→ Provider Params
→ Provider Adapter
```

factory 位於 `jeepay/jeepay-core/src/main/java/com/jeequan/jeepay/core/model/params/NormalMchParams.java`；config context 使用處位於 `jeepay/jeepay-payment/src/main/java/com/jeequan/jeepay/pay/service/ConfigContextService.java`。

## Extension Boundary

- GREEN：Provider-specific channel、params、ifCode、seed/config data 與 tests。
- YELLOW：既有 extension point 明確不足時，才最小化修改 shared request/response、callback、config rendering 或 Provider resolution；先提出 code evidence 與 alternative。
- RED：不得因新增 Provider 修改 PayOrder domain/state machine、order/notice controller、`PayOrderProcessService`、merchant notify MQ/retry、authentication 或 RBAC，除非任務明確重設 core architecture。
