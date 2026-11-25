package com.campus.trade.mapper;

import com.campus.trade.entity.AuditLog;
import com.campus.trade.vo.AuditLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditLogMapper {

    int insert(AuditLog log);

    List<AuditLogVO> list(@Param("action") String action,
                          @Param("offset") int offset,
                          @Param("size") int size);

    long count(@Param("action") String action);
}
