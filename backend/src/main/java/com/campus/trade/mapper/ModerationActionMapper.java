package com.campus.trade.mapper;

import com.campus.trade.entity.ModerationAction;
import com.campus.trade.vo.ModerationActionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ModerationActionMapper {

    int insert(ModerationAction action);

    List<ModerationActionVO> listByTarget(@Param("targetType") String targetType,
                                          @Param("targetId") Long targetId);

    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Map<String, Object>> countByTypeBetween(@Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
}
