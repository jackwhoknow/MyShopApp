# MyShopApp 运维手册（Runbook）

本文档面向开发与运维，覆盖本地启动、生产部署、数据库迁移、备份恢复、可观测性与发布回滚。

## 1. 系统组成

| 服务 | 技术栈 | 端口（默认） | 说明 |
| --- | --- | --- | --- |
| backend | Spring Boot 3.5 / Java 21 | 127.0.0.1:8080 | 交易、支付、营销、客服等全部业务接口 |
| frontend | Vue 3 + Vite + Nginx | 80 | 静态资源 + `/api` 反向代理 + 边缘限流 |
| mysql | MySQL 8.0 | 容器内网 3306 | 主数据存储，Flyway 管理表结构 |
| redis | Redis 7 | 127.0.0.1:6379 | 验证码、限流、token 黑名单、缓存、热搜 |
| rabbitmq | RabbitMQ 3 | 127.0.0.1:5672 / 15672 | 秒杀下单削峰 |

数据卷：`mysql-data`、`redis-data`、`rabbitmq-data`、`upload-data`、`app-logs`。

## 2. 本地启动

1. 复制环境变量模板并填写：
   ```bash
   cp .env.example .env
   ```
2. 启动全部服务：
   ```bash
   docker compose up -d --build
   ```
3. 访问：
   - 前端：http://localhost:8088（`.env` 中 `WEB_PORT`）
   - 后端健康检查：http://localhost:8080/actuator/health
   - 接口文档：http://localhost:8080/swagger-ui.html

## 3. 生产部署

生产使用基础 compose + 生产叠加层，集中启用日志轮转、资源上限、慢查询日志并关闭接口文档：

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

部署前检查：

- `.env` 中 `JWT_SECRET` 不少于 32 位随机字符，且未提交到仓库。
- 真实支付参数与证书放在 `secrets/` 并只读挂载；生产 profile 已硬编码关闭模拟支付，
  无需（也不支持）通过环境变量开启。
- `CORS_ALLOWED_ORIGINS`、`PAY_NOTIFY_BASE_URL` 使用正式 HTTPS 域名。
- 外网只暴露前端端口，`8080/6379/5672` 仅监听 127.0.0.1。

## 4. 环境变量

完整清单见 [.env.example](../.env.example)，关键项：

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` / `DB_PASSWORD` | 是 | MySQL root 与业务账号密码 |
| `REDIS_PASSWORD` | 是 | Redis 访问密码 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | 是 | MQ 账号 |
| `JWT_SECRET` | 是 | 至少 32 位，修改后所有登录态失效 |
| `API_DOCS_ENABLED` | 否 | 生产默认 false，临时排障可置 true |
| `ORDER_AUTO_COMPLETE_DAYS` | 否 | 发货后自动确认收货天数，默认 7 |
| `AFTER_SALE_CLOSE_DAYS` | 否 | 售后超时自动关闭天数，默认 7 |
| `COUPON_EXPIRY_WARN_DAYS` | 否 | 优惠券到期提醒提前天数，默认 3 |
| `MAIL_ENABLED` | 否 | 开启后找回密码验证码走 SMTP，默认 false 只写日志 |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | 条件 | `MAIL_ENABLED=true` 时必填，否则启动失败 |
| `LOGISTICS_PROVIDER` | 是 | 生产必须为 `kuaidi100`；`mock` 仅供自动化测试，使用会在启动时被拒绝 |
| `KUAIDI100_CUSTOMER` / `KUAIDI100_KEY` | 是 | `LOGISTICS_PROVIDER=kuaidi100` 时必填，缺失则拒绝启动 |
| `KUAIDI100_ENDPOINT` | 否 | 快递100 查询网关，默认官方地址 |
| `UPLOAD_STORAGE` | 否 | `local`（默认）或 `s3`；多实例部署必须用 `s3` |
| `S3_ENDPOINT` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_BUCKET` | 条件 | `UPLOAD_STORAGE=s3` 时必填 |
| `S3_PUBLIC_BASE_URL` | 否 | 图片公网访问前缀（CDN），留空回退到 endpoint/bucket |

### 4.1 邮件通道（SMTP）

- 本地开发保持 `MAIL_ENABLED=false`，验证码会以 `[EMAIL]` 前缀打印到日志。
- 生产置 `MAIL_ENABLED=true`，并填写 `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` /
  `MAIL_PASSWORD` / `MAIL_FROM`；缺项会在应用启动阶段直接失败，避免验证码静默丢失。
- 端口 465 使用 `MAIL_SSL_ENABLED=true`、`MAIL_STARTTLS_ENABLED=false`；
  端口 587 使用 `MAIL_SSL_ENABLED=false`、`MAIL_STARTTLS_ENABLED=true`。
- 无需认证的 relay 可将 `MAIL_SMTP_AUTH=false`，此时不要求账号密码。
- 目前邮件用于找回密码验证码，业务通知仍以站内信为主。

### 4.2 物流查询与发货

- 生产固定使用快递100 实时查询：按快递公司名称映射编码调用官方接口，
  缺 `KUAIDI100_CUSTOMER` / `KUAIDI100_KEY` 或配置了未知渠道时应用拒绝启动，
  不会出现“查不到轨迹却当成没有轨迹”的静默降级。
- 接口调用失败时记录错误日志并返回空轨迹，不伪造任何物流节点；此时应到
  快递100 后台确认账号状态、签名与配额。
- 商家发货必须填写真实的物流公司与运单号，系统不生成、不改写单号；
  模拟单号（`MOCK` 前缀）会被直接拒绝。同一包裹的多个子订单共用一个真实运单号。
- `LOGISTICS_PROVIDER=mock` 仅用于自动化测试，生产环境不可使用。

### 4.3 图片存储

- `local` 适合单机；容器重建后需保留 `upload-data` 卷，否则图片丢失。
- 多实例或需要 CDN 时使用 `s3`：上传返回 `S3_PUBLIC_BASE_URL` 下的完整 URL，
  桶不存在会自动创建，桶的读权限需在对象存储侧配置为公共读或经 CDN 授权。

### 4.4 真实支付通道

支付在支付宝/微信官方页面完成，本站只生成带签名的下单参数并验签回调，不接触银行卡或支付密码。

**支付宝（电脑网站支付）**

1. 申请电脑网站支付能力，生成应用 RSA2 密钥，把应用公钥上传到开放平台。
2. 将应用私钥（PKCS8 PEM）和支付宝公钥分别放到 `secrets/alipay_private_key.pem`、
   `secrets/alipay_public_key.pem`，容器以只读方式挂载 `/app/secrets`。
3. 配置 `ALIPAY_ENABLED=true`、`ALIPAY_APP_ID`、`ALIPAY_SELLER_ID`（可选）、
   `ALIPAY_RETURN_URL`；回调地址为 `${PAY_NOTIFY_BASE_URL}/api/payments/alipay/notify`。
4. 沙箱联调时把 `ALIPAY_SERVER_URL` 改为沙箱网关；上线前改回正式网关。

**微信支付（APIv3 Native）**

1. 开通 Native 支付，准备商户号、API 证书序列号、商户 API 私钥（PKCS8 PEM）、
   32 位 APIv3 密钥和微信支付平台证书 PEM。
2. 文件放到 `secrets/wechat_private_key.pem`、`secrets/wechat_platform_cert.pem`，
   配置 `WECHAT_PAY_ENABLED=true` 及对应的 `WECHAT_PAY_*` 环境变量。
3. 回调地址为 `${PAY_NOTIFY_BASE_URL}/api/payments/wechat/notify`，需在商户平台登记。
4. 平台证书会轮换：更新 `wechat_platform_cert.pem` 后滚动重启后端；序列号不匹配时
   回调会被拒绝，日志中有明确提示。

**联调与验证**

- 前端通过 `GET /api/payments/channels` 只展示已配置渠道，未配置的渠道不会出现。
- 启动时校验已启用渠道的密钥文件与回调地址；配置不完整会拒绝启动。
- 测试流程：下单 → 选择渠道 → 完成官方收银台支付 → 前端轮询订单状态；
  回调与订单金额、第三方交易号做幂等校验，重复回调不会重复入账。
- 退款按原渠道原路退回；渠道临时不可用会记为 `REFUND_REQUIRED`，由对账任务重试。

## 5. 数据库迁移规范

- 所有表结构变更必须新增 `V{n}__描述.sql`，禁止修改已执行的历史脚本。
- 启动时 `ddl-auto: validate`，实体与表结构不一致会直接启动失败。
- 迁移脚本需向后兼容：先加列/加索引，再切代码；删列/改名分两次发布。
- 出现 checksum 冲突时先排查是否有历史脚本被改动，禁止直接 `flyway repair` 掩盖问题。
- 高频查询索引统一在迁移中维护，例如 `V11__query_indexes.sql`。

## 6. 备份与恢复

### 6.1 备份

Windows：

```powershell
$env:DB_PASSWORD="<root 密码>"
.\scripts\backup-db.ps1 -BackupDir .\backups -KeepDays 7
```

Linux / CI：

```bash
DB_PASSWORD='<root 密码>' ./scripts/backup-db.sh
```

默认保留 7 天，建议通过 cron 每日执行：

```cron
0 3 * * * cd /opt/myshop && DB_PASSWORD='***' ./scripts/backup-db.sh >> /var/log/myshop-backup.log 2>&1
```

### 6.2 恢复

```powershell
$env:DB_PASSWORD="<root 密码>"
.\scripts\restore-db.ps1 -BackupFile .\backups\myshop-20260101-030000.sql
```

```bash
DB_PASSWORD='<root 密码>' ./scripts/restore-db.sh ./backups/myshop-20260101-030000.sql.gz
```

恢复会覆盖目标库，脚本默认要求输入 `YES` 二次确认；演练时使用独立库名。

### 6.3 演练建议

- 每季度至少做一次恢复演练，记录恢复耗时与数据校验结果。
- 恢复后执行 `docker compose restart backend`，让 Flyway 校验结构与迁移记录。
- 重要发布前手动打一次全量备份，并保留到发布后 7 天。

## 7. 可观测性

| 端点 | 用途 |
| --- | --- |
| `/actuator/health` | 存活/依赖健康（MySQL、Redis、RabbitMQ、磁盘） |
| `/actuator/info` | 应用名称、版本、构建时间 |
| `/actuator/prometheus` | Prometheus 抓取指标（JVM、HTTP、数据源、连接池） |

- 每个请求响应头返回 `X-Trace-Id`，日志格式已包含 `traceId`，排障时按该 ID 串联。
- 日志按 50MB 滚动、保留 30 天、总量上限 2GB；生产叠加层对容器 stdout 再做轮转。
- 建议告警：`http_server_requests_seconds` P99、`hikaricp_connections_pending`、`jvm_memory_used_bytes`、`rabbitmq_queue_messages`、健康检查连续失败。

## 8. 发布与回滚

发布：

1. 合并代码并通过 CI（后端 `mvn verify`、前端 `type-check` + `build`）。
2. 先执行向后兼容的数据库迁移。
3. `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build backend frontend`。
4. 检查 `/actuator/health` 为 `UP`，再跑下单/支付冒烟。

回滚：

1. 回退镜像到上一版本并重启（数据库不回滚）。
2. 若新版本已写入不兼容数据，使用发布前备份恢复到独立库核对后再决定。
3. 回滚后观察错误率与订单量 15 分钟。

## 9. 常见故障

| 现象 | 排查方向 |
| --- | --- |
| 后端启动失败，提示 schema 校验不一致 | 检查是否漏执行迁移、实体列名是否与迁移一致 |
| Flyway checksum mismatch | 确认历史脚本是否被修改，恢复原文件后重启 |
| 登录提示验证码错误 | 检查 Redis 连接与密码，验证码键 `myshop:captcha:*` |
| 下单提示排队中 | 检查 RabbitMQ 消费者与队列堆积，必要时关闭 `ORDER_ASYNC_ENABLED` 走同步下单 |
| 接口 429 | 前端 Nginx 边缘限流或后端 Redis 令牌桶触发，按 IP 定位 |
| 定时任务不执行 | 多实例下 ShedLock 需同一数据库，检查 `shedlock` 表与实例时钟 |
| 邮件验证码收不到 | 本地看日志 `[EMAIL]`；生产检查 `MAIL_ENABLED`、SMTP 账号密码与 465/587 端口配置 |
| 启动报“支付配置错误” | 已启用渠道的密钥路径不可读或回调地址为空，检查 `secrets/` 挂载与 `PAY_NOTIFY_BASE_URL` |
| 支付宝回调一直重试 | 应用私钥/支付宝公钥配置颠倒、沙箱与正式网关混用，或 `total_amount` 与订单不一致 |
| 微信回调报证书序列号不匹配 | 平台证书已轮换，更新 `wechat_platform_cert.pem` 并重启后端 |
| 收银台没有支付选项 | 前端拉取 `/api/payments/channels` 为空，说明服务端未启用任何真实渠道 |

## 10. 安全基线

- 密钥、证书只通过环境变量或 `secrets/` 只读挂载，禁止写入镜像与仓库。
- 生产关闭 Swagger（`API_DOCS_ENABLED=false`），Prometheus 端点只允许内网访问。
- 依赖升级由 Dependabot 每周提出 PR，合并前必须通过 CI。
- 上传文件限制类型与大小；评价/售后图片地址按 `/uploads/` 或对象存储前缀白名单校验，外部链接不入库。
- 自助注销仅对买家开放：商家/管理员需人工处理；注销会匿名化资料并吊销全部 token。
