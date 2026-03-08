package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.mapper.po.AuditLog;
import com.stalab.e_ink_billboard_backend.model.dto.AuditResultDTO;
import com.stalab.e_ink_billboard_backend.model.vo.AuditItemVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.model.vo.StatsVO;
import com.stalab.e_ink_billboard_backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员接口
 */
@Slf4j
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
     */
    @GetMapping("/stats")
    public Response<StatsVO> getStats(@RequestHeader("Authorization") String token) {
        if (!jwtUtils.validateToken(token)) {
            return Response.<StatsVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .data(null)
                    .build();
        }

        StatsVO stats = adminService.getStats();

        return Response.<StatsVO>builder()
                .code(200)
                .info("操作成功")
                .data(stats)
                .build();
    }

    /**
     * 获取审核列表（支持状态和类型筛选）
     * GET /api/admin/audit/list
     */
    @GetMapping("/audit/list")
    public Response<PageResult<AuditItemVO>> getAuditList(@RequestParam(required = false) Long current,
                                                         @RequestParam(required = false) Long size,
                                                         @RequestParam(required = false) String auditStatus,
                                                         @RequestParam(required = false) String contentType,
                                                         @RequestHeader("Authorization") String token) {
        if (!jwtUtils.validateToken(token)) {
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        try {
            AuditStatus status = auditStatus != null ? AuditStatus.valueOf(auditStatus) : null;
            ContentType type = contentType != null ? ContentType.valueOf(contentType) : null;

            PageResult<AuditItemVO> result = adminService.getAuditList(current, size, status, type);
            return Response.<PageResult<AuditItemVO>>builder()
                    .code(200)
                    .info("查询成功")
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("Get audit list error", e);
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
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<Void>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        try {
            Long auditorId = jwtUtils.getUserId(token);
            String auditorName = jwtUtils.getNickname(token);
            adminService.auditContent(auditDTO, auditorId, auditorName);
            return Response.<Void>builder()
                    .code(200)
                    .info("审核完成")
                    .build();
        } catch (Exception e) {
            log.error("Audit content error", e);
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 重新审核
     * POST /api/admin/audit/re-audit
     */
    @PostMapping("/audit/re-audit")
    public Response<Void> reAudit(@RequestBody Map<String, Object> params,
                                  @RequestHeader("Authorization") String token) {
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<Void>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        try {
            Long contentId = Long.valueOf(params.get("contentId").toString());
            ContentType contentType = ContentType.valueOf(params.get("contentType").toString());

            Long auditorId = jwtUtils.getUserId(token);
            String auditorName = jwtUtils.getNickname(token);

            adminService.reAudit(contentId, contentType, auditorId, auditorName);
            return Response.<Void>builder()
                    .code(200)
                    .info("已重置为待审核状态")
                    .build();
        } catch (Exception e) {
            log.error("Re-audit error", e);
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 获取审核历史记录
     * GET /api/admin/audit/history
     */
    @GetMapping("/audit/history")
    public Response<List<AuditLog>> getAuditHistory(@RequestParam Long contentId,
                                                    @RequestParam String contentType,
                                                    @RequestHeader("Authorization") String token) {
        if (!jwtUtils.validateToken(token)) {
            return Response.<List<AuditLog>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<List<AuditLog>>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        try {
            ContentType type = ContentType.valueOf(contentType);
            List<AuditLog> history = adminService.getAuditHistory(contentId, type);
            return Response.<List<AuditLog>>builder()
                    .code(200)
                    .info("查询成功")
                    .data(history)
                    .build();
        } catch (Exception e) {
            log.error("Get audit history error", e);
            return Response.<List<AuditLog>>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }
}
