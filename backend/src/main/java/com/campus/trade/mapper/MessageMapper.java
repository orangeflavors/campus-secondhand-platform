package com.campus.trade.mapper;

import com.campus.trade.entity.Message;
import com.campus.trade.vo.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insert(Message message);

    Message findById(Long id);

    List<MessageVO> listByConversation(@Param("conversationId") Long conversationId,
                                       @Param("afterId") Long afterId,
                                       @Param("currentUserId") Long currentUserId);
}
