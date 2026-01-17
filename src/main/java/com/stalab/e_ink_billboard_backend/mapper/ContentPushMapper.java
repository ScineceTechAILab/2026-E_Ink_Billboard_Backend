package com.stalab.e_ink_billboard_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.ContentPush;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容推送记录Mapper接口
 */
@Mapper
public interface ContentPushMapper extends BaseMapper<ContentPush> {
}
