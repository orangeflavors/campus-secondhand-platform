package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.ModerationActionMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ReportMapper;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营聚合统计（FR-030 / BR-15）：默认聚合、去标识化，不展示个人明细。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ItemMapper itemMapper;
    private final OrderMapper orderMapper;
    private final ReviewMapper reviewMapper;
    private final ReportMapper reportMapper;
    private final ModerationActionMapper moderationActionMapper;

    public StatsVO build(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw BizException.badRequest("开始日期不能晚于结束日期");
        }
        if (start.plusDays(366).isBefore(end)) {
            throw BizException.badRequest("统计区间不能超过 366 天");
        }
        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();

        StatsVO vo = new StatsVO();
        vo.setFrom(start.format(DATE));
        vo.setTo(end.format(DATE));

        StatsVO.ItemStats items = new StatsVO.ItemStats();
        items.setPublished(itemMapper.countCreatedBetween(fromTime, toTime));
        items.setOnSale(itemMapper.countByStatus(TradeConst.ITEM_ON_SALE));
        items.setSold(itemMapper.countByStatus(TradeConst.ITEM_SOLD));
        items.setPendingAudit(itemMapper.countByStatus(TradeConst.ITEM_PENDING));
        List<StatsVO.NameCount> byCategory = new ArrayList<>();
        for (Map<String, Object> row : itemMapper.countByCategoryBetween(fromTime, toTime)) {
            byCategory.add(new StatsVO.NameCount(String.valueOf(row.get("name")), number(row.get("count"))));
        }
        items.setByCategory(byCategory);
        vo.setItems(items);

        StatsVO.OrderStats orders = new StatsVO.OrderStats();
        long total = orderMapper.countCreatedBetween(fromTime, toTime);
        long completed = orderMapper.countCompletedBetween(fromTime, toTime);
        orders.setTotal(total);
        orders.setCompleted(completed);
        orders.setCancelled(orderMapper.countCancelledBetween(fromTime, toTime));
        orders.setActive(orderMapper.countActive());
        orders.setCompletionRate(total == 0 ? 0 : Math.round(completed * 1000.0 / total) / 10.0);
        vo.setOrders(orders);

        StatsVO.ReviewStats reviews = new StatsVO.ReviewStats();
        reviews.setCount(reviewMapper.countBetween(fromTime, toTime));
        reviews.setAvgRating(Math.round(reviewMapper.avgRatingBetween(fromTime, toTime) * 10.0) / 10.0);
        vo.setReviews(reviews);

        StatsVO.ReportStats reports = new StatsVO.ReportStats();
        reports.setTotal(reportMapper.countCreatedBetween(fromTime, toTime));
        Map<String, Long> reportStatus = toCountMap(reportMapper.countByStatusBetween(fromTime, toTime), "status");
        List<StatsVO.StatusCount> reportByStatus = new ArrayList<>();
        for (String status : List.of(TradeConst.REPORT_SUBMITTED, TradeConst.REPORT_PROCESSING,
                TradeConst.REPORT_SUPPLEMENT, TradeConst.REPORT_UPHELD, TradeConst.REPORT_REJECTED,
                TradeConst.REPORT_PENDING_APPEAL, TradeConst.REPORT_CLOSED)) {
            reportByStatus.add(new StatsVO.StatusCount(status, TradeConst.reportStatusText(status),
                    reportStatus.getOrDefault(status, 0L)));
        }
        reports.setByStatus(reportByStatus);
        vo.setReports(reports);

        StatsVO.ActionStats actions = new StatsVO.ActionStats();
        actions.setTotal(moderationActionMapper.countBetween(fromTime, toTime));
        Map<String, Long> actionTypes = toCountMap(moderationActionMapper.countByTypeBetween(fromTime, toTime), "action_type");
        List<StatsVO.StatusCount> byType = new ArrayList<>();
        Map<String, String> actionLabels = new LinkedHashMap<>();
        actionLabels.put(TradeConst.ACTION_TAKEDOWN, "违规下架");
        actionLabels.put(TradeConst.ACTION_WARN, "警告");
        actionLabels.put(TradeConst.ACTION_LIMIT, "限权");
        actionLabels.put(TradeConst.ACTION_BAN, "封禁");
        actionLabels.put(TradeConst.ACTION_UNBAN, "解封");
        actionLabels.forEach((type, label) ->
                byType.add(new StatsVO.StatusCount(type, label, actionTypes.getOrDefault(type, 0L))));
        actions.setByType(byType);
        vo.setActions(actions);

        vo.setTrend(buildTrend(start, end, fromTime, toTime));
        return vo;
    }

    private List<StatsVO.TrendPoint> buildTrend(LocalDate start, LocalDate end,
                                                LocalDateTime fromTime, LocalDateTime toTime) {
        Map<String, Long> created = new HashMap<>();
        for (Map<String, Object> row : orderMapper.trendCreated(fromTime, toTime)) {
            created.put(String.valueOf(row.get("d")), number(row.get("c")));
        }
        Map<String, Long> completed = new HashMap<>();
        for (Map<String, Object> row : orderMapper.trendCompleted(fromTime, toTime)) {
            completed.put(String.valueOf(row.get("d")), number(row.get("c")));
        }
        List<StatsVO.TrendPoint> trend = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = cursor.format(DATE);
            trend.add(new StatsVO.TrendPoint(key,
                    created.getOrDefault(key, 0L),
                    completed.getOrDefault(key, 0L)));
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows, String keyField) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object key = row.get(keyField);
            if (key != null) {
                map.put(String.valueOf(key), number(row.get("count")));
            }
        }
        return map;
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || TextUtils.isBlank(String.valueOf(value))) {
            return 0;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
