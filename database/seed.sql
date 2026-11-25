-- ============================================================================
-- 校园二手交易与物品流转平台 · 演示种子数据
-- 依赖：先执行 schema.sql
-- 演示账号（所有普通账号密码均为 123456）：
--   admin / admin123          管理员（已认证）
--   seller01 / 123456         已认证卖家（林小满）
--   buyer01 / 123456          已认证买家（陈一诺）
--   buyer02 / 123456          已认证买家（赵子航）
--   newbie01 / 123456         未认证新用户
--   banned01 / 123456         被封禁用户
-- ============================================================================

USE campus_secondhand;
SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- 用户（BCrypt 密码哈希）
-- ----------------------------------------------------------------------------
INSERT INTO sys_user
(id, username, password_hash, nickname, phone, email, real_name, student_no, college, avatar_url,
 contact_visibility, role, account_status, auth_status, auth_method, auth_time, limited_until, status_reason, created_at)
VALUES
(1, 'admin', '$2a$10$1jLeRdomaNKJd.sJ/rwha.eonLduoyl2MvOg1hftdHksjcdbzV2pm', '平台管理员',
 NULL, 'admin@campus.edu.cn', '管理员', NULL, '平台运营中心', NULL,
 'PRIVATE', 'ADMIN', 'NORMAL', 'VERIFIED', 'MANUAL', DATE_SUB(NOW(), INTERVAL 60 DAY), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 60 DAY)),
(2, 'seller01', '$2a$10$886/4iYslgrdDsWDcRy/8.Y8oCfIGbD1wPkuu3MFgnOCI5oEwMrWO', '林小满',
 '13800000001', 'seller01@campus.edu.cn', '林小满', '2021000101', '计算机学院', NULL,
 'AUTHENTICATED', 'USER', 'NORMAL', 'VERIFIED', 'STUDENT_NO', DATE_SUB(NOW(), INTERVAL 40 DAY), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 45 DAY)),
(3, 'buyer01', '$2a$10$886/4iYslgrdDsWDcRy/8.Y8oCfIGbD1wPkuu3MFgnOCI5oEwMrWO', '陈一诺',
 '13800000002', 'buyer01@campus.edu.cn', '陈一诺', '2021000102', '外国语学院', NULL,
 'AUTHENTICATED', 'USER', 'NORMAL', 'VERIFIED', 'STUDENT_NO', DATE_SUB(NOW(), INTERVAL 35 DAY), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 38 DAY)),
(4, 'buyer02', '$2a$10$886/4iYslgrdDsWDcRy/8.Y8oCfIGbD1wPkuu3MFgnOCI5oEwMrWO', '赵子航',
 '13800000003', 'buyer02@campus.edu.cn', '赵子航', '2021000103', '机械工程学院', NULL,
 'PUBLIC', 'USER', 'NORMAL', 'VERIFIED', 'STUDENT_NO', DATE_SUB(NOW(), INTERVAL 30 DAY), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 33 DAY)),
(5, 'newbie01', '$2a$10$886/4iYslgrdDsWDcRy/8.Y8oCfIGbD1wPkuu3MFgnOCI5oEwMrWO', '新同学',
 '13800000004', NULL, NULL, NULL, '经济管理学院', NULL,
 'PRIVATE', 'USER', 'NORMAL', 'UNVERIFIED', NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 'banned01', '$2a$10$886/4iYslgrdDsWDcRy/8.Y8oCfIGbD1wPkuu3MFgnOCI5oEwMrWO', '违规用户',
 '13800000005', NULL, '违规用户', '2021000105', '体育学院', NULL,
 'PRIVATE', 'USER', 'BANNED', 'VERIFIED', 'STUDENT_NO', DATE_SUB(NOW(), INTERVAL 25 DAY),
 DATE_ADD(NOW(), INTERVAL 20 DAY), '多次发布违规商品且警告无效', DATE_SUB(NOW(), INTERVAL 26 DAY)),
(7, 'seller02', '$2a$10$886/4iYslgrdDsWDcRy/8.Y8oCfIGbD1wPkuu3MFgnOCI5oEwMrWO', '苏小禾',
 '13800000006', 'seller02@campus.edu.cn', '苏小禾', '2021000106', '设计学院', NULL,
 'AUTHENTICATED', 'USER', 'NORMAL', 'VERIFIED', 'STUDENT_NO', DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 22 DAY));

-- ----------------------------------------------------------------------------
-- 商品分类
-- ----------------------------------------------------------------------------
INSERT INTO category (id, name, code, sort_no, enabled) VALUES
(1, '教材书籍', 'TEXTBOOK', 1, 1),
(2, '电子数码', 'DIGITAL', 2, 1),
(3, '生活用品', 'LIFE', 3, 1),
(4, '体育器材', 'SPORTS', 4, 1),
(5, '服装鞋帽', 'CLOTHING', 5, 1),
(6, '其他闲置', 'OTHER', 6, 1);

-- ----------------------------------------------------------------------------
-- 商品（覆盖草稿、待审核、在售、已锁定、已售、已下架、违规下架）
-- ----------------------------------------------------------------------------
INSERT INTO item
(id, seller_id, title, category_id, item_condition, description, price, handover_area, status,
 reject_reason, audit_by, audit_at, listed_at, view_count, favorite_count, created_at)
VALUES
(1, 2, '高等数学（第七版）上下册', 1, 'LIKE_NEW',
 '大一高数教材，上下册齐全，笔记较少，无破损，适合期末复习使用。图书馆门口当面交易。', 25.00, '图书馆', 'ON_SALE',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), 46, 1, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(2, 2, '99新 机械键盘 87键', 2, 'LIKE_NEW',
 '自用机械键盘，青轴，带背光，功能完好，因换键盘出闲置，可现场测试。', 180.00, '东区宿舍', 'ON_SALE',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 88, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(3, 2, '小米台灯 护眼版', 3, 'GOOD',
 '护眼台灯，三档亮度，使用一学期，外观有轻微使用痕迹，照明正常。', 35.00, '西区宿舍', 'ON_SALE',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 32, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4, 7, '羽毛球拍一对（含球）', 4, 'GOOD',
 '入门级羽毛球拍一对，附赠 3 个球，握把已更换，适合日常锻炼。', 60.00, '体育馆', 'ON_SALE',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 21, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 2, '考研英语真题（2020-2025）', 1, 'GOOD',
 '考研英语一真题，做过部分笔记，答案完整，适合二战同学刷题。', 20.00, '图书馆', 'ON_SALE',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 15, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 7, '冬季保暖外套 L 码', 5, 'LIKE_NEW',
 '去年冬天买的加厚外套，只穿过两次，尺码偏大，适合 175-180cm 同学。', 120.00, '南区宿舍', 'PENDING',
 NULL, NULL, NULL, NULL, 4, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 2, '27寸 2K 显示器', 2, 'GOOD',
 '27 寸 2K 显示器，IPS 屏，无亮点坏点，带 HDMI 线，可现场测试后自提。', 450.00, '东区宿舍', 'LOCKED',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 67, 0, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(8, 2, '蓝牙耳机（续航 20 小时）', 2, 'GOOD',
 '蓝牙 5.3 耳机，续航约 20 小时，音质正常，附充电仓，因换新出。', 90.00, '北区宿舍', 'SOLD',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), 73, 1, DATE_SUB(NOW(), INTERVAL 21 DAY)),
(9, 2, '山地自行车 21 速', 4, 'FAIR',
 '21 速山地车，骑行两年，链条和刹车近期保养过，有正常使用痕迹。', 260.00, '校门口', 'LOCKED',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), 54, 0, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(10, 7, '宿舍小冰箱 50L', 3, 'GOOD',
 '50L 单门小冰箱，制冷正常，轻微噪音，毕业出闲置，需自提。', 200.00, '西区宿舍', 'DRAFT',
 NULL, NULL, NULL, NULL, 2, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 2, '多功能折叠刀', 6, 'NEW',
 '全新多功能折叠刀，可折叠便携。', 45.00, '东区宿舍', 'VIOLATED',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, 9, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(12, 2, '旧版英语词典', 1, 'FAIR',
 '旧版英汉词典，书页泛黄但完整，适合查阅使用。', 10.00, '图书馆', 'OFF_SHELF',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NULL, 6, 0, DATE_SUB(NOW(), INTERVAL 9 DAY));

-- ----------------------------------------------------------------------------
-- 商品图片
-- ----------------------------------------------------------------------------
INSERT INTO item_image (item_id, url, sort_no) VALUES
(1, '/images/item-1.svg', 0),
(1, '/images/item-2.svg', 1),
(2, '/images/item-2.svg', 0),
(2, '/images/item-3.svg', 1),
(3, '/images/item-3.svg', 0),
(4, '/images/item-4.svg', 0),
(5, '/images/item-1.svg', 0),
(6, '/images/item-5.svg', 0),
(7, '/images/item-2.svg', 0),
(8, '/images/item-6.svg', 0),
(9, '/images/item-4.svg', 0),
(10, '/images/item-3.svg', 0),
(11, '/images/item-8.svg', 0),
(12, '/images/item-1.svg', 0);

-- ----------------------------------------------------------------------------
-- 收藏
-- ----------------------------------------------------------------------------
INSERT INTO favorite (user_id, item_id, created_at) VALUES
(3, 2, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 8, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 3, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ----------------------------------------------------------------------------
-- 咨询会话与消息
-- ----------------------------------------------------------------------------
INSERT INTO conversation (id, item_id, buyer_id, seller_id, last_message_at, created_at) VALUES
(1, 7, 4, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 5, 3, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO message (conversation_id, sender_id, content, created_at) VALUES
(1, 4, '同学你好，显示器还在吗？可以现场测试吗？', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 2, '在的，可以来东区宿舍现场测试，晚上七点后方便。', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 4, '好的，那我们约明晚七点，谢谢！', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 3, '真题的答案完整吗？', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, '答案完整，笔记不多，基本可以放心用。', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ----------------------------------------------------------------------------
-- 订单：已完成、待线下交付、争议中、待确认、未中选、买家取消
-- ----------------------------------------------------------------------------
INSERT INTO trade_order
(id, order_no, item_id, buyer_id, seller_id, status, request_remark, cancel_type, cancel_reason,
 cancelled_by, accepted_at, buyer_confirmed_at, seller_confirmed_at, completed_at, closed_at,
 expire_at, dispute_reason, disputed_by, created_at, updated_at)
VALUES
(1, 'O20261001120000DEMO', 8, 3, 2, 'COMPLETED', '希望周末在北区宿舍交易',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'O20261003180000DEMO', 7, 4, 2, 'DELIVERING', '想先看看屏幕有无坏点',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 'O20260930100000DEMO', 9, 3, 2, 'DISPUTE', '希望配一把车锁',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 6 DAY), '收到自行车后发现没有车锁，与描述不一致，联系卖家未解决。', 3,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 'O20261107100000DEMO', 5, 4, 2, 'PENDING_SELLER', '周末图书馆交易可以吗？',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 DATE_ADD(NOW(), INTERVAL 22 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 'O20261107113000DEMO', 2, 3, 2, 'PENDING_SELLER', '想测试一下手感',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 DATE_ADD(NOW(), INTERVAL 23 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(6, 'O20261003170000DEMO', 7, 3, 2, 'CANCELLED', '想购买显示器',
 'NOT_SELECTED', '卖家已接受其他买家的请求，本请求未中选', NULL,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY),
 DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 'O20261105120000DEMO', 3, 4, 2, 'CANCELLED', '考虑一下台灯',
 'BUYER_CANCEL', '已在线下买到同款台灯', 4,
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY),
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ----------------------------------------------------------------------------
-- 线下交付记录与变更留痕
-- ----------------------------------------------------------------------------
INSERT INTO handover_record
(order_id, handover_time, area, note, buyer_confirmed_at, seller_confirmed_at, updated_by, created_at, updated_at)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 3 DAY), '北区宿舍', '当面验货并完成付款', DATE_SUB(NOW(), INTERVAL 2 DAY),
 DATE_SUB(NOW(), INTERVAL 2 DAY), 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, DATE_ADD(NOW(), INTERVAL 1 DAY), '东区宿舍', '显示器可现场接笔记本测试，确认无坏点', NULL, NULL, 2,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, DATE_SUB(NOW(), INTERVAL 4 DAY), '校门口', '卖家称会配车锁', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 3,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO handover_log (order_id, changed_by, summary, created_at) VALUES
(2, 2, '交付时间：未设置 → 明天 19:00；交付区域：未设置 → 东区宿舍；备注：未设置 → 显示器可现场接笔记本测试', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 3, '备注：卖家称会配车锁', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ----------------------------------------------------------------------------
-- 交易评价（已完成订单双方各一条）
-- ----------------------------------------------------------------------------
INSERT INTO review (order_id, author_id, target_id, rating, content, created_at) VALUES
(1, 3, 2, 5, '耳机成色很好，卖家很守时，交易顺利。', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 2, 3, 5, '买家沟通顺畅，准时到场，交易愉快。', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ----------------------------------------------------------------------------
-- 举报（待处理、处理中、成立、不成立）
-- ----------------------------------------------------------------------------
INSERT INTO report
(report_no, reporter_id, target_type, target_id, category, description, snapshot, status,
 handler_id, handling_result, rule_basis, action_summary, appeal_reason, claimed_at, handled_at, created_at)
VALUES
('R202610200001', 4, 'ITEM', 11, 'PROHIBITED', '该商品疑似管制刀具，不符合校园禁售范围，建议尽快下架。',
 '{"summary":"商品：多功能折叠刀（¥45.00，违规下架）","itemId":11}', 'UPHELD',
 1, '举报成立，商品已违规下架并通知卖家。', 'BR-03 禁止发布法律、校规或平台规则禁止流转的物品', '违规下架商品《多功能折叠刀》',
 NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
('R202610180002', 3, 'USER', 6, 'ABUSE', '该用户多次在咨询中辱骂他人，希望平台处理。',
 '{"summary":"用户：违规用户","userId":6}', 'REJECTED',
 1, '经核查聊天记录，未发现辱骂证据，举报不成立。', 'BR-12 举报须提供说明与证据', '不执行处置动作',
 NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
('R202611070003', 4, 'ITEM', 5, 'FAKE', '商品描述写着真题完整，但咨询时卖家说答案不全，怀疑信息不实。',
 '{"summary":"商品：考研英语真题（2020-2025）（¥20.00，在售）","itemId":5}', 'SUBMITTED',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
('R202611060004', 3, 'USER', 2, 'FRAUD', '约定交易后卖家临时加价，感觉存在欺诈行为。',
 '{"summary":"用户：林小满","userId":2}', 'PROCESSING',
 1, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ----------------------------------------------------------------------------
-- 处置动作
-- ----------------------------------------------------------------------------
INSERT INTO moderation_action
(target_type, target_id, action_type, operator_id, reason, rule_basis, duration_days,
 effective_from, effective_to, related_report_id, created_at)
VALUES
('ITEM', 11, 'TAKEDOWN', 1, '发布违禁物品，举报成立', 'BR-03', NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('USER', 6, 'WARN', 1, '首次发布违规内容，予以警告', 'BR-03、BR-13', NULL,
 DATE_SUB(NOW(), INTERVAL 15 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 15 DAY)),
('USER', 6, 'BAN', 1, '多次发布违规商品且警告无效', 'BR-13、BR-16', 30,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ----------------------------------------------------------------------------
-- 审计日志
-- ----------------------------------------------------------------------------
INSERT INTO audit_log
(operator_id, operator_name, action, target_type, target_id, reason, before_value, after_value, ip, created_at)
VALUES
(2, 'seller01', 'ITEM_CREATE', 'ITEM', 8, '卖家创建商品草稿', NULL, '蓝牙耳机（续航 20 小时）', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 21 DAY)),
(1, 'admin', 'ITEM_APPROVE', 'ITEM', 8, '管理员审核通过', 'PENDING', 'ON_SALE', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, 'admin', 'ITEM_TAKEDOWN', 'ITEM', 11, '发布违禁物品，举报成立（依据：BR-03）', 'PENDING', 'VIOLATED', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 'buyer01', 'ORDER_CREATE', 'ORDER', 1, '买家创建待确认请求', NULL, 'O20261001120000DEMO', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 'seller01', 'ORDER_ACCEPT', 'ORDER', 1, '卖家接受待确认请求', 'PENDING_SELLER', 'DELIVERING', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'buyer01', 'ORDER_CONFIRM', 'ORDER', 1, '一方确认交付', 'DELIVERING', 'BUYER_CONFIRMED', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'seller01', 'ORDER_COMPLETE', 'ORDER', 1, '双方确认交付，订单完成', 'BUYER_CONFIRMED', 'COMPLETED', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'admin', 'REPORT_HANDLE', 'REPORT', 1, '举报成立，商品已违规下架并通知卖家。', 'PROCESSING', 'UPHELD', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 'admin', 'USER_BAN', 'USER', 6, '多次发布违规商品且警告无效（依据：BR-13、BR-16）', 'NORMAL', 'BANNED', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ----------------------------------------------------------------------------
-- 站内通知
-- ----------------------------------------------------------------------------
INSERT INTO notification
(user_id, type, title, content, related_type, related_id, read_flag, send_status, retry_count, created_at, read_at)
VALUES
(2, 'ORDER', '收到新的待确认请求', '赵子航 对你的商品《考研英语真题（2020-2025）》提交了待确认请求，请及时处理（24 小时内未处理将自动取消）。',
 'ORDER', 4, 0, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL),
(2, 'ORDER', '收到新的待确认请求', '陈一诺 对你的商品《99新 机械键盘 87键》提交了待确认请求，请及时处理。',
 'ORDER', 5, 0, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL),
(3, 'ORDER', '交易已完成', '你与卖家的《蓝牙耳机（续航 20 小时）》交易已完成，欢迎在 7 天内评价对方。',
 'ORDER', 1, 1, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 'ORDER', '待确认请求未中选', '你申请的商品《27寸 2K 显示器》已被其他买家抢先达成交易，本次请求未中选。',
 'ORDER', 6, 0, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(2, 'ITEM', '商品审核通过', '你的商品《99新 机械键盘 87键》已通过审核并上架，可被校内用户搜索到。',
 'ITEM', 2, 1, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6, 'ACCOUNT', '账户处置通知：封禁', '你的账户已被封禁至 30 天后。理由：多次发布违规商品且警告无效。',
 NULL, 3, 0, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL),
(2, 'REPORT', '举报处理结果', '你提交的举报（R202610180002）处理结果：举报不成立。处理说明：经核查聊天记录，未发现辱骂证据，举报不成立。',
 'REPORT', 2, 1, 'SENT', 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 修正自增起点，避免后续业务数据与演示 ID 冲突
ALTER TABLE sys_user AUTO_INCREMENT = 100;
ALTER TABLE item AUTO_INCREMENT = 100;
ALTER TABLE trade_order AUTO_INCREMENT = 100;
ALTER TABLE report AUTO_INCREMENT = 100;
ALTER TABLE conversation AUTO_INCREMENT = 100;
ALTER TABLE favorite AUTO_INCREMENT = 100;
ALTER TABLE review AUTO_INCREMENT = 100;
ALTER TABLE moderation_action AUTO_INCREMENT = 100;
ALTER TABLE audit_log AUTO_INCREMENT = 100;
ALTER TABLE notification AUTO_INCREMENT = 100;
ALTER TABLE handover_record AUTO_INCREMENT = 100;
ALTER TABLE handover_log AUTO_INCREMENT = 100;
