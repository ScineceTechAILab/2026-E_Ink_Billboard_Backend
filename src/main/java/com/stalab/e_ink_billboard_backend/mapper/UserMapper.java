package com.stalab.e_ink_billboard_backend.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 用户
 * @Version: 1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
