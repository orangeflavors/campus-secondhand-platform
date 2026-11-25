package com.campus.trade.common;

import java.util.Set;

/**
 * 业务常量：状态值、类别与规则参数（与前端 src/utils/dict.ts 保持一致）。
 */
public final class TradeConst {

    private TradeConst() {
    }

    /* ------------------------------ 角色 ------------------------------ */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    /* ------------------------------ 账户状态 ------------------------------ */
    public static final String ACCOUNT_NORMAL = "NORMAL";
    public static final String ACCOUNT_LIMITED = "LIMITED";
    public static final String ACCOUNT_BANNED = "BANNED";

    /* ------------------------------ 校园认证 ------------------------------ */
    public static final String AUTH_UNVERIFIED = "UNVERIFIED";
    public static final String AUTH_PENDING = "PENDING";
    public static final String AUTH_VERIFIED = "VERIFIED";
    public static final String AUTH_REJECTED = "REJECTED";

    /* ------------------------------ 联系方式可见范围 ------------------------------ */
    public static final String CONTACT_PUBLIC = "PUBLIC";
    public static final String CONTACT_AUTHENTICATED = "AUTHENTICATED";
    public static final String CONTACT_PRIVATE = "PRIVATE";

    /* ------------------------------ 成色 ------------------------------ */
    public static final String CONDITION_NEW = "NEW";
    public static final String CONDITION_LIKE_NEW = "LIKE_NEW";
    public static final String CONDITION_GOOD = "GOOD";
    public static final String CONDITION_FAIR = "FAIR";

    public static final Set<String> ITEM_CONDITIONS =
            Set.of(CONDITION_NEW, CONDITION_LIKE_NEW, CONDITION_GOOD, CONDITION_FAIR);

    /* ------------------------------ 商品状态 ------------------------------ */
    public static final String ITEM_DRAFT = "DRAFT";
    public static final String ITEM_PENDING = "PENDING";
    public static final String ITEM_REJECTED = "REJECTED";
    public static final String ITEM_ON_SALE = "ON_SALE";
    public static final String ITEM_LOCKED = "LOCKED";
    public static final String ITEM_SOLD = "SOLD";
    public static final String ITEM_OFF_SHELF = "OFF_SHELF";
    public static final String ITEM_VIOLATED = "VIOLATED";

    /* ------------------------------ 订单状态 ------------------------------ */
    public static final String ORDER_PENDING_SELLER = "PENDING_SELLER";
    public static final String ORDER_DELIVERING = "DELIVERING";
    public static final String ORDER_BUYER_CONFIRMED = "BUYER_CONFIRMED";
    public static final String ORDER_SELLER_CONFIRMED = "SELLER_CONFIRMED";
    public static final String ORDER_COMPLETED = "COMPLETED";
    public static final String ORDER_CANCELLED = "CANCELLED";
    public static final String ORDER_DISPUTE = "DISPUTE";
    public static final String ORDER_MANAGEMENT_CLOSED = "MANAGEMENT_CLOSED";

    /** 已接受且未终结的订单状态（同一商品最多 1 条）。 */
    public static final Set<String> ORDER_ACTIVE_STATUSES = Set.of(
            ORDER_DELIVERING, ORDER_BUYER_CONFIRMED, ORDER_SELLER_CONFIRMED, ORDER_DISPUTE);

    /* ------------------------------ 取消类型 ------------------------------ */
    public static final String CANCEL_BUYER = "BUYER_CANCEL";
    public static final String CANCEL_SELLER_REJECT = "SELLER_REJECT";
    public static final String CANCEL_TIMEOUT = "TIMEOUT";
    public static final String CANCEL_NOT_SELECTED = "NOT_SELECTED";
    public static final String CANCEL_ADMIN_CLOSE = "ADMIN_CLOSE";

    /* ------------------------------ 举报 ------------------------------ */
    public static final String REPORT_TARGET_ITEM = "ITEM";
    public static final String REPORT_TARGET_USER = "USER";
    public static final String REPORT_TARGET_MESSAGE = "MESSAGE";
    public static final Set<String> REPORT_TARGETS = Set.of(REPORT_TARGET_ITEM, REPORT_TARGET_USER, REPORT_TARGET_MESSAGE);

    public static final String REPORT_SUBMITTED = "SUBMITTED";
    public static final String REPORT_PROCESSING = "PROCESSING";
    public static final String REPORT_SUPPLEMENT = "SUPPLEMENT";
    public static final String REPORT_UPHELD = "UPHELD";
    public static final String REPORT_REJECTED = "REJECTED";
    public static final String REPORT_PENDING_APPEAL = "PENDING_APPEAL";
    public static final String REPORT_CLOSED = "CLOSED";

    public static final Set<String> REPORT_CATEGORIES =
            Set.of("PROHIBITED", "FAKE", "FRAUD", "ABUSE", "SPAM", "OTHER");

    /* ------------------------------ 处置动作 ------------------------------ */
    public static final String ACTION_TAKEDOWN = "TAKEDOWN";
    public static final String ACTION_WARN = "WARN";
    public static final String ACTION_LIMIT = "LIMIT";
    public static final String ACTION_BAN = "BAN";
    public static final String ACTION_UNBAN = "UNBAN";
    public static final Set<String> USER_ACTIONS = Set.of(ACTION_WARN, ACTION_LIMIT, ACTION_BAN, ACTION_UNBAN);

    /* ------------------------------ 通知 ------------------------------ */
    public static final String NOTIFY_ITEM = "ITEM";
    public static final String NOTIFY_ORDER = "ORDER";
    public static final String NOTIFY_REPORT = "REPORT";
    public static final String NOTIFY_REVIEW = "REVIEW";
    public static final String NOTIFY_ACCOUNT = "ACCOUNT";
    public static final String NOTIFY_SYSTEM = "SYSTEM";

    public static final String NOTIFY_SENT = "SENT";
    public static final String NOTIFY_FAILED = "FAILED";

    /* ------------------------------ 状态文案 ------------------------------ */
    public static String itemStatusText(String status) {
        return switch (status) {
            case ITEM_DRAFT -> "草稿";
            case ITEM_PENDING -> "待审核";
            case ITEM_REJECTED -> "审核驳回";
            case ITEM_ON_SALE -> "在售";
            case ITEM_LOCKED -> "已锁定";
            case ITEM_SOLD -> "已售出";
            case ITEM_OFF_SHELF -> "已下架";
            case ITEM_VIOLATED -> "违规下架";
            default -> status;
        };
    }

    public static String orderStatusText(String status) {
        return switch (status) {
            case ORDER_PENDING_SELLER -> "待卖家确认";
            case ORDER_DELIVERING -> "待线下交付";
            case ORDER_BUYER_CONFIRMED -> "买家已确认";
            case ORDER_SELLER_CONFIRMED -> "卖家已确认";
            case ORDER_COMPLETED -> "已完成";
            case ORDER_CANCELLED -> "已取消";
            case ORDER_DISPUTE -> "争议中";
            case ORDER_MANAGEMENT_CLOSED -> "管理关闭";
            default -> status;
        };
    }

    public static String cancelTypeText(String cancelType) {
        if (cancelType == null) {
            return null;
        }
        return switch (cancelType) {
            case CANCEL_BUYER -> "买家取消";
            case CANCEL_SELLER_REJECT -> "卖家拒绝";
            case CANCEL_TIMEOUT -> "超时未确认";
            case CANCEL_NOT_SELECTED -> "未中选";
            case CANCEL_ADMIN_CLOSE -> "管理员关闭";
            default -> cancelType;
        };
    }

    public static String reportStatusText(String status) {
        return switch (status) {
            case REPORT_SUBMITTED -> "待处理";
            case REPORT_PROCESSING -> "处理中";
            case REPORT_SUPPLEMENT -> "待补充证据";
            case REPORT_UPHELD -> "举报成立";
            case REPORT_REJECTED -> "举报不成立";
            case REPORT_PENDING_APPEAL -> "申诉中";
            case REPORT_CLOSED -> "已关闭";
            default -> status;
        };
    }

    public static String conditionText(String condition) {
        if (condition == null) {
            return null;
        }
        return switch (condition) {
            case CONDITION_NEW -> "全新";
            case CONDITION_LIKE_NEW -> "几乎全新";
            case CONDITION_GOOD -> "轻微使用痕迹";
            case CONDITION_FAIR -> "明显使用痕迹";
            default -> condition;
        };
    }
}
