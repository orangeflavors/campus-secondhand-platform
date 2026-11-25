package com.campus.trade.mapper;

import com.campus.trade.entity.ItemImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ItemImageMapper {

    int insertBatch(@Param("images") List<ItemImage> images);

    int deleteByItemId(Long itemId);

    List<ItemImage> findByItemId(Long itemId);

    List<ItemImage> findByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
