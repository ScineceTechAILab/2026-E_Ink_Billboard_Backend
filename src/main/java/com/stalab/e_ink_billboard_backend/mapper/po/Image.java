package com.stalab.e_ink_billboard_backend.mapper.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_image")
public class Image {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;        // 谁传的
    private String fileName;    // 文件名
    private Long fileSize;      // 大小

    private String originalUrl;  // 原图 (给管理员审核看)
    private String processedUrl; // 结果图 (给墨水屏下载用)

    private String md5;          // 防重校验码

    /**
     * 审核状态
     */
    private AuditStatus auditStatus;
    private String auditReason;

    private LocalDateTime createTime;
}
