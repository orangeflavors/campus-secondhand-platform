package com.campus.trade.common;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 文本与脱敏工具。
 */
public final class TextUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private TextUtils() {
    }

    /**
     * 转义 LIKE 通配符（以 ! 作为 ESCAPE 字符），防止用户输入被解释为通配符（FR-011 安全处理）。
     */
    public static String escapeLike(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public static String likePattern(String input) {
        String escaped = escapeLike(input == null ? "" : input.trim());
        return "%" + escaped + "%";
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? null : "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email == null ? null : "***";
        }
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        if (name.length() <= 1) {
            return "*" + email.substring(at);
        }
        return name.charAt(0) + "***" + email.substring(at);
    }

    public static String maskStudentNo(String studentNo) {
        if (studentNo == null || studentNo.length() <= 4) {
            return studentNo == null ? null : "****";
        }
        return studentNo.substring(0, 4) + "****" + studentNo.substring(studentNo.length() - 2);
    }

    /**
     * 生成带时间戳的业务编号，如 O20261015123045AB12。
     */
    public static String generateNo(String prefix) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        StringBuilder sb = new StringBuilder(prefix).append(time);
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String randomToken(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(ThreadLocalRandom.current().nextInt(16)));
        }
        return sb.toString();
    }

    public static String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
