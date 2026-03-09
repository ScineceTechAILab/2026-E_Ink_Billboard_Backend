import request from '@/utils/request'
import type {
  ApiResponse,
  LoginVO,
  ImageUploadVO,
  PageResult,
  ImageVO,
  DeviceVO,
  DeviceDTO,
  AuditItemVO,
  AuditDTO,
  AuditLogVO,
  PushImageDTO,
  PushBatchDTO,
  ContentPushVO,
  NetworkConfigDTO,
  UserActivityVO,
  DeviceStatusVO,
  UserVO,
  AnnouncementVO
} from '@/types'

export const authApi = {
  feishuLogin(code: string): Promise<ApiResponse<LoginVO>> {
    return request.post('/api/auth/feishu/login', { code })
  }
}

export const imageApi = {
  upload(
    file: File,
    onUploadProgress?: (progressEvent: any) => void,
    uploadId?: string
  ): Promise<ApiResponse<ImageUploadVO>> {
    const formData = new FormData()
    formData.append('file', file)

    const config: any = {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }

    if (uploadId) {
      config.headers['X-Upload-Id'] = uploadId
    }

    if (onUploadProgress) {
      config.onUploadProgress = onUploadProgress
    }

    return request.post('/api/image/upload', formData, config)
  },

  delete(id: number): Promise<ApiResponse<null>> {
    return request.delete(`/api/image/${id}`)
  },

  list(params?: {
    current?: number
    size?: number
    fileName?: string
    sortOrder?: 'asc' | 'desc'
    owner?: 'self' | 'all'
    userIds?: number[]
    auditStatus?: string
  }): Promise<ApiResponse<PageResult<ImageVO>>> {
    const queryParams: any = { ...params }
    if (params?.userIds) {
      queryParams.userIds = params.userIds.join(',')
    }
    return request.get('/api/image/list', { params: queryParams })
  }
}

export const deviceApi = {
  list(): Promise<ApiResponse<DeviceVO[]>> {
    return request.get('/api/device/list')
  },

  get(id: number): Promise<ApiResponse<DeviceVO>> {
    return request.get(`/api/device/${id}`)
  },

  create(data: DeviceDTO): Promise<ApiResponse<DeviceVO>> {
    return request.post('/api/device', data)
  },

  update(id: number, data: DeviceDTO): Promise<ApiResponse<null>> {
    return request.put(`/api/device/${id}`, data)
  },

  delete(id: number): Promise<ApiResponse<null>> {
    return request.delete(`/api/device/${id}`)
  },

  changeNetwork(id: number, data: NetworkConfigDTO): Promise<ApiResponse<null>> {
    return request.post(`/api/device/${id}/network`, data)
  }
}

export const pushApi = {
  pushImage(data: PushImageDTO): Promise<ApiResponse<null>> {
    return request.post('/api/push/image', data)
  },

  pushVideo(data: { deviceId: number; videoId: number; verificationCode?: string }): Promise<ApiResponse<null>> {
    return request.post('/api/push/video', data)
  },

  pushBatch(data: PushBatchDTO): Promise<ApiResponse<null>> {
    return request.post('/api/push/batch', data)
  },

  getHistory(params?: {
    current?: number
    size?: number
    deviceId?: number
    userId?: number
  }): Promise<ApiResponse<PageResult<ContentPushVO>>> {
    return request.get('/api/push/history', { params })
  }
}

export const adminApi = {
  getStats(): Promise<
    ApiResponse<{
      onlineDevices: number
      pendingAudits: number
      approvedContent: number
      online: number
      pending: number
      approved: number
    }>
  > {
    return request.get('/api/admin/stats')
  },

  getAuditList(params?: {
    current?: number
    size?: number
    auditStatus?: string
    contentType?: string
  }): Promise<ApiResponse<PageResult<AuditItemVO>>> {
    return request.get('/api/admin/audit/list', { params })
  },

  auditContent(data: AuditDTO): Promise<ApiResponse<null>> {
    return request.post('/api/admin/audit', data)
  },

  reAudit(data: { contentId: number; contentType: 'IMAGE' | 'VIDEO' }): Promise<ApiResponse<null>> {
    return request.post('/api/admin/audit/re-audit', data)
  },

  getAuditHistory(contentId: number, contentType: string): Promise<ApiResponse<AuditLogVO[]>> {
    return request.get('/api/admin/audit/history', {
      params: { contentId, contentType }
    })
  },

  getUsers(): Promise<ApiResponse<UserVO[]>> {
    return request.get('/api/admin/users')
  },

  getAnnouncement(): Promise<ApiResponse<AnnouncementVO>> {
    return request.get('/api/admin/announcement')
  },

  saveAnnouncement(content: string): Promise<ApiResponse<null>> {
    return request.post('/api/admin/announcement', { content })
  }
}

export const analyticsApi = {
  /**
   * 获取用户活跃度数据
   */
  getUserActivity(params: {
    startDate: string
    endDate: string
    granularity: 'day' | 'week' | 'month'
  }): Promise<ApiResponse<UserActivityVO>> {
    return request.get('/api/admin/analytics/user-activity', { params })
  },

  /**
   * 获取设备状态统计
   */
  getDeviceStatus(params?: {
    startDate?: string
    endDate?: string
  }): Promise<ApiResponse<DeviceStatusVO>> {
    return request.get('/api/admin/analytics/device-status', { params })
  }
}
