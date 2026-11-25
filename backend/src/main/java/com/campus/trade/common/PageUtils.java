package com.campus.trade.common;

/**
 * 分页参数规范化。
 */
public final class PageUtils {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private PageUtils() {
    }

    public static int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int size(Integer size, int defaultSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static int offset(int page, int size) {
        return (page - 1) * size;
    }
}
