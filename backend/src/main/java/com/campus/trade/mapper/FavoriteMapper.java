package com.campus.trade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper {

    int insert(@Param("userId") Long userId, @Param("itemId") Long itemId);

    int delete(@Param("userId") Long userId, @Param("itemId") Long itemId);

    int exists(@Param("userId") Long userId, @Param("itemId") Long itemId);
}
