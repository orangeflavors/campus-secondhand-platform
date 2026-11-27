package com.campus.trade.mapper;

import com.campus.trade.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(TradeOrder order);

    TradeOrder findById(Long id);

    TradeOrder findByIdForUpdate(Long id);

    List<TradeOrder> findByIds(@Param("ids") Collection<Long> ids);

    TradeOrder findActiveByItem(Long itemId);

    TradeOrder findPendingByItemAndBuyer(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId);

    List<TradeOrder> findOtherPendingForUpdate(@Param("itemId") Long itemId, @Param("excludeId") Long excludeId);

    long countPendingByItem(Long itemId);

    long countByItem(Long itemId);

    List<TradeOrder> listByUser(@Param("userId") Long userId,
                                @Param("role") String role,
                                @Param("status") String status);

    List<TradeOrder> listDisputes(@Param("offset") int offset, @Param("size") int size);

    long countDisputes();

    int update(TradeOrder order);

    List<TradeOrder> findExpiredPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countCompletedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countCancelledBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countActive();

    long countCompletedBySeller(Long sellerId);

    List<java.util.Map<String, Object>> trendCreated(@Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    List<java.util.Map<String, Object>> trendCompleted(@Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to);
}
