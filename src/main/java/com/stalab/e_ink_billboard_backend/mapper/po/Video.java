package com.stalab.e_ink_billboard_backend.mapper.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String fileName;
    private Long fileSize; // 原视频大小

    private String originalUrl;  // 原视频 (MP4)
    private String processedUrl; // 处理后的 (BIN)

    private Integer duration;    // 时长 (秒，可选)
    private Integer frameCount;  // 总帧数 (可选)

    private String auditStatus;  // PENDING, PASSED, REJECTED

    private String processingStatus; // PROCESSING, SUCCESS, FAILED
    private String failReason;

    private LocalDateTime createTime;

}
