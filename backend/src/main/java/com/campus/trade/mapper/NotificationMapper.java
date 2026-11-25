package com.campus.trade.mapper;

import com.campus.trade.entity.Notification;
import com.campus.trade.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    List<NotificationVO> list(@Param("userId") Long userId,
                              @Param("unread") boolean unread,
                              @Param("offset") int offset,
                              @Param("size") int size);

    long count(@Param("userId") Long userId, @Param("unread") boolean unread);

    long countUnread(Long userId);

    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    int markAllRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
