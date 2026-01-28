package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.model.dto.PushBatchDTO;
import com.stalab.e_ink_billboard_backend.model.dto.PushImageDTO;
import com.stalab.e_ink_billboard_backend.model.dto.PushVideoDTO;
import com.stalab.e_ink_billboard_backend.model.vo.ContentPushVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.service.push.PushService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 内容推送Controller
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;
    private final JwtUtils jwtUtils;

    public PushController(PushService pushService, JwtUtils jwtUtils) {
        this.pushService = pushService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 推送图片到设备
     * POST /api/push/image
     */
    @PostMapping("/image")
    public Response<Void> pushImage(@Valid @RequestBody PushImageDTO dto,
                                    @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long userId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 执行推送
        try {
            pushService.pushImage(dto.getDeviceId(), dto.getImageId(), userId, userRole, dto.getVerificationCode());
            return Response.<Void>builder()
                    .code(200)
                    .info("推送成功")
                    .build();
        } catch (Exception e) {
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 推送视频到设备
     * POST /api/push/video
     */
    @PostMapping("/video")
    public Response<Void> pushVideo(@Valid @RequestBody PushVideoDTO dto,
                                     @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long userId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 执行推送
        try {
            pushService.pushVideo(dto.getDeviceId(), dto.getVideoId(), userId, userRole, dto.getVerificationCode());
            return Response.<Void>builder()
                    .code(200)
                    .info("推送成功")
                    .build();
        } catch (Exception e) {
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 批量推送到多个设备
     * POST /api/push/batch
     */
    @PostMapping("/batch")
    public Response<Void> pushBatch(@Valid @RequestBody PushBatchDTO dto,
                                     @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long userId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 执行批量推送
        try {
            // 批量推送暂不支持验证码，因为逻辑比较复杂（一次消耗一个还是？）
            // 假设批量推送只有管理员或特定用户能用，或者暂时不加验证码限制（或默认不传）
            // 如果需要，可以在DTO里加verificationCode并传下去
            pushService.pushBatch(dto.getDeviceIds(), dto.getContentId(), dto.getContentType(), userId, userRole);
            return Response.<Void>builder()
                    .code(200)
                    .info("批量推送成功")
                    .build();
        } catch (Exception e) {
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询推送历史（分页）
     * GET /api/push/history
     */
    @GetMapping("/history")
    public Response<PageResult<ContentPushVO>> getPushHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size,
            @RequestParam(value = "deviceId", required = false) Long deviceId,
            @RequestParam(value = "userId", required = false) Long userId) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<PageResult<ContentPushVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long currentUserId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 查询推送历史
        PageResult<ContentPushVO> result = pushService.getPushHistory(
                current, size, deviceId, userId, currentUserId, userRole);

        return Response.<PageResult<ContentPushVO>>builder()
                .code(200)
                .info("查询成功")
                .data(result)
                .build();
    }
}
