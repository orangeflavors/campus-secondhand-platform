/**
 * 校园集市 · 后端接口冒烟测试
 *
 * 用法：
 *   1. 启动后端（http://localhost:8080）
 *   2. 执行数据库初始化：database/schema.sql + database/seed.sql
 *   3. node scripts/smoke-test.mjs
 *
 * 说明：脚本会创建商品、订单、评价、举报与处置等数据，运行结束后建议重新导入 seed.sql 恢复演示数据。
 */

const BASE = process.env.API_BASE || 'http://localhost:8080/api'

let passed = 0
let failed = 0
const failures = []

function ok(name, detail = '') {
  passed += 1
  console.log(`  ✓ ${name}${detail ? '  ' + detail : ''}`)
}

function fail(name, detail = '') {
  failed += 1
  failures.push(`${name} ${detail}`)
  console.log(`  ✗ ${name}  ${detail}`)
}

function assert(name, condition, detail = '') {
  if (condition) ok(name, detail)
  else fail(name, detail)
}

async function request(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  let payload = null
  const text = await response.text()
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = { raw: text }
  }
  return { status: response.status, body: payload }
}

async function login(username, password) {
  const res = await request('POST', '/auth/login', { body: { username, password } })
  if (res.status !== 200 || res.body?.code !== 0) {
    throw new Error(`登录失败 ${username}: ${res.status} ${JSON.stringify(res.body)}`)
  }
  return res.body.data
}

function section(title) {
  console.log(`\n[${title}]`)
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function main() {
  console.log('校园集市接口冒烟测试')
  console.log(`API: ${BASE}\n`)

  section('1. 认证与权限')
  const admin = await login('admin', 'admin123')
  const seller01 = await login('seller01', '123456')
  const seller02 = await login('seller02', '123456')
  const buyer01 = await login('buyer01', '123456')
  const buyer02 = await login('buyer02', '123456')
  const newbie = await login('newbie01', '123456')
  const banned = await login('banned01', '123456')
  assert('管理员登录成功', admin.user.role === 'ADMIN')
  assert('卖家登录成功', seller01.user.authStatus === 'VERIFIED')

  const badLogin = await request('POST', '/auth/login', { body: { username: 'seller01', password: 'wrong' } })
  assert('错误密码被拒绝', badLogin.status === 401 && badLogin.body.code !== 0)

  const me = await request('GET', '/auth/me', { token: buyer01.token })
  assert('获取当前用户信息', me.status === 200 && me.body.data.username === 'buyer01')

  const unverifiedBrowse = await request('GET', '/items', { token: newbie.token })
  assert('未认证用户不能浏览商品', unverifiedBrowse.status === 403 && unverifiedBrowse.body.code === 40301)

  const buyerAdmin = await request('GET', '/admin/stats', { token: buyer01.token })
  assert('普通用户不能访问管理端', buyerAdmin.status === 403)

  section('2. 商品搜索与详情')
  const search = await request('GET', '/items?keyword=%E6%95%99%E6%9D%90', { token: buyer01.token })
  assert('关键词搜索返回结果', search.status === 200 && search.body.data.total >= 1,
    `total=${search.body.data?.total}`)
  assert('搜索结果均为在售商品', (search.body.data.records || []).every((item) => item.status === 'ON_SALE'))

  const filter = await request('GET', '/items?categoryId=2&minPrice=100&maxPrice=500', { token: buyer01.token })
  assert('组合筛选生效', filter.status === 200 &&
    (filter.body.data.records || []).every((item) => item.categoryId === 2 && item.price >= 100 && item.price <= 500))

  const detail = await request('GET', '/items/1', { token: buyer01.token })
  assert('商品详情包含图片与卖家资料', detail.status === 200 &&
    Array.isArray(detail.body.data.images) && detail.body.data.images.length > 0 &&
    detail.body.data.seller?.authStatus === 'VERIFIED')

  const selfDetail = await request('GET', '/items/1', { token: seller01.token })
  assert('本人查看详情可看到编辑能力', selfDetail.body.data.isOwner === true)

  section('3. 校园认证')
  const authAgain = await request('POST', '/users/me/campus-auth', {
    token: seller01.token,
    body: { studentNo: '2021000101', realName: '林小满', college: '计算机学院' }
  })
  assert('重复认证被拒绝', authAgain.status === 409)

  section('4. 收藏与咨询')
  const fav = await request('POST', '/items/4/favorite', { token: buyer01.token })
  assert('收藏商品成功', fav.status === 200 && fav.body.data.favorited === true)
  const favAgain = await request('POST', '/items/4/favorite', { token: buyer01.token })
  assert('重复收藏不产生重复计数', favAgain.body.data.count === fav.body.data.count)
  const favList = await request('GET', '/me/favorites', { token: buyer01.token })
  assert('收藏列表包含商品', favList.body.data.records.some((item) => item.id === 4))

  const conversation = await request('POST', '/conversations', { token: buyer01.token, body: { itemId: 4 } })
  assert('创建咨询会话', conversation.status === 200 && conversation.body.data.id)
  const conversationId = conversation.body.data.id
  const message = await request('POST', `/conversations/${conversationId}/messages`, {
    token: buyer01.token,
    body: { content: '你好，球拍还在吗？' }
  })
  assert('发送咨询消息', message.status === 200 && message.body.data.mine === true)
  const messages = await request('GET', `/conversations/${conversationId}/messages`, { token: seller02.token })
  assert('对方可以读取消息', messages.status === 200 && messages.body.data.length >= 1)
  const outsider = await request('GET', `/conversations/${conversationId}/messages`, { token: buyer02.token })
  assert('非会话方不能读取消息', outsider.status === 403)

  section('5. 发布、驳回、重新提交与审核')
  const draft = await request('POST', '/items', {
    token: seller01.token,
    body: {
      title: '冒烟测试用二手教材',
      categoryId: 1,
      condition: 'GOOD',
      description: '这是一条用于冒烟测试的商品描述，至少十个字符。',
      price: 12.5,
      handoverArea: '图书馆',
      images: ['/images/item-1.svg']
    }
  })
  assert('创建商品草稿', draft.status === 200 && draft.body.data.status === 'DRAFT')
  const newItemId = draft.body.data.id

  const submitted = await request('POST', `/items/${newItemId}/submit`, { token: seller01.token })
  assert('提交审核后进入待审核', submitted.body.data.status === 'PENDING')

  const rejected = await request('POST', `/admin/items/${newItemId}/reject`, {
    token: admin.token,
    body: { reason: '图片信息不足，请补充实物照片' }
  })
  assert('管理员驳回归档', rejected.status === 200 && rejected.body.data.status === 'REJECTED' &&
    rejected.body.data.rejectReason?.length > 0)

  const edited = await request('PUT', `/items/${newItemId}`, {
    token: seller01.token,
    body: {
      title: '冒烟测试用二手教材（补充图片）',
      categoryId: 1,
      condition: 'GOOD',
      description: '这是一条用于冒烟测试的商品描述，已经补充了清晰的图片。',
      price: 12.5,
      handoverArea: '图书馆',
      images: ['/images/item-1.svg', '/images/item-2.svg']
    }
  })
  assert('驳回后修改回到草稿', edited.body.data.status === 'DRAFT')

  await request('POST', `/items/${newItemId}/submit`, { token: seller01.token })
  const approved = await request('POST', `/admin/items/${newItemId}/approve`, { token: admin.token })
  assert('审核通过后上架', approved.status === 200 && approved.body.data.status === 'ON_SALE')

  const wrongOwner = await request('PUT', `/items/${newItemId}`, {
    token: buyer01.token,
    body: {
      title: '越权修改测试', categoryId: 1, condition: 'GOOD',
      description: '试图越权修改他人商品的描述内容。', price: 1, handoverArea: '图书馆',
      images: ['/images/item-1.svg']
    }
  })
  assert('非本人不能编辑商品', wrongOwner.status === 403)

  section('6. 待确认请求与接受并发约束')
  const orderRes = await request('POST', '/orders', {
    token: buyer01.token,
    body: { itemId: 4, remark: '冒烟测试订单' }
  })
  assert('创建待确认请求', orderRes.status === 200 && orderRes.body.data.status === 'PENDING_SELLER')
  const orderId = orderRes.body.data.id

  const itemAfterRequest = await request('GET', '/items/4', { token: seller02.token })
  assert('待确认请求不锁定商品', itemAfterRequest.body.data.status === 'ON_SALE')

  const duplicate = await request('POST', '/orders', { token: buyer01.token, body: { itemId: 4 } })
  assert('同一买家重复请求被拒绝', duplicate.status === 409)

  const accepted = await request('POST', `/orders/${orderId}/accept`, { token: seller02.token })
  assert('卖家接受请求', accepted.status === 200 && accepted.body.data.status === 'DELIVERING')

  const itemLocked = await request('GET', '/items/4', { token: seller02.token })
  assert('接受后商品被锁定', itemLocked.body.data.status === 'LOCKED')

  const acceptAgain = await request('POST', `/orders/${orderId}/accept`, { token: seller02.token })
  assert('重复接受被拒绝', acceptAgain.status === 409)

  const cancelAfterAccept = await request('POST', `/orders/${orderId}/cancel`, {
    token: buyer01.token,
    body: { reason: '冒烟测试尝试取消' }
  })
  assert('待交付无人确认时可以取消', cancelAfterAccept.status === 200)
  const itemRestored = await request('GET', '/items/4', { token: seller02.token })
  assert('取消后商品恢复在售', itemRestored.body.data.status === 'ON_SALE')

  section('7. 交付双确认、完成与评价')
  const order2 = await request('POST', '/orders', { token: buyer01.token, body: { itemId: 4 } })
  const order2Id = order2.body.data.id
  await request('POST', `/orders/${order2Id}/accept`, { token: seller02.token })

  const handover = await request('PUT', `/orders/${order2Id}/handover`, {
    token: buyer01.token,
    body: { handoverTime: new Date(Date.now() + 86400000).toISOString().slice(0, 19), area: '体育馆', note: '冒烟测试交付备注' }
  })
  assert('更新交付约定', handover.status === 200 && handover.body.data.handover?.area === '体育馆')

  const buyerConfirm = await request('POST', `/orders/${order2Id}/confirm`, { token: buyer01.token })
  assert('买家单方确认不完成订单', buyerConfirm.body.data.status === 'BUYER_CONFIRMED')

  const cancelAfterConfirm = await request('POST', `/orders/${order2Id}/cancel`, {
    token: buyer01.token,
    body: { reason: '单方确认后尝试取消' }
  })
  assert('单方确认后普通取消失败', cancelAfterConfirm.status === 409)

  const sellerConfirm = await request('POST', `/orders/${order2Id}/confirm`, { token: seller02.token })
  assert('双方确认后订单完成', sellerConfirm.body.data.status === 'COMPLETED')

  const itemSold = await request('GET', '/items/4', { token: seller02.token })
  assert('对应商品标记为已售', itemSold.body.data.status === 'SOLD')

  const review = await request('POST', `/orders/${order2Id}/reviews`, {
    token: buyer01.token,
    body: { rating: 5, content: '冒烟测试评价：交易顺利' }
  })
  assert('已完成订单可以评价', review.status === 200 && review.body.data.rating === 5)

  const reviewAgain = await request('POST', `/orders/${order2Id}/reviews`, {
    token: buyer01.token,
    body: { rating: 4, content: '重复评价' }
  })
  assert('重复评价被拒绝', reviewAgain.status === 409)

  const outsiderReview = await request('POST', `/orders/${order2Id}/reviews`, {
    token: buyer02.token,
    body: { rating: 5, content: '非订单方评价' }
  })
  assert('非订单方不能评价', outsiderReview.status === 403)

  section('8. 举报与管理员处置')
  const report = await request('POST', '/reports', {
    token: buyer02.token,
    body: { targetType: 'ITEM', targetId: 6, category: 'FAKE', description: '冒烟测试举报：商品描述与实物可能不符' }
  })
  assert('提交举报', report.status === 200 && report.body.data.reportNo)
  const reportId = report.body.data.id

  const claim = await request('POST', `/admin/reports/${reportId}/claim`, { token: admin.token })
  assert('管理员领取举报', claim.status === 200 && claim.body.data.status === 'PROCESSING')

  const handled = await request('POST', `/admin/reports/${reportId}/handle`, {
    token: admin.token,
    body: { result: 'REJECTED', ruleBasis: 'BR-12', reason: '冒烟测试：证据不足，举报不成立' }
  })
  assert('管理员处理举报', handled.status === 200 && handled.body.data.status === 'REJECTED')

  const mineReports = await request('GET', '/reports/mine', { token: buyer02.token })
  assert('举报人可查看本人举报', mineReports.body.data.records.some((item) => item.id === reportId))

  section('9. 账户处置与通知')
  const sanction = await request('POST', `/admin/users/${buyer02.user.id}/sanction`, {
    token: admin.token,
    body: { actionType: 'WARN', reason: '冒烟测试：提醒遵守交易规范', ruleBasis: 'BR-01', durationDays: null }
  })
  assert('管理员可对账户执行警告', sanction.status === 200 && sanction.body.data.actionType === 'WARN')

  const unread = await request('GET', '/notifications/unread-count', { token: seller01.token })
  assert('通知未读数可查询', unread.status === 200 && typeof unread.body.data.count === 'number')

  const notifications = await request('GET', '/notifications?page=1&size=5', { token: seller01.token })
  assert('通知列表可查询', notifications.status === 200 && notifications.body.data.records.length > 0)

  section('10. 统计与审计')
  const stats = await request('GET', '/admin/stats', { token: admin.token })
  assert('运营统计可查询', stats.status === 200 && stats.body.data.items &&
    Array.isArray(stats.body.data.trend) && stats.body.data.trend.length > 0,
    `published=${stats.body.data.items?.published}, completed=${stats.body.data.orders?.completed}`)

  const audit = await request('GET', '/admin/audit-logs?page=1&size=10', { token: admin.token })
  assert('审计日志可查询', audit.status === 200 && audit.body.data.records.length > 0)

  const disputes = await request('GET', '/admin/orders', { token: admin.token })
  assert('争议订单列表可查询', disputes.status === 200 && Array.isArray(disputes.body.data.records))

  section('11. 封禁与未认证限制')
  const bannedOrder = await request('POST', '/orders', { token: banned.token, body: { itemId: 1 } })
  assert('封禁用户不能创建订单', bannedOrder.status === 403)

  console.log('\n----------------------------------------')
  console.log(`通过 ${passed} 项，失败 ${failed} 项`)
  if (failed > 0) {
    console.log('失败项：')
    failures.forEach((item) => console.log(`  - ${item}`))
    process.exitCode = 1
  } else {
    console.log('全部冒烟测试通过 ✅')
  }
}

main().catch((error) => {
  console.error('\n冒烟测试异常终止：', error)
  process.exitCode = 1
})
