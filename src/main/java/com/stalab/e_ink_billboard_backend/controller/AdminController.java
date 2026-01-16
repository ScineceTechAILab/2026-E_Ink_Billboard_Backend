package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.model.vo.StatsVO;
import com.stalab.e_ink_billboard_backend.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 管理员接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtils jwtUtils;

    public AdminController(AdminService adminService, JwtUtils jwtUtils) {
        this.adminService = adminService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 获取统计数据
     * @param token Bearer Token
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Response<StatsVO> getStats(@RequestHeader("Authorization") String token) {
        // 1. 校验 Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<StatsVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .data(null)
                    .build();
        }

        // 2. 获取统计数据
        StatsVO stats = adminService.getStats();

        // 3. 返回结果
        return Response.<StatsVO>builder()
                .code(200)
                .info("操作成功")
                .data(stats)
                .build();
    }
}
