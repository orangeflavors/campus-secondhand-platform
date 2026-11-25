package com.campus.trade.mapper;

import com.campus.trade.entity.Review;
import com.campus.trade.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewMapper {

    int insert(Review review);

    Review findByOrderAndAuthor(@Param("orderId") Long orderId, @Param("authorId") Long authorId);

    List<ReviewVO> listByOrder(Long orderId);

    List<ReviewVO> listByTarget(@Param("targetId") Long targetId,
                                @Param("offset") int offset,
                                @Param("size") int size);

    long countByTarget(Long targetId);

    double avgRatingByTarget(Long targetId);

    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    double avgRatingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
