package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.model.dto.DeleteQueueItemsDTO;
import com.stalab.e_ink_billboard_backend.model.dto.DeviceDTO;
import com.stalab.e_ink_billboard_backend.model.vo.DeviceVO;
import com.stalab.e_ink_billboard_backend.model.vo.QueueItemVO;
import com.stalab.e_ink_billboard_backend.service.DeviceService;
import com.stalab.e_ink_billboard_backend.service.PlayQueueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理Controller
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final JwtUtils jwtUtils;
    private final PlayQueueService playQueueService;

    public DeviceController(DeviceService deviceService, JwtUtils jwtUtils, PlayQueueService playQueueService) {
        this.deviceService = deviceService;
        this.jwtUtils = jwtUtils;
        this.playQueueService = playQueueService;
    }

    /**
     * 获取设备列表
     * GET /api/device/list
     */
    @GetMapping("/list")
    public Response<List<DeviceVO>> getDeviceList(@RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<List<DeviceVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 解析用户信息
        Long userId = jwtUtils.getUserId(token);
        String userRole = jwtUtils.getRole(token);

        // 3. 查询设备列表
        List<DeviceVO> devices = deviceService.getDeviceList(userId, userRole);

        return Response.<List<DeviceVO>>builder()
                .code(200)
                .info("查询成功")
                .data(devices)
                .build();
    }

    /**
     * 获取设备详情
     * GET /api/device/{id}
     */
    @GetMapping("/{id}")
    public Response<DeviceVO> getDeviceById(@PathVariable Long id,
                                            @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<DeviceVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 查询设备详情
        try {
            DeviceVO device = deviceService.getDeviceById(id);
            return Response.<DeviceVO>builder()
                    .code(200)
                    .info("查询成功")
                    .data(device)
                    .build();
        } catch (Exception e) {
            return Response.<DeviceVO>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 获取设备当前显示内容
     * GET /api/device/{id}/current-content
     */
    @GetMapping("/{id}/current-content")
    public Response<DeviceVO> getCurrentContent(@PathVariable Long id,
                                                @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<DeviceVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 查询设备详情（包含当前内容信息）
        try {
            DeviceVO device = deviceService.getDeviceById(id);
            return Response.<DeviceVO>builder()
                    .code(200)
                    .info("查询成功")
                    .data(device)
                    .build();
        } catch (Exception e) {
            return Response.<DeviceVO>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 添加设备（仅管理员）
     * POST /api/device
     */
    @PostMapping
    public Response<DeviceVO> addDevice(@Valid @RequestBody DeviceDTO deviceDTO,
                                        @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<DeviceVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查：仅管理员可添加设备
        String userRole = jwtUtils.getRole(token);
        if (!"ADMIN".equals(userRole)) {
            return Response.<DeviceVO>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 转换为实体并保存
        try {
            Device device = new Device();
            device.setDeviceName(deviceDTO.getDeviceName());
            device.setDeviceCode(deviceDTO.getDeviceCode());
            device.setMqttTopic(deviceDTO.getMqttTopic());
            device.setLocation(deviceDTO.getLocation());
            device.setDescription(deviceDTO.getDescription());

            Device savedDevice = deviceService.addDevice(device);
            DeviceVO deviceVO = deviceService.getDeviceById(savedDevice.getId());

            return Response.<DeviceVO>builder()
                    .code(200)
                    .info("添加成功")
                    .data(deviceVO)
                    .build();
        } catch (Exception e) {
            return Response.<DeviceVO>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 更新设备（仅管理员）
     * PUT /api/device/{id}
     */
    @PutMapping("/{id}")
    public Response<Void> updateDevice(@PathVariable Long id,
                                       @Valid @RequestBody DeviceDTO deviceDTO,
                                       @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查：仅管理员可更新设备
        String userRole = jwtUtils.getRole(token);
        if (!"ADMIN".equals(userRole)) {
            return Response.<Void>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 更新设备
        try {
            Device device = new Device();
            device.setId(id);
            device.setDeviceName(deviceDTO.getDeviceName());
            device.setDeviceCode(deviceDTO.getDeviceCode());
            device.setMqttTopic(deviceDTO.getMqttTopic());
            device.setLocation(deviceDTO.getLocation());
            device.setDescription(deviceDTO.getDescription());

            deviceService.updateDevice(device);

            return Response.<Void>builder()
                    .code(200)
                    .info("更新成功")
                    .build();
        } catch (Exception e) {
            return Response.<Void>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 删除设备（仅管理员）
     * DELETE /api/device/{id}
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteDevice(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Void>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查：仅管理员可删除设备
        String userRole = jwtUtils.getRole(token);
        if (!"ADMIN".equals(userRole)) {
            return Response.<Void>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 删除设备
        try {
            deviceService.deleteDevice(id);
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
     * 获取设备播放队列列表（仅管理员）
     * GET /api/device/{id}/queue
     */
    @GetMapping("/{id}/queue")
    public Response<List<QueueItemVO>> getDeviceQueue(@PathVariable Long id,
                                                       @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<List<QueueItemVO>>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查：仅管理员可查看队列
        String userRole = jwtUtils.getRole(token);
        if (!"ADMIN".equals(userRole)) {
            return Response.<List<QueueItemVO>>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 查询队列列表
        try {
            List<QueueItemVO> queueItems = playQueueService.getQueueList(id);
            return Response.<List<QueueItemVO>>builder()
                    .code(200)
                    .info("查询成功")
                    .data(queueItems)
                    .build();
        } catch (Exception e) {
            return Response.<List<QueueItemVO>>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 删除设备播放队列中的项（仅管理员）
     * DELETE /api/device/{id}/queue
     */
    @DeleteMapping("/{id}/queue")
    public Response<Integer> deleteQueueItems(@PathVariable Long id,
                                               @Valid @RequestBody DeleteQueueItemsDTO deleteDTO,
                                               @RequestHeader("Authorization") String token) {
        // 1. 校验Token
        if (!jwtUtils.validateToken(token)) {
            return Response.<Integer>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        // 2. 权限检查：仅管理员可删除队列项
        String userRole = jwtUtils.getRole(token);
        if (!"ADMIN".equals(userRole)) {
            return Response.<Integer>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        // 3. 删除队列项
        try {
            int deletedCount = playQueueService.deleteQueueItems(id, deleteDTO.getPushIds());
            return Response.<Integer>builder()
                    .code(200)
                    .info("删除成功")
                    .data(deletedCount)
                    .build();
        } catch (Exception e) {
            return Response.<Integer>builder()
                    .code(400)
                    .info(e.getMessage())
                    .build();
        }
    }
}
