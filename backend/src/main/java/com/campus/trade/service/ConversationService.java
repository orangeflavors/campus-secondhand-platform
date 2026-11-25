package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.entity.Conversation;
import com.campus.trade.entity.Item;
import com.campus.trade.entity.Message;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ConversationMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.MessageMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.ConversationVO;
import com.campus.trade.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 基于商品的咨询会话（FR-015 / FR-016）。
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final Set<String> CHATTABLE = Set.of(
            TradeConst.ITEM_ON_SALE, TradeConst.ITEM_LOCKED, TradeConst.ITEM_SOLD);

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    @Transactional
    public ConversationVO create(Long itemId, Long buyerId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw BizException.notFound("商品不存在");
        }
        if (item.getSellerId().equals(buyerId)) {
            throw BizException.conflict("不能与自己发布的商品发起咨询");
        }
        if (!CHATTABLE.contains(item.getStatus())) {
            throw BizException.conflict("商品当前状态不支持咨询");
        }
        User buyer = userMapper.findById(buyerId);
        if (buyer == null || !TradeConst.AUTH_VERIFIED.equals(buyer.getAuthStatus())) {
            throw BizException.forbidden("请先完成校园身份认证");
        }
        Conversation existing = conversationMapper.findByItemAndBuyer(itemId, buyerId);
        if (existing != null) {
            return conversationMapper.findVOById(existing.getId(), buyerId);
        }
        Conversation conversation = new Conversation();
        conversation.setItemId(itemId);
        conversation.setBuyerId(buyerId);
        conversation.setSellerId(item.getSellerId());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversationMapper.findVOById(conversation.getId(), buyerId);
    }

    public List<ConversationVO> list(Long userId) {
        return conversationMapper.listByUser(userId);
    }

    public ConversationVO detail(Long conversationId, Long userId) {
        Conversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, userId);
        return conversationMapper.findVOById(conversationId, userId);
    }

    public List<MessageVO> messages(Long conversationId, Long userId, Long afterId) {
        Conversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, userId);
        return messageMapper.listByConversation(conversationId, afterId, userId);
    }

    @Transactional
    public MessageVO send(Long conversationId, Long senderId, String content) {
        Conversation conversation = requireConversation(conversationId);
        requireParticipant(conversation, senderId);
        User sender = userMapper.findById(senderId);
        if (sender == null) {
            throw BizException.unauthorized("登录状态异常");
        }
        if (TradeConst.ACCOUNT_BANNED.equals(sender.getAccountStatus())) {
            throw BizException.forbidden("账户已被封禁，无法发送消息");
        }
        if (TextUtils.isBlank(content)) {
            throw BizException.badRequest("消息内容不能为空");
        }
        String text = content.trim();
        if (text.length() > 1000) {
            throw BizException.badRequest("消息内容不能超过 1000 个字符");
        }
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(text);
        messageMapper.insert(message);
        conversationMapper.updateLastMessage(conversationId, LocalDateTime.now());

        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setConversationId(conversationId);
        vo.setSenderId(senderId);
        vo.setSenderNickname(sender.getNickname());
        vo.setSenderAvatarUrl(sender.getAvatarUrl());
        vo.setContent(text);
        vo.setMine(true);
        vo.setCreatedAt(LocalDateTime.now());
        return vo;
    }

    private Conversation requireConversation(Long id) {
        Conversation conversation = conversationMapper.findById(id);
        if (conversation == null) {
            throw BizException.notFound("会话不存在");
        }
        return conversation;
    }

    private void requireParticipant(Conversation conversation, Long userId) {
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw BizException.forbidden("只有会话双方可以访问该会话");
        }
    }
}
