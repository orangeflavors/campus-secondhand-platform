package com.campus.trade.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端运营统计（默认聚合、去标识化）。
 */
@Data
public class StatsVO {
    private String from;
    private String to;
    private ItemStats items;
    private OrderStats orders;
    private ReviewStats reviews;
    private ReportStats reports;
    private ActionStats actions;
    private List<TrendPoint> trend;

    @Data
    public static class ItemStats {
        private long published;
        private long onSale;
        private long sold;
        private long pendingAudit;
        private List<NameCount> byCategory;
    }

    @Data
    public static class OrderStats {
        private long total;
        private long completed;
        private long cancelled;
        private long active;
        private double completionRate;
    }

    @Data
    public static class ReviewStats {
        private long count;
        private double avgRating;
    }

    @Data
    public static class ReportStats {
        private long total;
        private List<StatusCount> byStatus;
    }

    @Data
    public static class ActionStats {
        private long total;
        private List<StatusCount> byType;
    }

    @Data
    public static class NameCount {
        private String name;
        private long count;

        public NameCount() {
        }

        public NameCount(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    @Data
    public static class StatusCount {
        private String status;
        private String label;
        private long count;

        public StatusCount() {
        }

        public StatusCount(String status, String label, long count) {
            this.status = status;
            this.label = label;
            this.count = count;
        }
    }

    @Data
    public static class TrendPoint {
        private String date;
        private long published;
        private long completed;

        public TrendPoint() {
        }

        public TrendPoint(String date, long published, long completed) {
            this.date = date;
            this.published = published;
            this.completed = completed;
        }
    }
}
