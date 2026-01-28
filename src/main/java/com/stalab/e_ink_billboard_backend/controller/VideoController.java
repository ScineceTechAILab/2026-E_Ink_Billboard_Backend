package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.model.vo.VideoVO;
import com.stalab.e_ink_billboard_backend.service.media.VideoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoController {


    private final VideoService videoService;

    private final VideoMapper videoMapper; // 用于简单的查状态

    private final JwtUtils jwtUtils;

    public VideoController(VideoService videoService, VideoMapper videoMapper, JwtUtils jwtUtils) {
        this.videoService = videoService;
        this.videoMapper = videoMapper;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 上传接口：返回 VideoID
     */
    @PostMapping("/upload")
    public Response<Long> upload(@RequestParam("file") MultipartFile file,
                                 @RequestHeader("Authorization") String token) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Long>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        Long userId = jwtUtils.getUserId(token);

        Long videoId = videoService.initUpload(file, userId);

        return Response.<Long>builder()
                .code(200)
                .info("上传成功")
                .data(videoId)
                .build();
    }

    /**
     * 轮询接口：查询处理状态
     */
    @GetMapping("/status/{id}")
    public Response<Map<String, Object>> checkStatus(@PathVariable Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            return Response.<Map<String, Object>>builder()
                    .code(404)
                    .info("视频不存在")
                    .build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", video.getProcessingStatus()); // PROCESSING, SUCCESS, FAILED
        data.put("processedUrl", video.getProcessedUrl());
        data.put("failReason", video.getFailReason());

        return Response.<Map<String, Object>>builder()
                .code(200)
                .info("成功")
                .data(data)
                .build();
    }

    /**
     * 删除视频接口
     * header: Authorization: Bearer xxx
     * path: /api/video/{id}
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable Long id,
                                  @RequestHeader("Authorization") String token) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long userId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 执行删除
        try {
            videoService.deleteVideo(id, userId, userRole);
            return Response.<Void>builder()
                    .code(200)
                    .info("删除成功")
                    .build();
        } catch (Exception e) {
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询视频列表接口
     * header: Authorization: Bearer xxx
     * query参数: current(页码), size(每页大小), userId(可选), auditStatus(可选), processingStatus(可选)
     */
    @GetMapping("/list")
    public Response<PageResult<VideoVO>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "auditStatus", required = false) String auditStatus,
            @RequestParam(value = "processingStatus", required = false) String processingStatus) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<PageResult<VideoVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long currentUserId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 权限控制：如果不是管理员，只能查看自己的视频
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            userId = currentUserId;
        }

        // 4. 查询列表
        PageResult<VideoVO> result = videoService.listVideos(current, size, userId, auditStatus, processingStatus);

        return Response.<PageResult<VideoVO>>builder()
                .code(200)
                .info("查询成功")
                .data(result)
                .build();
    }
}
