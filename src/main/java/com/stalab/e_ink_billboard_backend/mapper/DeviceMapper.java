package com.stalab.e_ink_billboard_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备Mapper接口
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
