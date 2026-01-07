package com.stalab.e_ink_billboard_backend.controller;


import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.service.ImageService;
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
     */
    @PostMapping("/upload")
    public Response<String> upload(@RequestParam("file") MultipartFile file,
                                   @RequestHeader("Authorization") String token) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<String>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析 UserId
        Long userId = jwtUtils.getUserId(token);

        // 3. 执行业务
        if (file.isEmpty()) {
            return Response.<String>builder()
                    .code(400)
                    .info("上传文件不能为空")
                    .build();
        }

        String url = imageService.uploadAndProcess(file, userId);

        // 返回原图URL给前端回显
        return Response.<String>builder()
                .code(200)
                .info("上传成功")
                .data(url)
                .build();
    }
}
