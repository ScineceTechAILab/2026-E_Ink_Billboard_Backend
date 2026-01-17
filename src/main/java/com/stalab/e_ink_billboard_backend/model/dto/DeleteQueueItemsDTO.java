package com.stalab.e_ink_billboard_backend.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 删除队列项DTO
 */
@Data
public class DeleteQueueItemsDTO {
    /**
     * 要删除的推送记录ID列表
     */
    @NotEmpty(message = "推送记录ID列表不能为空")
    private List<Long> pushIds;
}
