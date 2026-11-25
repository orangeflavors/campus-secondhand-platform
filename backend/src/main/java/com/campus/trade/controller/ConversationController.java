package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.security.UserPrincipal;
import com.campus.trade.service.ConversationService;
import com.campus.trade.vo.ConversationVO;
import com.campus.trade.vo.MessageVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    public record CreateConversationRequest(@NotNull(message = "请选择商品") Long itemId) {
    }

    public record SendMessageRequest(
            @NotBlank(message = "消息内容不能为空")
            @Size(max = 1000, message = "消息内容不能超过 1000 个字符")
            String content) {
    }

    @PostMapping
    public ApiResponse<ConversationVO> create(@RequestBody CreateConversationRequest request) {
        UserPrincipal principal = SecurityUtils.requireMessaging();
        return ApiResponse.ok(conversationService.create(request.itemId(), principal.id()));
    }

    @GetMapping
    public ApiResponse<List<ConversationVO>> list() {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(conversationService.list(SecurityUtils.currentId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationVO> detail(@PathVariable Long id) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(conversationService.detail(id, SecurityUtils.currentId()));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<MessageVO>> messages(@PathVariable Long id,
                                                 @RequestParam(required = false) Long afterId) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(conversationService.messages(id, SecurityUtils.currentId(), afterId));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<MessageVO> send(@PathVariable Long id,
                                       @RequestBody SendMessageRequest request) {
        UserPrincipal principal = SecurityUtils.requireMessaging();
        return ApiResponse.ok(conversationService.send(id, principal.id(), request.content()));
    }
}
