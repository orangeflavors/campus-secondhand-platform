/**
 * 校园集市 · 并发一致性测试（对应 NFR-02 / AC-05b / BR-06 / BR-19）
 *
 * 场景：
 *   1. 卖家发布并上架一件商品；
 *   2. 10 个已认证买家先后创建待确认请求（均成功，商品仍在售）；
 *   3. 第 11 个买家创建请求被拒绝（单商品待确认请求上限 10 条）；
 *   4. 卖家对 10 条请求并发执行「接受」；
 *   5. 断言：恰好 1 条成功进入待线下交付、商品被锁定、其余 9 条变为已取消（未中选）。
 *
 * 用法：node scripts/concurrency-test.mjs
 */

const BASE = process.env.API_BASE || 'http://localhost:8080/api'
const BUYER_COUNT = 10
const suffix = Date.now().toString().slice(-6)

let passed = 0
let failed = 0
const failures = []

function check(name, condition, detail = '') {
  if (condition) {
    passed += 1
    console.log(`  ✓ ${name}${detail ? '  ' + detail : ''}`)
  } else {
    failed += 1
    failures.push(name)
    console.log(`  ✗ ${name}  ${detail}`)
  }
}

async function request(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = { raw: text }
  }
  return { status: response.status, body: payload }
}

async function login(username, password) {
  const res = await request('POST', '/auth/login', { body: { username, password } })
  if (res.status !== 200) throw new Error(`登录失败 ${username}: ${JSON.stringify(res.body)}`)
  return res.body.data
}

async function main() {
  console.log('并发一致性测试（同商品 10 条请求并发接受）\n')

  const admin = await login('admin', 'admin123')
  const seller = await login('seller01', '123456')

  // 1. 准备商品
  const draft = await request('POST', '/items', {
    token: seller.token,
    body: {
      title: `并发测试商品 ${suffix}`,
      categoryId: 6,
      condition: 'GOOD',
      description: '用于验证并发接受待确认请求时的一致性，商品描述至少十个字。',
      price: 9.9,
      handoverArea: '图书馆',
      images: ['/images/item-8.svg']
    }
  })
  const itemId = draft.body.data.id
  await request('POST', `/items/${itemId}/submit`, { token: seller.token })
  const approved = await request('POST', `/admin/items/${itemId}/approve`, { token: admin.token })
  check('商品已上架', approved.body.data.status === 'ON_SALE')

  // 2. 创建 10 个已认证买家并提交请求
  const buyers = []
  for (let i = 1; i <= BUYER_COUNT; i += 1) {
    const username = `ct_${suffix}_${i}`
    const reg = await request('POST', '/auth/register', {
      body: {
        username,
        password: '123456',
        nickname: `并发买家${i}`,
        college: '计算机学院',
        studentNo: `2099${suffix}${String(i).padStart(2, '0')}`.slice(0, 12)
      }
    })
    if (reg.status !== 200) throw new Error(`注册失败 ${username}: ${JSON.stringify(reg.body)}`)
    const token = reg.body.data.token
    const auth = await request('POST', '/users/me/campus-auth', {
      token,
      body: {
        studentNo: `2099${suffix}${String(i).padStart(2, '0')}`.slice(0, 12),
        realName: `并发买家${i}`,
        college: '计算机学院'
      }
    })
    if (auth.status !== 200) throw new Error(`认证失败 ${username}: ${JSON.stringify(auth.body)}`)
    buyers.push({ token, username })
  }

  const orderIds = []
  for (const buyer of buyers) {
    const created = await request('POST', '/orders', {
      token: buyer.token,
      body: { itemId, remark: '并发测试请求' }
    })
    if (created.status === 200) orderIds.push(created.body.data.id)
  }
  check('10 条待确认请求全部创建成功', orderIds.length === BUYER_COUNT, `created=${orderIds.length}`)

  const detailAfterRequests = await request('GET', `/items/${itemId}`, { token: seller.token })
  check('待确认请求不锁定商品', detailAfterRequests.body.data.status === 'ON_SALE')

  // 3. 第 11 条请求应被拒绝
  const extra = await request('POST', '/auth/register', {
    body: {
      username: `ct_${suffix}_x`,
      password: '123456',
      nickname: '并发买家X',
      college: '计算机学院',
      studentNo: `2099${suffix}99`.slice(0, 12)
    }
  })
  const extraToken = extra.body.data.token
  await request('POST', '/users/me/campus-auth', {
    token: extraToken,
    body: { studentNo: `2099${suffix}99`.slice(0, 12), realName: '并发买家X', college: '计算机学院' }
  })
  const eleventh = await request('POST', '/orders', { token: extraToken, body: { itemId } })
  check('第 11 条待确认请求被拒绝', eleventh.status === 409, `status=${eleventh.status}`)

  // 4. 并发接受
  const results = await Promise.all(
    orderIds.map((orderId) =>
      request('POST', `/orders/${orderId}/accept`, { token: seller.token }).then((res) => ({ orderId, res }))
    )
  )
  const successes = results.filter((item) => item.res.status === 200)
  check('并发接受只有 1 条成功', successes.length === 1, `success=${successes.length}`)

  const detailAfterAccept = await request('GET', `/items/${itemId}`, { token: seller.token })
  check('商品被锁定为 LOCKED', detailAfterAccept.body.data.status === 'LOCKED')

  // 5. 校验全部请求状态
  const sellerOrders = await request('GET', '/orders?role=SELLER', { token: seller.token })
  const related = sellerOrders.body.data.filter((order) => order.item.id === itemId)
  const delivering = related.filter((order) => order.status === 'DELIVERING')
  const cancelled = related.filter((order) => order.status === 'CANCELLED')
  check('恰好 1 条订单进入待线下交付', delivering.length === 1, `delivering=${delivering.length}`)
  check('其余 9 条订单已取消（未中选）', cancelled.length === BUYER_COUNT - 1, `cancelled=${cancelled.length}`)
  check(
    '取消类型均为 NOT_SELECTED',
    cancelled.every((order) => order.cancelType === 'NOT_SELECTED')
  )

  console.log('\n----------------------------------------')
  console.log(`通过 ${passed} 项，失败 ${failed} 项`)
  if (failed > 0) {
    console.log('失败项：', failures.join('、'))
    process.exitCode = 1
  } else {
    console.log('并发一致性测试全部通过 ✅')
  }
}

main().catch((error) => {
  console.error('并发测试异常终止：', error)
  process.exitCode = 1
})
