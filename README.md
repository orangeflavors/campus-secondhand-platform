# 校园集市 · 校园二手交易与物品流转平台

面向校园内部用户的二手交易平台，依据《需求规格说明书》（SRS-CSTP-001 v1.1）实现。平台把「身份认证 → 发布审核 → 搜索发现 → 收藏咨询 → 请求确认 → 线下交付 → 评价举报 → 管理处置 → 统计分析」纳入可追踪的业务闭环，并实现了服务端鉴权、并发一致性、双确认完成与审计留痕。

> 教学模拟声明：本项目为课程项目实现，校园认证为模拟流程，不提供在线支付、资金托管、物流配送与商品鉴定；所有演示账号、商品与交易数据均为虚构。

---

## 一、技术栈

| 层次 | 技术 |
|---|---|
| 前端 | Vue 3.5、Vite 6、TypeScript 5.7、Element Plus 2.14、Pinia、Vue Router 4、Axios、ECharts 5 |
| 后端 | Java 21、Spring Boot 3.3.5、Spring Security（JWT）、MyBatis 3.0、Bean Validation、Lombok |
| 数据库 | MySQL 8.0（utf8mb4），MyBatis XML 映射 |
| 构建 | Maven（后端）、pnpm + Vite（前端） |

---

## 二、已实现功能

| 模块 | 对应需求 | 实现要点 |
|---|---|---|
| 注册 / 登录 / 找回凭据 | FR-001 | BCrypt 密码、JWT 无状态会话、用户名+学号模拟找回 |
| 校园身份认证 | FR-002、FR-003 | 认证状态/方式/时间；未认证无法浏览与交易；联系方式可见范围 |
| 账户状态限制 | FR-004 | NORMAL / LIMITED / BANNED；服务端拦截并返回明确提示 |
| 商品草稿、提交审核、编辑、下架、删除 | FR-005、FR-006、FR-008 | 商品状态机；关键字段修改重新审核；有活跃订单不可下架 |
| 管理员审核与违规下架 | FR-007、FR-009 | 批准 / 驳回（必填原因）/ 违规下架；违规时关联订单转争议 |
| 搜索与组合筛选 | FR-010 ～ FR-013 | 关键词、分类、价格区间、成色、区域、排序；LIKE 通配符转义 |
| 收藏 | FR-014 | 唯一约束、幂等收藏、收藏计数 |
| 咨询会话 | FR-015、FR-016 | 基于商品的会话，仅双方可访问；封禁用户禁止发送 |
| 待确认请求 | FR-017、FR-020 | 商品不锁定、同一买家唯一、单商品上限 10 条、24 小时超时取消 |
| 卖家接受 / 拒绝 | FR-018、FR-019 | 行锁 + 唯一索引保证「同商品最多 1 条活跃订单」，其余请求自动未中选 |
| 线下交付与双确认 | FR-021 ～ FR-024 | 交付时间/区域/备注、变更留痕、双方分别确认、争议转人工 |
| 交易评价 | FR-025 | 已完成订单、每方一次、7 天窗口、唯一约束 |
| 举报 | FR-026、FR-027 | 商品/用户/消息，保存对象快照，举报人可查结果 |
| 举报处理与账户处置 | FR-028、FR-029 | 领取、成立/不成立/待补充、申诉复核、警告/限权/封禁/解封 |
| 运营统计 | FR-030 | 发布量、成交量、成交率、评价、举报、处置、分类分布与趋势 |
| 审计日志 | FR-031 | 关键操作追加式记录操作者、时间、对象、理由与前后值 |
| 站内通知 | FR-032、FR-033 | 业务事务提交后投递；失败记录 `FAILED` 不回滚核心业务 |
| 文件上传 | — | 本地存储图片，限制类型与 5MB |

---

## 三、目录结构

```
campus-secondhand-platform/
├─ backend/                      # Spring Boot 后端
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/campus/trade/
│     │  ├─ common/              # 统一响应、异常、常量、分页
│     │  ├─ config/              # Security、静态资源映射
│     │  ├─ controller/          # REST 接口（用户端 / 管理端）
│     │  ├─ dto/                 # 请求体与校验
│     │  ├─ entity/              # 数据库实体
│     │  ├─ mapper/              # MyBatis Mapper 接口
│     │  ├─ scheduler/           # 待确认请求超时任务
│     │  ├─ security/            # JWT、权限工具
│     │  ├─ service/             # 业务服务（状态机、事务）
│     │  └─ vo/                  # 响应视图对象
│     └─ resources/
│        ├─ application.yml
│        └─ mapper/*.xml         # SQL 映射
├─ frontend/                     # Vue3 + Vite 前端
│  ├─ src/api/                   # 接口封装
│  ├─ src/components/            # 通用组件
│  ├─ src/layouts/               # 主布局 / 个人中心 / 管理后台
│  ├─ src/router/                # 路由与守卫
│  ├─ src/stores/                # Pinia 用户状态
│  ├─ src/types/                 # TypeScript 类型（与后端契约一致）
│  ├─ src/utils/                 # 请求封装、字典、格式化
│  └─ src/views/                 # 页面（市场、详情、订单、后台等）
├─ database/
│  ├─ schema.sql                 # 建库建表（含生成列唯一约束）
│  └─ seed.sql                   # 演示数据与演示账号
├─ docs/
│  ├─ requirements/              # 原始需求文档
│  └─ design/                    # 接口与页面说明
└─ scripts/
   ├─ init-db.ps1                # 初始化数据库
   ├─ start-backend.ps1          # 构建并启动后端
   ├─ start-frontend.ps1         # 安装依赖并启动前端
   └─ smoke-test.mjs             # 52 项接口冒烟测试
```

---

## 四、快速开始

### 1. 环境要求

- JDK 21+（推荐 21 LTS）
- Maven 3.9+
- Node.js 20+ 与 pnpm 10+
- MySQL 8.0（服务已启动）

> 说明：项目内的 `.m2repo/`、`.pnpm-store/`、`frontend/node_modules/`、`frontend/dist/`、`backend/target/` 均为本机构建缓存或产物，已在 `.gitignore` 中排除，可按需删除；删除后首次构建需要重新联网下载依赖。

### 2. 初始化数据库

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\init-db.ps1 -Password 你的MySQL密码
```

脚本会依次执行 `database/schema.sql` 与 `database/seed.sql`，创建 `campus_secondhand` 库并写入演示数据。

也可以手动执行：

```powershell
mysql -u root -p --default-character-set=utf8mb4 < database/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < database/seed.sql
```

### 3. 启动后端（端口 8080）

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

默认使用项目内的 `.m2repo` 作为 Maven 本地仓库，避免受机器全局 `settings.xml` 影响；如需使用默认仓库可传入 `-MavenRepo "$env:USERPROFILE\.m2"`。

手动启动：

```powershell
cd backend
mvn -DskipTests package
java -jar target/campus-trade.jar
```

> 本机默认 JDK 为 24 时，直接 `mvn` 可能因 Lombok 版本不兼容而失败；请先执行 `$env:JAVA_HOME='E:\Java\jdk-21'`，或直接使用 `scripts\start-backend.cmd`（脚本会自动选择 JDK 21 与项目内 `.m2repo`）。

数据库连接可通过环境变量覆盖：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`（默认 `root` / `orange`，请按本机修改）。

### 4. 启动前端（端口 5173）

项目依赖已安装时，直接启动即可：

```powershell
cd frontend
npm run dev
```

也可以使用 pnpm（需先安装）：`pnpm dev`；或双击 `scripts\start-frontend.cmd`（脚本要求已安装 pnpm）。

打开 <http://localhost:5173>。前端通过 Vite 代理把 `/api` 与 `/uploads` 转发到 `http://localhost:8080`。

> 若执行时出现 `'node' is not recognized as an internal or external command`，说明当前终端没有读到 Node 的 PATH（例如终端在安装 Node 之前就已打开）。关闭所有终端/编辑器后重开，或手动执行：
>
> ```powershell
> $env:Path = "E:\nodejs;$env:Path"   # 换成你的 Node 安装目录
> npm run dev
> ```

### 5. 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `admin123` | 管理员 | 审核、举报处理、账户处置、统计、审计 |
| `seller01` | `123456` | 已认证卖家 | 拥有在售、锁定、已售、违规等多状态商品 |
| `buyer01` | `123456` | 已认证买家 | 有已完成订单与评价记录 |
| `buyer02` | `123456` | 已认证买家 | 有待线下交付订单与待确认请求 |
| `seller02` | `123456` | 已认证卖家 | 有待审核商品 |
| `newbie01` | `123456` | 未认证用户 | 用于验证认证拦截 |
| `banned01` | `123456` | 封禁用户 | 用于验证账户限制 |

---

## 五、测试

后端启动并初始化数据库后执行：

```powershell
node .\scripts\smoke-test.mjs
```

覆盖 52 项断言，包括：登录与权限拦截、未认证/封禁限制、关键词与组合筛选、商品详情、收藏幂等、咨询会话隔离、发布→驳回→修改→重新提交→审核通过、待确认请求不锁商品、重复请求拒绝、接受后锁定商品、重复接受拒绝、取消恢复在售、交付双确认、单方确认后取消失败、完成后商品已售、评价唯一、举报领取与处理、账户处置、通知、统计与审计、争议订单查询。

并发一致性测试（对应 NFR-02 / AC-05b）：

```powershell
node .\scripts\concurrency-test.mjs
```

模拟 10 个已认证买家对同一商品各创建 1 条待确认请求，第 11 条被上限拦截，随后卖家并发执行 10 次「接受」，验证：恰好 1 条进入待线下交付、商品锁定、其余 9 条自动取消（未中选）。本机实测 9/9 断言通过。

> 以上测试会写入业务数据，执行完成后建议重新导入 `database/seed.sql` 恢复演示数据。

前端类型检查与构建（本机实测通过）：

```powershell
cd frontend
npm run type-check   # vue-tsc，无类型错误
npm run build        # Vite 生产构建，2347 个模块
```

> 如已安装 pnpm，也可使用 `pnpm run type-check` / `pnpm run build`。

---

## 六、关键设计与规则落地

### 6.1 权限与认证

- 所有受保护接口在 Spring Security 过滤器链中校验 JWT，每次请求从数据库加载最新账户状态，封禁/限权立即生效。
- 业务服务再次校验：浏览需 `VERIFIED`；发布与下单需已认证且账户正常；管理员接口需 `ADMIN`。
- 前端路由守卫只做导航体验优化，真正的权限判断全部在服务端（NFR-04）。

### 6.2 订单一致性（BR-06 / BR-19 / NFR-02）

- 接受待确认请求时按「商品行锁 → 订单行锁」的固定顺序加锁，并在同一事务内完成：请求转待交付、商品转已锁定、其他待确认请求转已取消（未中选）并通知。
- 数据库层面使用生成列 + 唯一索引兜底：
  - `uk_order_active_item`：同一商品最多 1 条已接受且未终结的订单；
  - `uk_order_pending_buyer`：同一买家对同一商品最多 1 条待确认请求。
- 待确认请求总数上限 10 条，在商品行锁保护下计数校验。

### 6.3 交付与完成（BR-08 ～ BR-10）

- 单一确认只更新本方确认时间和订单状态，不会自动完成；
- 双方均确认后在同一事务内把订单置为已完成、商品置为已售；
- 一方确认后普通取消返回 409 并引导提交争议；
- 争议期间商品保持锁定，管理员裁决为完成或管理关闭。

### 6.4 审计与通知（BR-16 / BR-17 / FR-031 / FR-032）

- 商品审核、下架、订单状态变更、举报处理、账户处置等均写入 `audit_log`，记录操作者、时间、对象、理由与前后值。
- 通知在业务事务提交后写入；写入失败只记录 `FAILED` 通知，不影响核心业务结果。

### 6.5 统计（FR-030）

- 按时间范围聚合发布量、成交量、成交率、评价均分、举报状态分布与处置动作分布，并输出每日趋势。
- 统计接口不返回个人明细，管理端列表才提供必要业务字段（BR-15）。

---

## 七、配置项

`backend/src/main/resources/application.yml` 支持以下环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `localhost` | 数据库主机 |
| `DB_PORT` | `3306` | 数据库端口 |
| `DB_NAME` | `campus_secondhand` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | `orange` | 数据库密码（请按本机修改） |
| `APP_JWT_SECRET` | 内置开发密钥 | JWT 签名密钥，生产必须更换 |
| `APP_UPLOAD_DIR` | `./uploads` | 图片上传目录 |
| `APP_FRONTEND_ORIGIN` | `http://localhost:5173` | 允许的跨域来源（逗号分隔） |

业务参数（`app.order.timeout-hours`、`app.order.max-pending-per-item`、`app.review.window-days`）对应 SRS 中的候选值，可直接在配置中调整。

---

## 八、已知限制与待确认事项

1. 校园认证为模拟实现：校验学号格式即视为认证通过，正式部署需接入学校统一身份认证（SRS Q-01）。
2. 不提供在线支付、资金托管、退款与物流；交付与付款均为线下完成并需双方确认。
3. 图片保存在本地 `backend/uploads`，未接入对象存储；生产环境需补充文件校验、限流与备份。
4. 通知失败仅记录状态，未实现定时重试与死信队列（BR-17 待技术复核）。
5. 待确认请求 24 小时超时、评价 7 天窗口、单商品 10 条上限均为 SRS 候选值，已做成可配置参数。
6. 申诉流程后端已支持，前端暂仅在管理端提供申诉复核入口；用户侧申诉可通过接口 `POST /api/reports/{id}/appeal` 调用。
7. 统计默认按天聚合，超长区间未做预聚合优化，适用于课程规模数据。

---

## 九、相关文档

- 需求规格说明书：`docs/requirements/需求规格说明书.md`
- 环境清单（本机实测 + 从零搭建）：[docs/design/环境清单.md](docs/design/环境清单.md)
- 系统说明（架构 / 功能 / 状态 / 数据 / 部署）：[docs/design/系统说明.md](docs/design/系统说明.md)
- 系统实现说明（代码级设计）：[docs/design/系统实现说明.md](docs/design/系统实现说明.md)
- 接口与页面说明：[docs/design/前后端接口与页面说明.md](docs/design/前后端接口与页面说明.md)
- 数据库结构：`database/schema.sql`
- 演示数据：`database/seed.sql`
- 接口冒烟测试：`scripts/smoke-test.mjs`
- 并发一致性测试：`scripts/concurrency-test.mjs`
