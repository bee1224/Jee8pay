# Project file tree

Generated from `/mnt/c/Users/tim.huang/Documents/Jee8pay` on 2026-08-12.

Excluded generated/metadata directories: `.git`, `node_modules`, `vendor`, `dist`, `build`, `target`, `.idea`, and `.vscode`.

Regenerate with:

```bash
tree -a -I '.git|node_modules|vendor|dist|build|target|.idea|.vscode' --dirsfirst .
```

```text
.
├── .agents
├── .codex
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
│   │   │   ├── mgr.all.log
│   │   │   └── mgr.error.log
│   │   ├── merchant
│   │   │   ├── mch.all.log
│   │   │   └── mch.error.log
│   │   └── payment
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
└── jeepay-ui
    ├── jeepay-ui-cashier
    │   ├── public
    │   │   └── favicon.ico
    │   ├── src
    │   │   ├── api
    │   │   │   └── api.js
    │   │   ├── assets
    │   │   │   ├── icon
    │   │   │   │   ├── S.svg
    │   │   │   │   ├── error.svg
    │   │   │   │   └── wx.svg
    │   │   │   ├── images
    │   │   │   │   ├── empty.svg
    │   │   │   │   ├── loading.gif
    │   │   │   │   ├── ysf.jpg
    │   │   │   │   └── zfb.jpeg
    │   │   │   └── wx-zt
    │   │   │       ├── WeChatSansSS-Bold.ttf
    │   │   │       ├── WeChatSansSS-Light.ttf
    │   │   │       ├── WeChatSansSS-Medium.ttf
    │   │   │       ├── WeChatSansSS-Regular.ttf
    │   │   │       ├── WeChatSansStd-Bold.ttf
    │   │   │       ├── WeChatSansStd-Light.ttf
    │   │   │       ├── WeChatSansStd-Medium.ttf
    │   │   │       └── WeChatSansStd-Regular.ttf
    │   │   ├── config
    │   │   │   ├── index.js
    │   │   │   └── rem.js
    │   │   ├── http
    │   │   │   ├── HttpRequest.js
    │   │   │   └── request.js
    │   │   ├── router
    │   │   │   └── index.js
    │   │   ├── utils
    │   │   │   ├── channelUserId.js
    │   │   │   └── wayCode.js
    │   │   ├── views
    │   │   │   ├── dialog
    │   │   │   │   ├── dialog.vue
    │   │   │   │   └── index.js
    │   │   │   ├── keyboard
    │   │   │   │   └── keyboard.vue
    │   │   │   ├── payway
    │   │   │   │   ├── Alipay.vue
    │   │   │   │   ├── Wxpay.vue
    │   │   │   │   ├── Ysfpay.vue
    │   │   │   │   └── pay.css
    │   │   │   ├── Cashier.vue
    │   │   │   ├── Error.vue
    │   │   │   ├── Hub.vue
    │   │   │   └── Oauth2Callback.vue
    │   │   ├── App.vue
    │   │   └── main.js
    │   ├── .env
    │   ├── .env.development
    │   ├── .gitignore
    │   ├── README.md
    │   ├── index.html
    │   ├── package-lock.json
    │   ├── package.json
    │   └── vite.config.js
    ├── jeepay-ui-manager
    │   ├── imgs
    │   │   ├── defava_f.png
    │   │   ├── defava_m.png
    │   │   ├── favicon.ico
    │   │   └── logo.svg
    │   ├── src
    │   │   ├── api
    │   │   │   ├── login.js
    │   │   │   └── manage.js
    │   │   ├── assets
    │   │   │   ├── styles
    │   │   │   │   ├── color.css
    │   │   │   │   └── color.less
    │   │   │   ├── svg
    │   │   │   │   ├── 403.svg
    │   │   │   │   ├── 404.svg
    │   │   │   │   ├── 500.svg
    │   │   │   │   ├── add-icon-hover.svg
    │   │   │   │   ├── add-icon.svg
    │   │   │   │   ├── background.svg
    │   │   │   │   ├── backgroundold.svg
    │   │   │   │   ├── code.svg
    │   │   │   │   ├── empty.svg
    │   │   │   │   ├── jeepay.svg
    │   │   │   │   ├── lock.svg
    │   │   │   │   ├── mini-logo.svg
    │   │   │   │   ├── more.svg
    │   │   │   │   ├── operate.svg
    │   │   │   │   ├── scroll_down.svg
    │   │   │   │   ├── scroll_left.svg
    │   │   │   │   ├── scroll_right.svg
    │   │   │   │   ├── scroll_up.svg
    │   │   │   │   ├── select-code.svg
    │   │   │   │   ├── select-lock.svg
    │   │   │   │   ├── select-user.svg
    │   │   │   │   └── user.svg
    │   │   │   ├── logo-j.svg
    │   │   │   └── logo.svg
    │   │   ├── components
    │   │   │   ├── GlobalFooter
    │   │   │   │   └── index.vue
    │   │   │   ├── GlobalHeader
    │   │   │   │   ├── AvatarDropdown.vue
    │   │   │   │   └── RightContent.vue
    │   │   │   ├── GlobalLoad
    │   │   │   │   └── GlobalLoad.vue
    │   │   │   ├── JeepayCard
    │   │   │   │   └── JeepayCard.vue
    │   │   │   ├── JeepayLayout
    │   │   │   │   ├── JeepayLayout.vue
    │   │   │   │   └── SubMenu.vue
    │   │   │   ├── JeepayTable
    │   │   │   │   ├── JeepayDrChildren.vue
    │   │   │   │   ├── JeepayMenu.vue
    │   │   │   │   ├── JeepayTable.vue
    │   │   │   │   ├── JeepayTableColState.vue
    │   │   │   │   └── JeepayTableColumns.vue
    │   │   │   ├── JeepayTextUp
    │   │   │   │   └── JeepayTextUp.vue
    │   │   │   ├── JeepayUpload
    │   │   │   │   └── JeepayUpload.vue
    │   │   │   └── NProgress
    │   │   │       └── nprogress.less
    │   │   ├── config
    │   │   │   └── appConfig.js
    │   │   ├── core
    │   │   │   ├── bootstrap.js
    │   │   │   ├── lazy_use.js
    │   │   │   └── use.js
    │   │   ├── http
    │   │   │   ├── HttpRequest.js
    │   │   │   └── request.js
    │   │   ├── layouts
    │   │   │   ├── BasicLayout.less
    │   │   │   ├── BasicLayout.vue
    │   │   │   ├── BlankLayout.vue
    │   │   │   ├── PageView.vue
    │   │   │   ├── RouteView.vue
    │   │   │   ├── UserLayout.vue
    │   │   │   └── index.js
    │   │   ├── less
    │   │   │   ├── color.css
    │   │   │   └── color.less
    │   │   ├── router
    │   │   │   ├── generator-routers.js
    │   │   │   └── index.js
    │   │   ├── store
    │   │   │   └── modules
    │   │   │       └── user.ts
    │   │   ├── utils
    │   │   │   ├── domUtil.js
    │   │   │   ├── filter.js
    │   │   │   ├── infoBox.js
    │   │   │   ├── jeepayStorageWrapper.js
    │   │   │   ├── screenLog.js
    │   │   │   ├── throttle.js
    │   │   │   ├── util.js
    │   │   │   └── utils.less
    │   │   ├── views
    │   │   │   ├── current
    │   │   │   │   ├── AvatarModal.vue
    │   │   │   │   └── UserinfoPage.vue
    │   │   │   ├── dashboard
    │   │   │   │   ├── Analysis.vue
    │   │   │   │   ├── empty.vue
    │   │   │   │   ├── index.css
    │   │   │   │   └── index.less
    │   │   │   ├── ent
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   └── EntPage.vue
    │   │   │   ├── exception
    │   │   │   │   ├── 403.vue
    │   │   │   │   ├── 404.vue
    │   │   │   │   └── 500.vue
    │   │   │   ├── isv
    │   │   │   │   ├── custom
    │   │   │   │   │   ├── AlipayPayConfig.vue
    │   │   │   │   │   └── WxpayPayConfig.vue
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── IsvList.vue
    │   │   │   │   └── IsvPayIfConfigList.vue
    │   │   │   ├── mch
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── Detail.vue
    │   │   │   │   └── MchList.vue
    │   │   │   ├── mchApp
    │   │   │   │   ├── custom
    │   │   │   │   │   ├── AlipayPayConfig.vue
    │   │   │   │   │   └── WxpayPayConfig.vue
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── AlipayAuth.vue
    │   │   │   │   ├── List.vue
    │   │   │   │   ├── MchPayConfigAddOrEdit.vue
    │   │   │   │   ├── MchPayIfConfigList.vue
    │   │   │   │   └── MchPayPassageAddOrEdit.vue
    │   │   │   ├── order
    │   │   │   │   ├── notify
    │   │   │   │   │   └── MchNotifyList.vue
    │   │   │   │   ├── pay
    │   │   │   │   │   ├── PayOrderList.vue
    │   │   │   │   │   └── RefundModal.vue
    │   │   │   │   ├── refund
    │   │   │   │   │   └── RefundOrderList.vue
    │   │   │   │   └── transfer
    │   │   │   │       ├── TransferOrderDetail.vue
    │   │   │   │       └── TransferOrderList.vue
    │   │   │   ├── payconfig
    │   │   │   │   ├── payIfDefine
    │   │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   │   └── List.vue
    │   │   │   │   └── payWay
    │   │   │   │       ├── AddOrEdit.vue
    │   │   │   │       └── List.vue
    │   │   │   ├── role
    │   │   │   │   ├── Add.vue
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── RoleDist.vue
    │   │   │   │   └── RolePage.vue
    │   │   │   ├── sys
    │   │   │   │   ├── config
    │   │   │   │   │   └── SysConfig.vue
    │   │   │   │   └── log
    │   │   │   │       └── SysLog.vue
    │   │   │   ├── sysuser
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── RoleDist.vue
    │   │   │   │   └── SysUserPage.vue
    │   │   │   └── user
    │   │   │       └── Login.vue
    │   │   ├── App.vue
    │   │   ├── global.less
    │   │   ├── icons.ts
    │   │   ├── main.ts
    │   │   └── router.ts
    │   ├── tests
    │   │   └── unit
    │   │       └── .eslintrc.js
    │   ├── .browserslistrc
    │   ├── .env
    │   ├── .env.development
    │   ├── .eslintrc.js
    │   ├── .prettierrc.cjs
    │   ├── components.d.ts
    │   ├── index.html
    │   ├── package-lock.json
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── vite.config.ts
    ├── jeepay-ui-merchant
    │   ├── indexImgs
    │   │   ├── defava_f.png
    │   │   ├── defava_m.png
    │   │   ├── favicon.ico
    │   │   └── logo.svg
    │   ├── public
    │   │   ├── imgs
    │   │   │   ├── defava_f.png
    │   │   │   ├── defava_m.png
    │   │   │   ├── favicon.ico
    │   │   │   └── logo.svg
    │   │   └── index.html
    │   ├── src
    │   │   ├── api
    │   │   │   ├── login.js
    │   │   │   └── manage.js
    │   │   ├── assets
    │   │   │   ├── images
    │   │   │   │   └── background.png
    │   │   │   ├── payTestImg
    │   │   │   │   ├── ali_app.svg
    │   │   │   │   ├── ali_bar.svg
    │   │   │   │   ├── ali_jsapi.svg
    │   │   │   │   ├── ali_pc.svg
    │   │   │   │   ├── ali_qr.svg
    │   │   │   │   ├── ali_wap.svg
    │   │   │   │   ├── auto_bar.svg
    │   │   │   │   ├── jee-big.svg
    │   │   │   │   ├── jee-quan.svg
    │   │   │   │   ├── logo.svg
    │   │   │   │   ├── pay_h5.png
    │   │   │   │   ├── pp_pc.svg
    │   │   │   │   ├── qr_cashier.svg
    │   │   │   │   ├── scan.png
    │   │   │   │   ├── scan.svg
    │   │   │   │   ├── top.svg
    │   │   │   │   ├── wx_app.svg
    │   │   │   │   ├── wx_bar.svg
    │   │   │   │   ├── wx_h5.svg
    │   │   │   │   ├── wx_jsapi.svg
    │   │   │   │   └── wx_native.svg
    │   │   │   ├── styles
    │   │   │   │   ├── color.css
    │   │   │   │   └── color.less
    │   │   │   ├── svg
    │   │   │   │   ├── 403.svg
    │   │   │   │   ├── 404.svg
    │   │   │   │   ├── 500.svg
    │   │   │   │   ├── add-icon.svg
    │   │   │   │   ├── background.svg
    │   │   │   │   ├── backgroundold.svg
    │   │   │   │   ├── code.svg
    │   │   │   │   ├── empty.svg
    │   │   │   │   ├── jeepay.svg
    │   │   │   │   ├── lock.svg
    │   │   │   │   ├── mini-logo.svg
    │   │   │   │   ├── more.svg
    │   │   │   │   ├── operate.svg
    │   │   │   │   ├── scroll_down.svg
    │   │   │   │   ├── scroll_left.svg
    │   │   │   │   ├── scroll_right.svg
    │   │   │   │   ├── scroll_up.svg
    │   │   │   │   ├── select-code.svg
    │   │   │   │   ├── select-lock.svg
    │   │   │   │   ├── select-user.svg
    │   │   │   │   └── user.svg
    │   │   │   ├── logo-j.svg
    │   │   │   └── logo.svg
    │   │   ├── components
    │   │   │   ├── ChannelUser
    │   │   │   │   └── ChannelUserModal.vue
    │   │   │   ├── GlobalFooter
    │   │   │   │   └── index.vue
    │   │   │   ├── GlobalHeader
    │   │   │   │   ├── AvatarDropdown.vue
    │   │   │   │   └── RightContent.vue
    │   │   │   ├── GlobalLoad
    │   │   │   │   └── GlobalLoad.vue
    │   │   │   ├── JeepayCard
    │   │   │   │   └── JeepayCard.vue
    │   │   │   ├── JeepayLayout
    │   │   │   │   ├── JeepayLayout.vue
    │   │   │   │   └── SubMenu.vue
    │   │   │   ├── JeepayTable
    │   │   │   │   ├── JeepayDrChildren.vue
    │   │   │   │   ├── JeepayMenu.vue
    │   │   │   │   ├── JeepayTable.vue
    │   │   │   │   ├── JeepayTableColState.vue
    │   │   │   │   └── JeepayTableColumns.vue
    │   │   │   ├── JeepayTextUp
    │   │   │   │   └── JeepayTextUp.vue
    │   │   │   ├── JeepayUpload
    │   │   │   │   └── JeepayUpload.vue
    │   │   │   └── NProgress
    │   │   │       └── nprogress.less
    │   │   ├── config
    │   │   │   └── appConfig.js
    │   │   ├── core
    │   │   │   ├── bootstrap.js
    │   │   │   ├── lazy_use.js
    │   │   │   └── use.js
    │   │   ├── http
    │   │   │   ├── HttpRequest.js
    │   │   │   └── request.js
    │   │   ├── layouts
    │   │   │   ├── BasicLayout.less
    │   │   │   ├── BasicLayout.vue
    │   │   │   ├── BlankLayout.vue
    │   │   │   ├── PageView.vue
    │   │   │   ├── RouteView.vue
    │   │   │   ├── UserLayout.vue
    │   │   │   └── index.js
    │   │   ├── less
    │   │   │   ├── color.css
    │   │   │   └── color.less
    │   │   ├── router
    │   │   │   ├── generator-routers.js
    │   │   │   └── index.js
    │   │   ├── store
    │   │   │   └── modules
    │   │   │       └── user.ts
    │   │   ├── utils
    │   │   │   ├── domUtil.js
    │   │   │   ├── filter.js
    │   │   │   ├── infoBox.js
    │   │   │   ├── jeepayStorageWrapper.js
    │   │   │   ├── ruleGenerator.js
    │   │   │   ├── screenLog.js
    │   │   │   ├── throttle.js
    │   │   │   ├── util.js
    │   │   │   └── utils.less
    │   │   ├── views
    │   │   │   ├── current
    │   │   │   │   ├── AvatarModal.vue
    │   │   │   │   └── UserinfoPage.vue
    │   │   │   ├── dashboard
    │   │   │   │   ├── Analysis.vue
    │   │   │   │   ├── empty.vue
    │   │   │   │   ├── index.css
    │   │   │   │   └── index.less
    │   │   │   ├── division
    │   │   │   │   ├── group
    │   │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   │   └── DivisionReceiverGroupPage.vue
    │   │   │   │   ├── receiver
    │   │   │   │   │   ├── DivisionReceiverPage.vue
    │   │   │   │   │   ├── ReceiverAdd.vue
    │   │   │   │   │   └── ReceiverEdit.vue
    │   │   │   │   └── record
    │   │   │   │       ├── Detail.vue
    │   │   │   │       └── DivisionRecordPage.vue
    │   │   │   ├── exception
    │   │   │   │   ├── 403.vue
    │   │   │   │   ├── 404.vue
    │   │   │   │   └── 500.vue
    │   │   │   ├── mchApp
    │   │   │   │   ├── custom
    │   │   │   │   │   ├── AlipayPayConfig.vue
    │   │   │   │   │   └── WxpayPayConfig.vue
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── AlipayAuth.vue
    │   │   │   │   ├── List.vue
    │   │   │   │   ├── MchPayConfigAddOrEdit.vue
    │   │   │   │   ├── MchPayIfConfigList.vue
    │   │   │   │   └── MchPayPassageAddOrEdit.vue
    │   │   │   ├── mchCode
    │   │   │   │   └── MchCodePage.vue
    │   │   │   ├── order
    │   │   │   │   ├── pay
    │   │   │   │   │   ├── PayOrderList.vue
    │   │   │   │   │   └── RefundModal.vue
    │   │   │   │   ├── refund
    │   │   │   │   │   └── RefundOrderList.vue
    │   │   │   │   └── transfer
    │   │   │   │       ├── TransferOrderDetail.vue
    │   │   │   │       └── TransferOrderList.vue
    │   │   │   ├── payTest
    │   │   │   │   ├── PayTest.vue
    │   │   │   │   ├── PayTestBarCode.vue
    │   │   │   │   ├── PayTestModal.vue
    │   │   │   │   └── payTest.css
    │   │   │   ├── role
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── RoleDist.vue
    │   │   │   │   └── RolePage.vue
    │   │   │   ├── sysuser
    │   │   │   │   ├── AddOrEdit.vue
    │   │   │   │   ├── RoleDist.vue
    │   │   │   │   └── SysUserPage.vue
    │   │   │   ├── transfer
    │   │   │   │   ├── MchTransferPage.css
    │   │   │   │   └── MchTransferPage.vue
    │   │   │   └── user
    │   │   │       └── Login.vue
    │   │   ├── App.vue
    │   │   ├── global.less
    │   │   ├── icons.ts
    │   │   ├── main.ts
    │   │   └── router.ts
    │   ├── .browserslistrc
    │   ├── .env
    │   ├── .env.development
    │   ├── .eslintrc.js
    │   ├── .prettierrc.cjs
    │   ├── components.d.ts
    │   ├── index.html
    │   ├── package-lock.json
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── vite.config.ts
    ├── .dockerignore
    ├── .gitignore
    ├── Dockerfile
    ├── LICENSE
    ├── README.md
    └── default.conf.template

390 directories, 1035 files
```

