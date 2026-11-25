package com.campus.trade.mapper;

import com.campus.trade.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface UserMapper {

    User findById(Long id);

    User findByUsername(String username);

    User findByStudentNo(String studentNo);

    int insert(User user);

    int updateProfile(User user);

    int updateCampusAuth(User user);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateAccountStatus(@Param("id") Long id,
                            @Param("accountStatus") String accountStatus,
                            @Param("limitedUntil") LocalDateTime limitedUntil,
                            @Param("statusReason") String statusReason);

    List<User> findByIds(@Param("ids") Collection<Long> ids);

    List<User> search(@Param("keyword") String keyword,
                      @Param("accountStatus") String accountStatus,
                      @Param("authStatus") String authStatus,
                      @Param("offset") int offset,
                      @Param("size") int size);

    long countSearch(@Param("keyword") String keyword,
                     @Param("accountStatus") String accountStatus,
                     @Param("authStatus") String authStatus);

    long countAll();

    long countPublishedItems(Long userId);

    long countSoldItems(Long userId);
}
