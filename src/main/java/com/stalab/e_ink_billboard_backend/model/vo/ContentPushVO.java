package com.stalab.e_ink_billboard_backend.model.vo;

import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.PushStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 内容推送记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPushVO {
    /**
     * 推送记录ID
     */
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 内容ID
     */
    private Long contentId;

    /**
     * 内容类型
     */
    private ContentType contentType;

    /**
     * 推送状态
     */
    private PushStatus pushStatus;

    /**
     * 推送时间
     */
    private LocalDateTime pushTime;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 错误信息
     */
    private String errorMessage;
}
