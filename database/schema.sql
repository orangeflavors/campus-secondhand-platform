-- ============================================================================
-- 校园二手交易与物品流转平台 · 数据库结构（MySQL 8.0）
-- 对应需求：SRS-CSTP-001 v1.1
-- 说明：本脚本可重复执行；会重建全部业务表（请勿在生产数据上直接执行）
-- ============================================================================

CREATE DATABASE IF NOT EXISTS campus_secondhand
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE campus_secondhand;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS moderation_action;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS handover_log;
DROP TABLE IF EXISTS handover_record;
DROP TABLE IF EXISTS trade_order;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS item_image;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS sys_user;

-- ----------------------------------------------------------------------------
-- 用户账户（FR-001 / FR-002 / FR-003 / FR-004）
-- ----------------------------------------------------------------------------
CREATE TABLE sys_user (
  id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username           VARCHAR(50)  NOT NULL                COMMENT '登录名',
  password_hash      VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码哈希',
  nickname           VARCHAR(50)  NOT NULL                COMMENT '昵称（公开）',
  phone              VARCHAR(20)  DEFAULT NULL            COMMENT '手机号（敏感）',
  email              VARCHAR(100) DEFAULT NULL            COMMENT '邮箱（敏感）',
  real_name          VARCHAR(50)  DEFAULT NULL            COMMENT '校园认证真实姓名（不公开）',
  student_no         VARCHAR(32)  DEFAULT NULL            COMMENT '学号（不公开）',
  college            VARCHAR(100) DEFAULT NULL            COMMENT '学院（公开）',
  avatar_url         VARCHAR(255) DEFAULT NULL            COMMENT '头像地址',
  contact_visibility VARCHAR(20)  NOT NULL DEFAULT 'AUTHENTICATED'
                     COMMENT '联系方式可见范围：PUBLIC / AUTHENTICATED / PRIVATE',
  role               VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
  account_status     VARCHAR(20)  NOT NULL DEFAULT 'NORMAL'
                     COMMENT '账户状态：NORMAL / LIMITED / BANNED',
  auth_status        VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED'
                     COMMENT '校园认证状态：UNVERIFIED / PENDING / VERIFIED / REJECTED',
  auth_method        VARCHAR(30)  DEFAULT NULL            COMMENT '认证方式：STUDENT_NO / EMAIL / MANUAL',
  auth_time          DATETIME     DEFAULT NULL            COMMENT '认证通过时间',
  limited_until      DATETIME     DEFAULT NULL            COMMENT '限权到期时间（NULL 表示长期）',
  status_reason      VARCHAR(255) DEFAULT NULL            COMMENT '当前账户状态说明（对本人可见）',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version            INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_student_no (student_no),
  KEY idx_user_role (role),
  KEY idx_user_account_status (account_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户账户';

-- ----------------------------------------------------------------------------
-- 商品分类
-- ----------------------------------------------------------------------------
CREATE TABLE category (
  id       BIGINT      NOT NULL AUTO_INCREMENT,
  name     VARCHAR(50) NOT NULL COMMENT '分类名称',
  code     VARCHAR(30) NOT NULL COMMENT '分类编码',
  sort_no  INT         NOT NULL DEFAULT 0,
  enabled  TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类';

-- ----------------------------------------------------------------------------
-- 商品（FR-005 ~ FR-009 / 状态机 ST-I01 ~ ST-I10）
-- ----------------------------------------------------------------------------
CREATE TABLE item (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  seller_id      BIGINT        NOT NULL COMMENT '卖家（创建后不可更换）',
  title          VARCHAR(100)  NOT NULL,
  category_id    BIGINT        NOT NULL,
  item_condition VARCHAR(20)   NOT NULL DEFAULT 'GOOD'
                 COMMENT '成色：NEW / LIKE_NEW / GOOD / FAIR',
  description    TEXT          COMMENT '描述',
  price          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '标价，0 表示免费流转',
  handover_area  VARCHAR(50)   DEFAULT NULL COMMENT '校内交付区域',
  status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                 COMMENT 'DRAFT/PENDING/REJECTED/ON_SALE/LOCKED/SOLD/OFF_SHELF/VIOLATED',
  reject_reason  VARCHAR(500)  DEFAULT NULL COMMENT '审核驳回原因',
  audit_by       BIGINT        DEFAULT NULL COMMENT '审核管理员',
  audit_at       DATETIME      DEFAULT NULL COMMENT '审核时间',
  listed_at      DATETIME      DEFAULT NULL COMMENT '上架（在售）时间，用于排序',
  view_count     INT           NOT NULL DEFAULT 0,
  favorite_count INT           NOT NULL DEFAULT 0,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version        INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_item_status_listed (status, listed_at),
  KEY idx_item_category (category_id),
  KEY idx_item_seller (seller_id, status),
  CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品';

CREATE TABLE item_image (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  item_id    BIGINT       NOT NULL,
  url        VARCHAR(255) NOT NULL,
  sort_no    INT          NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_image_item (item_id),
  CONSTRAINT fk_image_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品图片';

-- ----------------------------------------------------------------------------
-- 收藏（FR-014）
-- ----------------------------------------------------------------------------
CREATE TABLE favorite (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL,
  item_id    BIGINT   NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_user_item (user_id, item_id),
  KEY idx_favorite_item (item_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_favorite_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收藏';

-- ----------------------------------------------------------------------------
-- 咨询会话与消息（FR-015 / FR-016）
-- ----------------------------------------------------------------------------
CREATE TABLE conversation (
  id              BIGINT   NOT NULL AUTO_INCREMENT,
  item_id         BIGINT   NOT NULL,
  buyer_id        BIGINT   NOT NULL,
  seller_id       BIGINT   NOT NULL,
  last_message_at DATETIME DEFAULT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_conversation_item_buyer (item_id, buyer_id),
  KEY idx_conversation_buyer (buyer_id),
  KEY idx_conversation_seller (seller_id),
  CONSTRAINT fk_conversation_item FOREIGN KEY (item_id) REFERENCES item (id),
  CONSTRAINT fk_conversation_buyer FOREIGN KEY (buyer_id) REFERENCES sys_user (id),
  CONSTRAINT fk_conversation_seller FOREIGN KEY (seller_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品咨询会话';

CREATE TABLE message (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT        NOT NULL,
  sender_id       BIGINT        NOT NULL,
  content         VARCHAR(1000) NOT NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_message_conversation (conversation_id, id),
  CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '咨询消息';

-- ----------------------------------------------------------------------------
-- 订单 / 待确认请求（FR-017 ~ FR-024）
-- 关键约束：同一商品最多 1 条"已接受且未终结"订单；同一买家对同一商品最多 1 条待确认请求。
-- 通过生成列 + 唯一索引在数据库层兜底（BR-06 / BR-19）。
-- ----------------------------------------------------------------------------
CREATE TABLE trade_order (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  order_no            VARCHAR(32)  NOT NULL COMMENT '订单/请求号',
  item_id             BIGINT       NOT NULL,
  buyer_id            BIGINT       NOT NULL,
  seller_id           BIGINT       NOT NULL,
  status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_SELLER'
                      COMMENT 'PENDING_SELLER/DELIVERING/BUYER_CONFIRMED/SELLER_CONFIRMED/COMPLETED/CANCELLED/DISPUTE/MANAGEMENT_CLOSED',
  request_remark      VARCHAR(255) DEFAULT NULL COMMENT '买家请求备注',
  cancel_type         VARCHAR(30)  DEFAULT NULL
                      COMMENT 'BUYER_CANCEL/SELLER_REJECT/TIMEOUT/NOT_SELECTED/ADMIN_CLOSE',
  cancel_reason       VARCHAR(500) DEFAULT NULL,
  cancelled_by        BIGINT       DEFAULT NULL,
  accepted_at         DATETIME     DEFAULT NULL,
  buyer_confirmed_at  DATETIME     DEFAULT NULL,
  seller_confirmed_at DATETIME     DEFAULT NULL,
  completed_at        DATETIME     DEFAULT NULL,
  closed_at           DATETIME     DEFAULT NULL,
  expire_at           DATETIME     DEFAULT NULL COMMENT '待确认请求超时时间（候选 24 小时）',
  dispute_reason      VARCHAR(500) DEFAULT NULL,
  disputed_by         BIGINT       DEFAULT NULL,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version             INT          NOT NULL DEFAULT 0,
  active_item_key     BIGINT       GENERATED ALWAYS AS (
                        CASE WHEN status IN ('DELIVERING','BUYER_CONFIRMED','SELLER_CONFIRMED','DISPUTE')
                             THEN item_id ELSE NULL END
                      ) STORED COMMENT '活跃订单去重键',
  pending_buyer_key   VARCHAR(80)  GENERATED ALWAYS AS (
                        CASE WHEN status = 'PENDING_SELLER'
                             THEN CONCAT(item_id, '-', buyer_id) ELSE NULL END
                      ) STORED COMMENT '待确认请求去重键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_active_item (active_item_key),
  UNIQUE KEY uk_order_pending_buyer (pending_buyer_key),
  KEY idx_order_item (item_id),
  KEY idx_order_buyer (buyer_id, status),
  KEY idx_order_seller (seller_id, status),
  KEY idx_order_status (status),
  CONSTRAINT fk_order_item FOREIGN KEY (item_id) REFERENCES item (id),
  CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES sys_user (id),
  CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单（待确认请求与已接受订单）';

-- ----------------------------------------------------------------------------
-- 线下交付约定与双方确认（FR-021 ~ FR-024）
-- ----------------------------------------------------------------------------
CREATE TABLE handover_record (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  order_id            BIGINT       NOT NULL,
  handover_time       DATETIME     DEFAULT NULL COMMENT '约定交付时间',
  area                VARCHAR(50)  DEFAULT NULL COMMENT '约定交付区域',
  note                VARCHAR(500) DEFAULT NULL,
  buyer_confirmed_at  DATETIME     DEFAULT NULL,
  seller_confirmed_at DATETIME     DEFAULT NULL,
  updated_by          BIGINT       DEFAULT NULL,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_handover_order (order_id),
  CONSTRAINT fk_handover_order FOREIGN KEY (order_id) REFERENCES trade_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '线下交付记录';

CREATE TABLE handover_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  order_id    BIGINT       NOT NULL,
  changed_by  BIGINT       NOT NULL,
  summary     VARCHAR(500) NOT NULL COMMENT '变更摘要',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_handover_log_order (order_id),
  CONSTRAINT fk_handover_log_order FOREIGN KEY (order_id) REFERENCES trade_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '交付约定变更留痕';

-- ----------------------------------------------------------------------------
-- 交易评价（FR-025 / BR-11）
-- ----------------------------------------------------------------------------
CREATE TABLE review (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  order_id   BIGINT       NOT NULL,
  author_id  BIGINT       NOT NULL COMMENT '评价人',
  target_id  BIGINT       NOT NULL COMMENT '被评价人',
  rating     TINYINT      NOT NULL COMMENT '1-5 星',
  content    VARCHAR(500) DEFAULT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_order_author (order_id, author_id),
  KEY idx_review_target (target_id),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES trade_order (id),
  CONSTRAINT fk_review_author FOREIGN KEY (author_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '交易评价';

-- ----------------------------------------------------------------------------
-- 举报与治理（FR-026 ~ FR-031）
-- ----------------------------------------------------------------------------
CREATE TABLE report (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  report_no        VARCHAR(32)   NOT NULL,
  reporter_id      BIGINT        NOT NULL,
  target_type      VARCHAR(20)   NOT NULL COMMENT 'ITEM / USER / MESSAGE',
  target_id        BIGINT        NOT NULL,
  category         VARCHAR(30)   NOT NULL COMMENT '举报类别',
  description      VARCHAR(1000) NOT NULL,
  snapshot         TEXT          COMMENT '举报时的对象快照（JSON），不可由普通用户覆盖',
  status           VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED'
                   COMMENT 'SUBMITTED/PROCESSING/SUPPLEMENT/UPHELD/REJECTED/PENDING_APPEAL/CLOSED',
  handler_id       BIGINT        DEFAULT NULL,
  handling_result  VARCHAR(500)  DEFAULT NULL COMMENT '处理结论的公开说明',
  rule_basis       VARCHAR(500)  DEFAULT NULL COMMENT '规则依据',
  action_summary   VARCHAR(255)  DEFAULT NULL COMMENT '执行的处置动作摘要',
  appeal_reason    VARCHAR(500)  DEFAULT NULL,
  claimed_at       DATETIME      DEFAULT NULL,
  handled_at       DATETIME      DEFAULT NULL,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version          INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_report_no (report_no),
  KEY idx_report_status (status, created_at),
  KEY idx_report_reporter (reporter_id),
  KEY idx_report_target (target_type, target_id),
  CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '举报';

CREATE TABLE moderation_action (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  target_type       VARCHAR(20)  NOT NULL COMMENT 'ITEM / USER',
  target_id         BIGINT       NOT NULL,
  action_type       VARCHAR(30)  NOT NULL COMMENT 'TAKEDOWN/WARN/LIMIT/BAN/UNBAN',
  operator_id       BIGINT       NOT NULL,
  reason            VARCHAR(500) NOT NULL,
  rule_basis        VARCHAR(500) DEFAULT NULL,
  duration_days     INT          DEFAULT NULL COMMENT '限时处置天数，NULL 表示长期/永久',
  effective_from    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  effective_to      DATETIME     DEFAULT NULL,
  related_report_id BIGINT       DEFAULT NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_action_target (target_type, target_id),
  KEY idx_action_operator (operator_id),
  CONSTRAINT fk_action_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员处置动作';

-- ----------------------------------------------------------------------------
-- 审计日志（FR-031 / BR-16）—— 仅追加，不提供普通删除接口
-- ----------------------------------------------------------------------------
CREATE TABLE audit_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  action        VARCHAR(50)  NOT NULL COMMENT '动作编码，如 ITEM_APPROVE',
  target_type   VARCHAR(20)  DEFAULT NULL COMMENT 'ITEM/ORDER/USER/REPORT/SYSTEM',
  target_id     BIGINT       DEFAULT NULL,
  reason        VARCHAR(500) DEFAULT NULL,
  before_value  TEXT         COMMENT '变更前值（JSON）',
  after_value   TEXT         COMMENT '变更后值（JSON）',
  ip            VARCHAR(45)  DEFAULT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_created (created_at),
  KEY idx_audit_action (action),
  KEY idx_audit_target (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审计日志';

-- ----------------------------------------------------------------------------
-- 站内通知（FR-032 / BR-17）
-- ----------------------------------------------------------------------------
CREATE TABLE notification (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  user_id      BIGINT       NOT NULL,
  type         VARCHAR(30)  NOT NULL COMMENT 'ITEM/ORDER/REPORT/REVIEW/ACCOUNT/SYSTEM',
  title        VARCHAR(100) NOT NULL,
  content      VARCHAR(500) NOT NULL,
  related_type VARCHAR(20)  DEFAULT NULL COMMENT 'ITEM/ORDER/REPORT/CONVERSATION',
  related_id   BIGINT       DEFAULT NULL,
  read_flag    TINYINT      NOT NULL DEFAULT 0,
  send_status  VARCHAR(20)  NOT NULL DEFAULT 'SENT' COMMENT 'SENT/FAILED',
  retry_count  INT          NOT NULL DEFAULT 0,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at      DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_notification_user (user_id, read_flag, id),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内通知';

SET FOREIGN_KEY_CHECKS = 1;
