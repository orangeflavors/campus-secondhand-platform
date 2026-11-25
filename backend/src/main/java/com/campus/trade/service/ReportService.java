package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.PageUtils;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.AdminDtos;
import com.campus.trade.dto.ReportDtos;
import com.campus.trade.entity.Item;
import com.campus.trade.entity.Message;
import com.campus.trade.entity.Report;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.MessageMapper;
import com.campus.trade.mapper.ReportMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.ReportVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 举报提交、查询与管理员处理（FR-026 ~ FR-028 / ST-R01 ~ ST-R08）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final MessageMapper messageMapper;
    private final ItemService itemService;
    private final SanctionService sanctionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> TARGET_TEXT = Map.of(
            TradeConst.REPORT_TARGET_ITEM, "商品",
            TradeConst.REPORT_TARGET_USER, "用户",
            TradeConst.REPORT_TARGET_MESSAGE, "消息"
    );

    private static final Map<String, String> CATEGORY_TEXT = Map.of(
            "PROHIBITED", "违禁物品",
            "FAKE", "虚假信息",
            "FRAUD", "欺诈行为",
            "ABUSE", "辱骂骚扰",
            "SPAM", "广告刷屏",
            "OTHER", "其他违规"
    );

    /* ------------------------------ 用户端 ------------------------------ */

    @Transactional
    public ReportVO create(Long reporterId, ReportDtos.CreateReportRequest request) {
        if (!TradeConst.REPORT_TARGETS.contains(request.targetType())) {
            throw BizException.badRequest("举报对象类型不合法");
        }
        if (!TradeConst.REPORT_CATEGORIES.contains(request.category())) {
            throw BizException.badRequest("举报类别不合法");
        }
        String snapshot = buildSnapshot(reporterId, request.targetType(), request.targetId());

        Report report = new Report();
        report.setReportNo(TextUtils.generateNo("R"));
        report.setReporterId(reporterId);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setCategory(request.category());
        report.setDescription(request.description().trim());
        report.setSnapshot(snapshot);
        report.setStatus(TradeConst.REPORT_SUBMITTED);
        reportMapper.insert(report);

        auditService.log("REPORT_CREATE", "REPORT", report.getId(),
                "用户提交举报：" + CATEGORY_TEXT.getOrDefault(request.category(), request.category()),
                null, report.getReportNo());
        return toVO(report, loadUsers(List.of(report)));
    }

    public PageResult<ReportVO> mine(Long reporterId, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        List<Report> reports = reportMapper.listMine(reporterId, PageUtils.offset(p, s), s);
        List<ReportVO> records = toVOList(reports);
        return PageResult.of(records, reportMapper.countMine(reporterId), p, s);
    }

    public ReportVO detail(Long reportId, Long viewerId, boolean admin) {
        Report report = requireReport(reportId);
        if (!admin && !report.getReporterId().equals(viewerId)) {
            throw BizException.forbidden("无权查看该举报");
        }
        return toVO(report, loadUsers(List.of(report)));
    }

    @Transactional
    public ReportVO appeal(Long reportId, Long userId, String reason) {
        Report report = reportMapper.findByIdForUpdate(reportId);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        if (!TradeConst.REPORT_UPHELD.equals(report.getStatus())) {
            throw BizException.conflict("当前举报状态不支持申诉");
        }
        boolean isTargetUser = TradeConst.REPORT_TARGET_USER.equals(report.getTargetType())
                && report.getTargetId().equals(userId);
        boolean isItemOwner = false;
        if (TradeConst.REPORT_TARGET_ITEM.equals(report.getTargetType())) {
            Item item = itemMapper.findById(report.getTargetId());
            isItemOwner = item != null && item.getSellerId().equals(userId);
        }
        if (!isTargetUser && !isItemOwner) {
            throw BizException.forbidden("只有被处置方可以提交申诉");
        }
        report.setStatus(TradeConst.REPORT_PENDING_APPEAL);
        report.setAppealReason(reason.trim());
        reportMapper.update(report);
        auditService.log("REPORT_APPEAL", "REPORT", reportId, reason.trim(),
                TradeConst.REPORT_UPHELD, TradeConst.REPORT_PENDING_APPEAL);
        return toVO(report, loadUsers(List.of(report)));
    }

    /* ------------------------------ 管理端 ------------------------------ */

    public PageResult<ReportVO> adminList(String status, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        List<Report> reports = reportMapper.listAdmin(TextUtils.isBlank(status) ? null : status,
                PageUtils.offset(p, s), s);
        return PageResult.of(toVOList(reports), reportMapper.countAdmin(TextUtils.isBlank(status) ? null : status), p, s);
    }

    @Transactional
    public ReportVO claim(Long reportId, Long adminId) {
        Report report = reportMapper.findByIdForUpdate(reportId);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        if (!TradeConst.REPORT_SUBMITTED.equals(report.getStatus())) {
            throw BizException.conflict("只有待处理的举报可以领取");
        }
        report.setStatus(TradeConst.REPORT_PROCESSING);
        report.setHandlerId(adminId);
        report.setClaimedAt(LocalDateTime.now());
        reportMapper.update(report);
        auditService.log("REPORT_CLAIM", "REPORT", reportId, "管理员领取举报",
                TradeConst.REPORT_SUBMITTED, TradeConst.REPORT_PROCESSING);
        return toVO(report, loadUsers(List.of(report)));
    }

    @Transactional
    public ReportVO handle(Long reportId, Long adminId, AdminDtos.HandleReportRequest request) {
        Report report = reportMapper.findByIdForUpdate(reportId);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        if (!Set.of(TradeConst.REPORT_SUBMITTED, TradeConst.REPORT_PROCESSING,
                TradeConst.REPORT_SUPPLEMENT).contains(report.getStatus())) {
            throw BizException.conflict("当前举报状态不允许该处理");
        }
        String result = request.result();
        if (!Set.of(TradeConst.REPORT_UPHELD, TradeConst.REPORT_REJECTED, TradeConst.REPORT_SUPPLEMENT).contains(result)) {
            throw BizException.badRequest("处理结论不合法");
        }
        if (TradeConst.REPORT_UPHELD.equals(result) && TextUtils.isBlank(request.ruleBasis())) {
            throw BizException.badRequest("举报成立必须填写规则依据");
        }

        LocalDateTime now = LocalDateTime.now();
        String before = report.getStatus();
        report.setHandlerId(adminId);
        report.setHandledAt(now);
        report.setHandlingResult(request.reason().trim());
        report.setRuleBasis(TextUtils.trimTo(request.ruleBasis(), 500));
        if (report.getClaimedAt() == null) {
            report.setClaimedAt(now);
        }

        String actionSummary = null;
        if (TradeConst.REPORT_UPHELD.equals(result)) {
            report.setStatus(TradeConst.REPORT_UPHELD);
            actionSummary = applyPunishment(report, adminId, request);
            report.setActionSummary(actionSummary);
            notifyReporter(report, true);
            saveAndAudit(report, before, request.reason().trim());
        } else if (TradeConst.REPORT_REJECTED.equals(result)) {
            report.setStatus(TradeConst.REPORT_REJECTED);
            report.setActionSummary("不执行处置动作");
            notifyReporter(report, false);
            saveAndAudit(report, before, request.reason().trim());
        } else {
            report.setStatus(TradeConst.REPORT_SUPPLEMENT);
            report.setActionSummary("待举报人补充证据");
            notificationService.send(report.getReporterId(), TradeConst.NOTIFY_REPORT, "举报需要补充证据",
                    "你的举报（" + report.getReportNo() + "）需要补充证据材料，请联系管理员或重新提交详细说明。",
                    "REPORT", reportId);
            saveAndAudit(report, before, request.reason().trim());
        }
        return toVO(report, loadUsers(List.of(report)));
    }

    @Transactional
    public ReportVO close(Long reportId, Long adminId) {
        Report report = reportMapper.findByIdForUpdate(reportId);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        if (!Set.of(TradeConst.REPORT_UPHELD, TradeConst.REPORT_REJECTED,
                TradeConst.REPORT_PENDING_APPEAL).contains(report.getStatus())) {
            throw BizException.conflict("当前举报状态不能关闭");
        }
        String before = report.getStatus();
        report.setStatus(TradeConst.REPORT_CLOSED);
        report.setHandlerId(adminId);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.update(report);
        auditService.log("REPORT_CLOSE", "REPORT", reportId, "管理员关闭举报", before, TradeConst.REPORT_CLOSED);
        return toVO(report, loadUsers(List.of(report)));
    }

    @Transactional
    public ReportVO resolveAppeal(Long reportId, Long adminId, AdminDtos.ResolveAppealRequest request) {
        Report report = reportMapper.findByIdForUpdate(reportId);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        if (!TradeConst.REPORT_PENDING_APPEAL.equals(report.getStatus())) {
            throw BizException.conflict("当前举报不在申诉中");
        }
        String before = report.getStatus();
        boolean uphold = Boolean.TRUE.equals(request.uphold());
        if (uphold) {
            report.setStatus(TradeConst.REPORT_UPHELD);
            report.setHandlingResult("申诉复核：维持原处理结论。" + request.reason().trim());
        } else {
            report.setStatus(TradeConst.REPORT_REJECTED);
            report.setHandlingResult("申诉复核：撤销原处理结论。" + request.reason().trim());
            if (TradeConst.REPORT_TARGET_USER.equals(report.getTargetType())) {
                User target = userMapper.findById(report.getTargetId());
                if (target != null && !TradeConst.ACCOUNT_NORMAL.equals(target.getAccountStatus())) {
                    userMapper.updateAccountStatus(target.getId(), TradeConst.ACCOUNT_NORMAL, null, "申诉复核撤销处置");
                }
            }
        }
        report.setHandlerId(adminId);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.update(report);
        notificationService.send(report.getReporterId(), TradeConst.NOTIFY_REPORT, "举报申诉复核结果",
                "举报（" + report.getReportNo() + "）申诉复核完成：" + (uphold ? "维持原结论" : "撤销原结论"),
                "REPORT", reportId);
        auditService.log("REPORT_APPEAL_RESOLVE", "REPORT", reportId, request.reason().trim(), before, report.getStatus());
        return toVO(report, loadUsers(List.of(report)));
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    private String applyPunishment(Report report, Long adminId, AdminDtos.HandleReportRequest request) {
        String actionType = request.actionType();
        if (TradeConst.REPORT_TARGET_ITEM.equals(report.getTargetType())) {
            if (actionType != null && !actionType.isBlank() && !TradeConst.ACTION_TAKEDOWN.equals(actionType)) {
                throw BizException.badRequest("商品举报只能执行违规下架");
            }
            Item item = itemMapper.findById(report.getTargetId());
            if (item == null) {
                return "商品已不存在，未执行下架";
            }
            if (TradeConst.ITEM_VIOLATED.equals(item.getStatus())) {
                return "商品已处于违规下架状态";
            }
            itemService.takedown(item.getId(), "举报成立：" + request.reason().trim(), request.ruleBasis(), adminId);
            return "违规下架商品《" + item.getTitle() + "》";
        }
        if (TradeConst.REPORT_TARGET_USER.equals(report.getTargetType())) {
            String type = (actionType == null || actionType.isBlank()) ? TradeConst.ACTION_WARN : actionType;
            if (!Set.of(TradeConst.ACTION_WARN, TradeConst.ACTION_LIMIT, TradeConst.ACTION_BAN).contains(type)) {
                throw BizException.badRequest("用户举报只能执行警告、限权或封禁");
            }
            sanctionService.apply(report.getTargetId(), type, request.reason().trim(), request.ruleBasis(),
                    request.durationDays(), report.getId());
            return switch (type) {
                case TradeConst.ACTION_LIMIT -> "限制账户权限";
                case TradeConst.ACTION_BAN -> "封禁账户";
                default -> "警告账户";
            };
        }
        return "消息举报已记录，保留原始证据";
    }

    private void notifyReporter(Report report, boolean upheld) {
        String statusText = upheld ? "举报成立" : "举报不成立";
        String content = "你提交的举报（" + report.getReportNo() + "）处理结果：" + statusText
                + "。处理说明：" + report.getHandlingResult();
        if (upheld && report.getActionSummary() != null) {
            content += "；处置动作：" + report.getActionSummary();
        }
        notificationService.send(report.getReporterId(), TradeConst.NOTIFY_REPORT, "举报处理结果",
                content, "REPORT", report.getId());
    }

    private void saveAndAudit(Report report, String before, String reason) {
        reportMapper.update(report);
        auditService.log("REPORT_HANDLE", "REPORT", report.getId(), reason, before, report.getStatus());
    }

    private Report requireReport(Long id) {
        Report report = reportMapper.findById(id);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        return report;
    }

    private String buildSnapshot(Long reporterId, String targetType, Long targetId) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("reporterId", reporterId);
            node.put("capturedAt", LocalDateTime.now().toString());
            if (TradeConst.REPORT_TARGET_ITEM.equals(targetType)) {
                Item item = itemMapper.findById(targetId);
                if (item == null) {
                    throw BizException.notFound("被举报的商品不存在");
                }
                node.put("itemId", item.getId());
                node.put("title", item.getTitle());
                node.put("price", item.getPrice().toPlainString());
                node.put("status", item.getStatus());
                node.put("sellerId", item.getSellerId());
                node.put("summary", "商品：" + item.getTitle() + "（¥" + item.getPrice().toPlainString()
                        + "，" + TradeConst.itemStatusText(item.getStatus()) + "）");
            } else if (TradeConst.REPORT_TARGET_USER.equals(targetType)) {
                User user = userMapper.findById(targetId);
                if (user == null) {
                    throw BizException.notFound("被举报的用户不存在");
                }
                if (user.getId().equals(reporterId)) {
                    throw BizException.badRequest("不能举报自己");
                }
                node.put("userId", user.getId());
                node.put("nickname", user.getNickname());
                node.put("summary", "用户：" + user.getNickname());
            } else {
                Message message = messageMapper.findById(targetId);
                if (message == null) {
                    throw BizException.notFound("被举报的消息不存在");
                }
                node.put("messageId", message.getId());
                node.put("conversationId", message.getConversationId());
                node.put("senderId", message.getSenderId());
                node.put("content", message.getContent());
                String preview = message.getContent().length() > 30
                        ? message.getContent().substring(0, 30) + "…"
                        : message.getContent();
                node.put("summary", "消息：" + preview);
            }
            return objectMapper.writeValueAsString(node);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("生成举报快照失败", ex);
            return "{\"summary\":\"对象快照生成失败\"}";
        }
    }

    private List<ReportVO> toVOList(List<Report> reports) {
        if (reports.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, User> users = loadUsers(reports);
        List<ReportVO> result = new ArrayList<>();
        for (Report report : reports) {
            result.add(toVO(report, users));
        }
        return result;
    }

    private Map<Long, User> loadUsers(List<Report> reports) {
        Set<Long> ids = new HashSet<>();
        reports.forEach(report -> {
            ids.add(report.getReporterId());
            if (report.getHandlerId() != null) {
                ids.add(report.getHandlerId());
            }
        });
        Map<Long, User> map = new HashMap<>();
        if (!ids.isEmpty()) {
            for (User user : userMapper.findByIds(ids)) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    private ReportVO toVO(Report report, Map<Long, User> users) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReportNo(report.getReportNo());
        vo.setTargetType(report.getTargetType());
        vo.setTargetId(report.getTargetId());
        vo.setTargetTypeText(TARGET_TEXT.getOrDefault(report.getTargetType(), report.getTargetType()));
        vo.setTargetSummary(extractSummary(report.getSnapshot()));
        vo.setCategory(report.getCategory());
        vo.setCategoryText(CATEGORY_TEXT.getOrDefault(report.getCategory(), report.getCategory()));
        vo.setDescription(report.getDescription());
        vo.setStatus(report.getStatus());
        vo.setStatusText(TradeConst.reportStatusText(report.getStatus()));
        vo.setHandlingResult(report.getHandlingResult());
        vo.setRuleBasis(report.getRuleBasis());
        vo.setActionSummary(report.getActionSummary());
        vo.setAppealReason(report.getAppealReason());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setHandledAt(report.getHandledAt());
        User reporter = users.get(report.getReporterId());
        vo.setReporterNickname(reporter == null ? null : reporter.getNickname());
        User handler = report.getHandlerId() == null ? null : users.get(report.getHandlerId());
        vo.setHandlerName(handler == null ? null : handler.getNickname());
        return vo;
    }

    private String extractSummary(String snapshot) {
        if (TextUtils.isBlank(snapshot)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot);
            JsonNode summary = node.get("summary");
            return summary == null ? null : summary.asText();
        } catch (Exception ex) {
            return null;
        }
    }
}
