# 实际执行记录（2026-11-25）

## 基线与环境

| 项 | 值 |
|---|---|
| 基线提交 | `8b593eada8b27c983545483a13b2311feddee079` |
| 检查分支 | `review/accept-pending-order` |
| 被检查对象 | `OrderController.accept` → `OrderService.accept`；`OrderMapper.findOtherPending/update`；`trade_order` 生成列唯一索引 |
| 后端 | Spring Boot 3.3.5 / Java 21.0.8，`http://127.0.0.1:18080` |
| 数据库 | MySQL 8.0.46，隔离库 `campus_secondhand_review`，`@@transaction_isolation=REPEATABLE-READ` |
| 测试工具 | Node.js 26.13.0（全局 `fetch`）+ 本机 `mysql.exe` |

隔离库由 `review/setup-review-db.ps1` 从 `database/schema.sql`、`database/seed.sql` 替换库名后重建，不修改原库。初始为 7 个用户、12 个商品、7 个订单；测试脚本再通过真实接口创建合成商品、买家与请求。

## 测试命令

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File review\setup-review-db.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File review\start-review-backend.ps1 -SkipBuild
$env:API_BASE='http://127.0.0.1:18080/api'

node review\tests\accept-inspect.mjs              # 全部检查
node scripts\concurrency-test.mjs                 # 既有 10 并发测试
node scripts\smoke-test.mjs                       # 既有 52 项冒烟测试
```

## 结果总览

| 测试 | 结果 | 说明 |
|---|---|---|
| 既有冒烟测试 | 52/52 通过 | 正常接受、权限、重复接受等主路径未回归 |
| 既有 10 并发接受测试 | 9/9 通过 | 10 条请求预先存在时，恰好 1 条成功、9 条未中选 |
| 接受主路径 + 审计检查 | 6/7 通过 | 状态/通知正确；自动未中选缺少审计 |
| 权限边界 | 6/6 通过 | 非卖家、未认证、封禁、管理员均被拒绝；重复接受 409 |
| 并发窗口（新增） | 3/4 通过 | **复现旧快照漏取消**：新请求 109 仍为 `PENDING_SELLER` |
| 过期请求接受（新增） | 2/2 通过 | 过期请求仍返回 200 并进入 `DELIVERING` |
| 全量连续运行 | 17/19 通过 | 失败项正是上述 F1/F2；同一轮订单 127/128 再次复现 F1 |

结果 JSON 见 `review/results/`；`review/results/smoke-review.log` 是冒烟测试完整输出（运行期日志，未纳入 Git）。

## 关键证据

### 1. 并发窗口漏取消（已复现）

测试用真实接口创建商品 104，买家 3（A）与买家 4（B）先后提交待确认请求。为稳定放大窗口，测试临时在隔离库上创建触发器，让 B 的 `INSERT trade_order` 在持有商品行锁时 `SLEEP(4)`；A 的接受请求在 B 提交前已读取旧快照并等待商品锁。触发器在 `finally` 中删除，不修改应用代码。

结果：

```text
order 108 (A): DELIVERING, item 104: LOCKED
order 109 (B): PENDING_SELLER, cancel_type NULL, item 104: LOCKED
```

- B 的请求创建接口返回 200，但未被 A 的接受事务取消。
- B 再次接受返回 `409 / 40901 商品已被其他请求锁定或不在售，无法接受`。
- 通知表中 A（108）有“卖家已接受你的请求”；B（109）只有卖家侧的“收到新的待确认请求”，没有“待确认请求未中选”。
- 审计表中 B（109）只有 `ORDER_CREATE`，没有状态变更审计。

结果 JSON 为 `review/results/accept-inspect-244598.json`。全量连续运行（`accept-inspect-565776.json`）在商品 110 上再次复现：订单 127 `DELIVERING`、128 `PENDING_SELLER`、商品 `LOCKED`。

### 2. 自动未中选缺少审计（已复现）

正常接受场景（订单 103 接受，订单 104/105 自动未中选）：

```text
audit_log:
  ORDER_CREATE  103
  ORDER_CREATE  104
  ORDER_CREATE  105
  ORDER_ACCEPT  103
```

104/105 的 `PENDING_SELLER → CANCELLED/NOT_SELECTED` 没有对应审计行；接受订单只有一条 `ORDER_ACCEPT`。通知存在，审计缺失。

### 3. 过期请求仍可接受（已复现）

订单 110 的 `expire_at` 被置为当前时间之前 1 分钟，接受接口仍返回 200，订单进入 `DELIVERING`。随后测试用买家取消清理，审计表留下：

```text
ORDER_ACCEPT  110  PENDING_SELLER → DELIVERING
ORDER_CANCEL  110  DELIVERING     → CANCELLED
```

该行为依赖 60 秒一次的定时任务；在任务运行前（或任务停摆时）过期请求仍可被接受。

## 可复现性说明

- 并发窗口测试依赖 MySQL 默认 `REPEATABLE-READ`。若目标环境把事务隔离级别改为 `READ-COMMITTED`，该现象可能不再出现；代码没有显式固定隔离级别。
- 触发器只是把既有窗口从毫秒级放大到 4 秒，不改变 `OrderService.accept` 的 SQL 或事务语义。
- 隔离库可随时重建；测试数据均为合成数据，演示账号密码来自 `database/seed.sql`。
