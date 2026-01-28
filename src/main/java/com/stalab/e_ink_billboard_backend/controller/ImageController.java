package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.model.vo.ImageUploadVO;
import com.stalab.e_ink_billboard_backend.model.vo.ImageVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.service.media.ImageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private final ImageService imageService;
    private final JwtUtils jwtUtils;

    public ImageController(ImageService imageService, JwtUtils jwtUtils) {
        this.imageService = imageService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 图片上传接口
     * header: Authorization: Bearer xxx
     * body: file (binary)
     * 返回：包含图片ID和URL的对象
     */
    @PostMapping("/upload")
    public Response<ImageUploadVO> upload(@RequestParam("file") MultipartFile file,
                                          @RequestHeader("Authorization") String token) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<ImageUploadVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析 UserId
        Long userId = jwtUtils.getUserId(token);

        // 3. 执行业务
        if (file.isEmpty()) {
            return Response.<ImageUploadVO>builder()
                    .code(400)
                    .info("上传文件不能为空")
                    .build();
        }

        ImageUploadVO result = imageService.uploadAndProcess(file, userId);

        // 返回图片ID和URL
        return Response.<ImageUploadVO>builder()
                .code(200)
                .info("上传成功")
                .data(result)
                .build();
    }

    /**
     * 删除图片接口
     * header: Authorization: Bearer xxx
     * path: /api/image/{id}
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
            imageService.deleteImage(id, userId, userRole);
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
     * 查询图片列表接口
     * header: Authorization: Bearer xxx
     * query参数: current(页码), size(每页大小), userId(可选), auditStatus(可选: PENDING/APPROVED/REJECTED)
     */
    @GetMapping("/list")
    public Response<PageResult<ImageVO>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "auditStatus", required = false) String auditStatusStr) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<PageResult<ImageVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long currentUserId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 权限控制：如果不是管理员，只能查看自己的图片
        if (!"ADMIN".equals(userRole)) {
            userId = currentUserId;
        }

        // 4. 转换审核状态枚举
        AuditStatus auditStatus = null;
        if (auditStatusStr != null) {
            try {
                auditStatus = AuditStatus.valueOf(auditStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Response.<PageResult<ImageVO>>builder()
                        .code(400)
                        .info("审核状态参数无效，可选值: PENDING, APPROVED, REJECTED")
                        .build();
            }
        }

        // 5. 查询列表
        PageResult<ImageVO> result = imageService.listImages(current, size, userId, auditStatus);

        return Response.<PageResult<ImageVO>>builder()
                .code(200)
                .info("查询成功")
                .data(result)
                .build();
    }
}
