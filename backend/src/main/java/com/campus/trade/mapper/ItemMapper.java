package com.campus.trade.mapper;

import com.campus.trade.entity.Item;
import com.campus.trade.vo.AdminItemVO;
import com.campus.trade.vo.ItemCardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ItemMapper {

    int insert(Item item);

    int update(Item item);

    Item findById(Long id);

    Item findByIdForUpdate(Long id);

    List<Item> findByIds(@Param("ids") Collection<Long> ids);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("rejectReason") String rejectReason,
                     @Param("auditBy") Long auditBy,
                     @Param("auditAt") java.time.LocalDateTime auditAt,
                     @Param("listedAt") java.time.LocalDateTime listedAt);

    int increaseViewCount(Long id);

    int updateFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

    int deleteById(Long id);

    List<ItemCardVO> search(@Param("keyword") String keyword,
                            @Param("categoryId") Long categoryId,
                            @Param("minPrice") BigDecimal minPrice,
                            @Param("maxPrice") BigDecimal maxPrice,
                            @Param("condition") String condition,
                            @Param("area") String area,
                            @Param("sort") String sort,
                            @Param("offset") int offset,
                            @Param("size") int size);

    long countSearch(@Param("keyword") String keyword,
                     @Param("categoryId") Long categoryId,
                     @Param("minPrice") BigDecimal minPrice,
                     @Param("maxPrice") BigDecimal maxPrice,
                     @Param("condition") String condition,
                     @Param("area") String area);

    List<ItemCardVO> findBySeller(@Param("sellerId") Long sellerId,
                                  @Param("status") String status,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    long countBySeller(@Param("sellerId") Long sellerId, @Param("status") String status);

    List<ItemCardVO> findFavorites(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    long countFavorites(Long userId);

    ItemCardVO findCardById(Long id);

    List<AdminItemVO> adminSearch(@Param("status") String status,
                                  @Param("keyword") String keyword,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    long adminCount(@Param("status") String status, @Param("keyword") String keyword);

    AdminItemVO findAdminById(Long id);

    long countByStatus(String status);

    long countCreatedBetween(@Param("from") java.time.LocalDateTime from,
                             @Param("to") java.time.LocalDateTime to);

    List<java.util.Map<String, Object>> countByCategoryBetween(@Param("from") java.time.LocalDateTime from,
                                                               @Param("to") java.time.LocalDateTime to);
}
