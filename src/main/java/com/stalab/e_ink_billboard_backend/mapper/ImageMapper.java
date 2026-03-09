package com.stalab.e_ink_billboard_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<Image> {

    /**
     * 查询指定时间范围内的活跃用户昵称列表（去重，过滤管理员）
     */
    @Select("SELECT DISTINCT u.nickname FROM sys_image i " +
            "JOIN sys_user u ON i.user_id = u.id " +
            "WHERE i.create_time BETWEEN #{startTime} AND #{endTime} " +
            "AND u.role != 'ADMIN'")
    List<String> selectActiveUserNicknames(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
