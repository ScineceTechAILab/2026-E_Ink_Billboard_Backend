package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.model.dto.AuditResultDTO;
import com.stalab.e_ink_billboard_backend.model.vo.AuditItemVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.model.vo.StatsVO;
import com.stalab.e_ink_billboard_backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 获取待审核列表
     * GET /api/admin/audit/list
     */
    @GetMapping("/audit/list")
    public Response<PageResult<AuditItemVO>> getPendingAuditList(@RequestParam(required = false) Long current,
                                                                 @RequestParam(required = false) Long size,
                                                                 @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查
        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 获取列表
        try {
            PageResult<AuditItemVO> result = adminService.getPendingAuditList(current, size);
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(200)
                    .info("查询成功")
                    .data(result)
                    .build();
        } catch (Exception e) {
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 审核内容
     * POST /api/admin/audit
     */
    @PostMapping("/audit")
    public Response<Void> auditContent(@Valid @RequestBody AuditResultDTO auditDTO,
                                       @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查
        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<Void>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 执行审核
        try {
            adminService.auditContent(auditDTO);
            return Response.<Void>builder()
                    .code(200)
                    .info("审核完成")
                    .build();
        } catch (Exception e) {
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }
}
