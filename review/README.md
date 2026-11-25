# 接受待确认请求 · 独立检查环境

本目录是一次针对 `POST /api/orders/{id}/accept` 的代码检查实践产物。它不修改 `backend/` 源码，只增加隔离数据库、可重复测试和证据。

## 基线

- 基线提交：`8b593eada8b27c983545483a13b2311feddee079`
- 检查分支：`review/accept-pending-order`
- 项目原目录没有 `.git`；基线是检查开始时初始化的交付快照提交，详见 `evidence/git-baseline.txt`。

## 目录

```text
review/
├─ setup-review-db.ps1        # 用 schema.sql + seed.sql 重建隔离库 campus_secondhand_review
├─ start-review-backend.ps1   # 构建并在 18080 启动指向隔离库的后端
├─ stop-review-backend.ps1    # 停止后端（读取 .run/backend.pid）
├─ tests/accept-inspect.mjs   # 正常/权限/并发窗口/过期四组检查
├─ results/                   # 每次运行的 JSON 结果（合成数据）
└─ evidence/
   ├─ key-snippets.md         # 100 行关键代码，带路径和行号
   ├─ test-run-summary.md     # 实际执行记录、订单/商品 ID、通知与审计证据
   ├─ git-baseline.txt
   └─ environment.txt
```

## 运行

```powershell
# 1. 重建隔离库（会 DROP/重建 campus_secondhand_review 中的表）
powershell -NoProfile -ExecutionPolicy Bypass -File review\setup-review-db.ps1

# 2. 启动后端（默认 18080；需要 JDK 21、Maven、MySQL）
powershell -NoProfile -ExecutionPolicy Bypass -File review\start-review-backend.ps1

# 3. 执行检查
$env:API_BASE='http://127.0.0.1:18080/api'
node review\tests\accept-inspect.mjs

# 4. 既有回归
node scripts\concurrency-test.mjs
node scripts\smoke-test.mjs

# 5. 停止
powershell -NoProfile -ExecutionPolicy Bypass -File review\stop-review-backend.ps1
```

只跑某一组：

```powershell
$env:ONLY='normal'     # 正常接受 + 审计
$env:ONLY='permission' # 权限边界
$env:ONLY='snapshot'   # 并发窗口（会临时创建并删除测试触发器）
$env:ONLY='expired'    # 过期请求接受
```

## 合成数据

- `setup-review-db.ps1` 从 `database/schema.sql`、`database/seed.sql` 复制并替换库名，保留演示账号与 7 张订单。
- 测试脚本通过真实接口创建合成商品、注册/认证合成买家、创建待确认请求；所有标题/用户名带时间戳，重复运行不会撞唯一键。
- `snapshot` 场景会临时创建触发器 `review_sleep_order`，让新订单插入在持有商品行锁时 `SLEEP(4)`，用于稳定放大并发窗口；脚本在 `finally` 中删除触发器。这属于测试故障注入，不是应用代码。

## 说明

- 接受路径的旧快照问题依赖 MySQL 默认 `REPEATABLE-READ`；代码没有显式固定事务隔离级别。
- 隔离库可以随时删除重建；测试会写入合成数据，不影响原 `campus_secondhand` 演示库。
- 完整结论见 [代码检查报告.md](代码检查报告.md)。
