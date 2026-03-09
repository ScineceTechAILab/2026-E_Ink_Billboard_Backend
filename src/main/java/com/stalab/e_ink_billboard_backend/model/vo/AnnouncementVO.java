package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公告信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementVO {

    /**
     * 公告内容
     */
    private String content;

    /**
     * 更新时间
     */
    private String updatedAt;
}
