
package com.stalab.e_ink_billboard_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核记录Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
