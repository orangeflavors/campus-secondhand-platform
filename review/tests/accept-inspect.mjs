/**
 * 接受待确认请求（OrderService.accept）检查用测试。
 *
 * 前置：review/setup-review-db.ps1 已执行，review/start-review-backend.ps1 已启动 18080。
 * 运行：node review/tests/accept-inspect.mjs
 *
 * 该脚本会写入隔离库 campus_secondhand_review，并直接使用本机 mysql CLI 做
 * 数据核对与故障注入（触发器延迟插入），不修改被检查源码。
 */
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const API = process.env.API_BASE || 'http://127.0.0.1:18080/api'
const MYSQL = process.env.MYSQL_BIN || 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe'
const DB = process.env.REVIEW_DB || 'campus_secondhand_review'
const ONLY = process.env.ONLY || 'all'
const stamp = Date.now().toString().slice(-6)

const results = []
let sqlSeq = 0

function sql(query) {
  const out = path.join(os.tmpdir(), `campus-review-sql-${process.pid}-${sqlSeq++}.tsv`)
  const fd = fs.openSync(out, 'w')
  const args = [
    '-h', '127.0.0.1', '-P', '3306', '-u', process.env.DB_USERNAME || 'root',
    '--default-character-set=utf8mb4',
    '-N', '-B', '-D', DB, '-e', query
  ]
  const run = spawnSync(MYSQL, args, {
    stdio: ['ignore', fd, 'inherit'],
    env: { ...process.env, MYSQL_PWD: process.env.DB_PASSWORD || 'orange' }
  })
  fs.closeSync(fd)
  const text = fs.readFileSync(out, 'utf8')
  fs.unlinkSync(out)
  if (run.status !== 0) throw new Error(`mysql 执行失败: ${query}`)
  return text.trim() ? text.trim().split(/\r?\n/).map((line) => line.split('\t')) : []
}

function record(name, pass, detail) {
  results.push({ name, pass, detail })
  console.log(`${pass ? '  ✓' : '  ✗'} ${name}${detail ? `  ${detail}` : ''}`)
}

async function request(method, route, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${API}${route}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let payload = null
  try { payload = text ? JSON.parse(text) : null } catch { payload = { raw: text } }
  return { status: res.status, body: payload }
}

async function login(username, password) {
  const res = await request('POST', '/auth/login', { body: { username, password } })
  if (res.status !== 200) throw new Error(`登录失败 ${username}: ${JSON.stringify(res.body)}`)
  return res.body.data
}

async function createItem(title) {
  const draft = await request('POST', '/items', {
    token: seller.token,
    body: {
      title,
      categoryId: 6,
      condition: 'GOOD',
      description: '接受待确认请求检查用合成商品，描述长度满足校验要求。',
      price: 12.5,
      handoverArea: '图书馆',
      images: ['/images/item-8.svg']
    }
  })
  if (draft.status !== 200) throw new Error(`创建商品失败: ${JSON.stringify(draft.body)}`)
  const id = draft.body.data.id
  const submitted = await request('POST', `/items/${id}/submit`, { token: seller.token })
  if (submitted.status !== 200) throw new Error(`提交商品失败: ${JSON.stringify(submitted.body)}`)
  const approved = await request('POST', `/admin/items/${id}/approve`, { token: admin.token })
  if (approved.status !== 200) throw new Error(`审核商品失败: ${JSON.stringify(approved.body)}`)
  return id
}

async function createOrder(token, itemId, remark = '检查用待确认请求') {
  const res = await request('POST', '/orders', { token, body: { itemId, remark } })
  if (res.status !== 200) throw new Error(`创建请求失败: ${JSON.stringify(res.body)}`)
  return res.body.data.id
}

async function ordersOf(itemId) {
  const res = await request('GET', '/orders?role=SELLER', { token: seller.token })
  if (res.status !== 200) throw new Error(`查询订单失败: ${JSON.stringify(res.body)}`)
  return res.body.data.filter((order) => order.item?.id === itemId)
}

async function notificationsOf(token, size = 30) {
  const res = await request('GET', `/notifications?page=1&size=${size}`, { token })
  if (res.status !== 200) throw new Error(`查询通知失败: ${JSON.stringify(res.body)}`)
  return res.body.data.records
}

async function registerBuyer(suffix) {
  const username = `rv_${stamp}_${suffix}`
  const studentNo = `30${stamp}${String(suffix).padStart(2, '0')}`.slice(0, 12)
  const reg = await request('POST', '/auth/register', {
    body: {
      username, password: '123456', nickname: `检查买家${suffix}`,
      college: '计算机学院', studentNo
    }
  })
  if (reg.status !== 200) throw new Error(`注册失败: ${JSON.stringify(reg.body)}`)
  const token = reg.body.data.token
  const auth = await request('POST', '/users/me/campus-auth', {
    token, body: { studentNo, realName: `检查买家${suffix}`, college: '计算机学院' }
  })
  if (auth.status !== 200) throw new Error(`认证失败: ${JSON.stringify(auth.body)}`)
  return { token, username, id: reg.body.data.user.id }
}

async function scenarioNormal() {
  console.log('\n[1] 正常接受：原子性、未中选取消、通知与审计')
  const itemId = await createItem(`正常接受检查 ${stamp}`)
  const a = await createOrder(buyer01.token, itemId)
  const b = await createOrder(buyer02.token, itemId)
  const c = await createOrder(extra.token, itemId)

  const accepted = await request('POST', `/orders/${a}/accept`, { token: seller.token })
  record('卖家接受成功并进入待线下交付', accepted.status === 200 && accepted.body.data?.status === 'DELIVERING',
    `http=${accepted.status}, status=${accepted.body.data?.status}`)

  const item = await request('GET', `/items/${itemId}`, { token: seller.token })
  record('商品同步锁定为 LOCKED', item.body.data?.status === 'LOCKED', `status=${item.body.data?.status}`)

  const related = await ordersOf(itemId)
  const byId = new Map(related.map((order) => [order.id, order]))
  record('被接受请求为 DELIVERING', byId.get(a)?.status === 'DELIVERING', `status=${byId.get(a)?.status}`)
  record('其他请求均取消且类型为 NOT_SELECTED',
    [b, c].every((id) => byId.get(id)?.status === 'CANCELLED' && byId.get(id)?.cancelType === 'NOT_SELECTED'),
    `b=${byId.get(b)?.status}/${byId.get(b)?.cancelType}, c=${byId.get(c)?.status}/${byId.get(c)?.cancelType}`)

  const nb = await notificationsOf(buyer02.token)
  const nc = await notificationsOf(extra.token)
  record('未中选买家的站内通知已生成',
    nb.some((n) => n.relatedType === 'ORDER' && n.relatedId === b && n.title.includes('未中选')) &&
    nc.some((n) => n.relatedType === 'ORDER' && n.relatedId === c && n.title.includes('未中选')),
    `b=${nb.filter((n) => n.relatedId === b).length}, c=${nc.filter((n) => n.relatedId === c).length}`)

  const audit = sql(`SELECT action, target_id FROM audit_log WHERE target_type='ORDER' AND target_id IN (${a},${b},${c}) ORDER BY id`)
  const bAudit = audit.filter((row) => row[1] === String(b) && row[0] !== 'ORDER_CREATE').length
  const cAudit = audit.filter((row) => row[1] === String(c) && row[0] !== 'ORDER_CREATE').length
  record('被接受订单写入 ORDER_ACCEPT 审计', audit.some((row) => row[0] === 'ORDER_ACCEPT' && row[1] === String(a)),
    `rows=${JSON.stringify(audit)}`)
  record('自动未中选的订单也应有状态变更审计（BR-16）', bAudit + cAudit > 0,
    `b_noncreate_audit=${bAudit}, c_noncreate_audit=${cAudit}`)
}

async function scenarioPermission() {
  console.log('\n[2] 权限与状态边界')
  const itemId = await createItem(`权限检查 ${stamp}`)
  const orderId = await createOrder(buyer01.token, itemId)

  const outsider = await request('POST', `/orders/${orderId}/accept`, { token: buyer02.token })
  record('非订单卖家不能接受', outsider.status === 403, `http=${outsider.status}, code=${outsider.body?.code}`)

  const unverified = await request('POST', `/orders/${orderId}/accept`, { token: newbie.token })
  record('未认证用户不能接受', unverified.status === 403 && unverified.body?.code === 40301,
    `http=${unverified.status}, code=${unverified.body?.code}`)

  const bannedRes = await request('POST', `/orders/${orderId}/accept`, { token: banned.token })
  record('封禁账号不能接受', bannedRes.status === 403 && bannedRes.body?.code === 40302,
    `http=${bannedRes.status}, code=${bannedRes.body?.code}`)

  const adminAccept = await request('POST', `/orders/${orderId}/accept`, { token: admin.token })
  record('管理员不能代替普通卖家接受', adminAccept.status === 403, `http=${adminAccept.status}`)

  const sellerAccept = await request('POST', `/orders/${orderId}/accept`, { token: seller.token })
  record('订单卖家接受成功', sellerAccept.status === 200 && sellerAccept.body.data?.status === 'DELIVERING',
    `http=${sellerAccept.status}`)

  const again = await request('POST', `/orders/${orderId}/accept`, { token: seller.token })
  record('重复接受返回 409 且状态不变', again.status === 409 && again.body?.code === 40901,
    `http=${again.status}, code=${again.body?.code}`)
}

async function scenarioSnapshotRace() {
  console.log('\n[3] 并发窗口：接受事务的旧快照是否漏掉新提交的待确认请求')
  const itemId = await createItem(`快照竞态检查 ${stamp}`)
  const a = await createOrder(buyer01.token, itemId)

  sql('DROP TRIGGER IF EXISTS review_sleep_order')
  // 仅用于测试的故障注入：让新请求 INSERT 在持有商品行锁时停 4 秒，
  // 从而稳定地把“接受事务先读旧快照、后拿商品锁”的窗口放大。
  sql('CREATE TRIGGER review_sleep_order BEFORE INSERT ON trade_order FOR EACH ROW SET @review_sleep = SLEEP(4)')
  try {
    const bPromise = request('POST', '/orders', { token: buyer02.token, body: { itemId, remark: '并发窗口中的新请求' } })
    await new Promise((resolve) => setTimeout(resolve, 1000))
    const acceptPromise = request('POST', `/orders/${a}/accept`, { token: seller.token })
    const [bRes, aRes] = await Promise.all([bPromise, acceptPromise])
    const bId = bRes.body?.data?.id

    record('并发新请求创建成功', bRes.status === 200 && Boolean(bId), `http=${bRes.status}, id=${bId}`)
    record('接受请求成功', aRes.status === 200 && aRes.body.data?.status === 'DELIVERING', `http=${aRes.status}`)

    const rows = sql(`SELECT id,status,cancel_type FROM trade_order WHERE id IN (${a},${bId}) ORDER BY id`)
    const byId = new Map(rows.map((row) => [row[0], { status: row[1], cancelType: row[2] }]))
    record('新请求被接受事务取消为 NOT_SELECTED（FR-018/BR-19）',
      byId.get(String(bId))?.status === 'CANCELLED' && byId.get(String(bId))?.cancelType === 'NOT_SELECTED',
      `a=${JSON.stringify(byId.get(String(a)))}, b=${JSON.stringify(byId.get(String(bId)))}`)

    const item = sql(`SELECT status FROM item WHERE id=${itemId}`)
    record('商品仍锁定且仅一条活跃订单',
      item[0]?.[0] === 'LOCKED' && sql(`SELECT COUNT(*) FROM trade_order WHERE item_id=${itemId} AND status IN ('DELIVERING','BUYER_CONFIRMED','SELLER_CONFIRMED','DISPUTE')`)[0][0] === '1',
      `item=${item[0]?.[0]}`)
  } finally {
    sql('DROP TRIGGER IF EXISTS review_sleep_order')
  }
}

async function scenarioExpired() {
  console.log('\n[4] 过期请求在接受时是否仍被检查')
  let accepted = null
  let itemId = null
  let orderId = null
  for (let attempt = 0; attempt < 3 && !accepted; attempt += 1) {
    itemId = await createItem(`过期检查 ${stamp}-${attempt}`)
    orderId = await createOrder(buyer01.token, itemId)
    sql(`UPDATE trade_order SET expire_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id=${orderId}`)
    const res = await request('POST', `/orders/${orderId}/accept`, { token: seller.token })
    if (res.status === 200) accepted = res
    else console.log(`  · 第 ${attempt + 1} 次被定时任务抢先取消（http=${res.status}），重试`)
  }
  record('过期请求仍可被接受（接受路径未校验 expire_at）', accepted !== null,
    accepted ? `order=${orderId}, expire_at 已早于当前时间` : '三次均被定时任务取消')
  if (accepted) {
    const rows = sql(`SELECT status, expire_at < NOW() FROM trade_order WHERE id=${orderId}`)
    record('接受后订单进入 DELIVERING 且过期时间仍为过去', rows[0]?.[0] === 'DELIVERING' && rows[0]?.[1] === '1',
      `status=${rows[0]?.[0]}, expired=${rows[0]?.[1]}`)
    // 清理：待交付且无人确认，买家可以取消并恢复商品。
    await request('POST', `/orders/${orderId}/cancel`, { token: buyer01.token, body: { reason: '检查结束清理' } })
  }
}

async function main() {
  console.log(`检查目标：${API}（数据库 ${DB}）`)
  admin = await login('admin', 'admin123')
  seller = await login('seller01', '123456')
  buyer01 = await login('buyer01', '123456')
  buyer02 = await login('buyer02', '123456')
  newbie = await login('newbie01', '123456')
  banned = await login('banned01', '123456')
  extra = await registerBuyer('03')

  if (ONLY === 'all' || ONLY === 'normal') await scenarioNormal()
  if (ONLY === 'all' || ONLY === 'permission') await scenarioPermission()
  if (ONLY === 'all' || ONLY === 'snapshot') await scenarioSnapshotRace()
  if (ONLY === 'all' || ONLY === 'expired') await scenarioExpired()

  const failed = results.filter((r) => !r.pass)
  console.log(`\n结果：通过 ${results.length - failed.length} 项，失败 ${failed.length} 项`)
  if (failed.length) console.log(`失败项：${failed.map((r) => r.name).join('、')}`)

  const outDir = path.resolve('review/results')
  fs.mkdirSync(outDir, { recursive: true })
  fs.writeFileSync(path.join(outDir, `accept-inspect-${stamp}.json`), JSON.stringify({
    api: API, database: DB, stamp, results
  }, null, 2))
  if (failed.length) process.exitCode = 1
}

let admin, seller, buyer01, buyer02, newbie, banned, extra
main().catch((error) => {
  console.error('检查脚本异常终止：', error)
  process.exitCode = 1
})
