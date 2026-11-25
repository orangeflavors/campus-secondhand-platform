package com.campus.trade.mapper;

import com.campus.trade.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    int insert(Report report);

    Report findById(Long id);

    Report findByIdForUpdate(Long id);

    int update(Report report);

    List<Report> listMine(@Param("reporterId") Long reporterId,
                          @Param("offset") int offset,
                          @Param("size") int size);

    long countMine(Long reporterId);

    List<Report> listAdmin(@Param("status") String status,
                           @Param("offset") int offset,
                           @Param("size") int size);

    long countAdmin(@Param("status") String status);

    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Map<String, Object>> countByStatusBetween(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}
