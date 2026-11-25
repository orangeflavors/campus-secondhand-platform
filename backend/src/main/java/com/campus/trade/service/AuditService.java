package com.campus.trade.service;

import com.campus.trade.entity.AuditLog;
import com.campus.trade.mapper.AuditLogMapper;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志：记录操作者、时间、对象、理由与前后值（BR-16 / FR-031）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void log(String action, String targetType, Long targetId, String reason, Object before, Object after) {
        try {
            AuditLog auditLog = new AuditLog();
            try {
                UserPrincipal principal = SecurityUtils.current();
                auditLog.setOperatorId(principal.id());
                auditLog.setOperatorName(principal.username());
            } catch (Exception ignored) {
                auditLog.setOperatorName("SYSTEM");
            }
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setReason(reason);
            auditLog.setBeforeValue(toJson(before));
            auditLog.setAfterValue(toJson(after));
            auditLog.setIp(currentIp());
            auditLogMapper.insert(auditLog);
        } catch (Exception ex) {
            // 审计写入失败不应导致核心业务回滚，但必须留下错误日志
            log.error("审计日志写入失败 action={} target={}:{}", action, targetType, targetId, ex);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String currentIp() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // 定时任务等非请求上下文
        }
        return null;
    }
}
