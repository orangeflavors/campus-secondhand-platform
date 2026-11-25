package com.campus.trade.mapper;

import com.campus.trade.entity.Conversation;
import com.campus.trade.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper {

    int insert(Conversation conversation);

    Conversation findById(Long id);

    Conversation findByItemAndBuyer(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId);

    List<ConversationVO> listByUser(Long userId);

    ConversationVO findVOById(@Param("id") Long id, @Param("userId") Long userId);

    int updateLastMessage(@Param("id") Long id, @Param("lastMessageAt") LocalDateTime lastMessageAt);
}
