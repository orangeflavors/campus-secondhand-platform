package com.campus.trade.mapper;

import com.campus.trade.entity.HandoverLog;
import com.campus.trade.entity.HandoverRecord;
import com.campus.trade.vo.HandoverLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HandoverMapper {

    HandoverRecord findByOrderId(Long orderId);

    List<HandoverRecord> findByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    int insert(HandoverRecord record);

    int update(HandoverRecord record);

    int insertLog(HandoverLog log);

    List<HandoverLogVO> listLogs(Long orderId);
}
