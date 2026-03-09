export interface ApiResponse<T = any> {
  code: number
  info: string
  data: T
}

export interface LoginVO {
  nickname: string
  role: string
  token: string
}

export interface ImageUploadVO {
  id: number
  url: string
  auditStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditMessage: string
}

export interface ImageVO {
  id: number
  userId: number
  fileName: string
  fileSize: number
  originalUrl: string
  processedUrl: string
  auditStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditReason: string | null
  createTime: string
  uploadUser?: string
}

export interface UserVO {
  id: number
  nickname: string
  role?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface UploadingFile {
  id: string
  file: File
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  error?: string
  url?: string
  compressedSize?: number
  originalSize: number
}

export interface DeviceVO {
  id: number
  deviceName: string
  deviceCode: string
  mqttTopic: string
  status: 'ONLINE' | 'OFFLINE'
  lastHeartbeat: string
  currentContentId: number | null
  currentContentType: 'IMAGE' | 'VIDEO' | null
  currentContent: {
    contentId: number
    contentType: 'IMAGE' | 'VIDEO'
    fileName: string
    thumbnailUrl: string
    fileSize: number
  } | null
  location: string | null
  description: string | null
  createTime: string
}

export interface DeviceDTO {
  deviceName: string
  deviceCode: string
  mqttTopic?: string
  location?: string
  description?: string
}

export interface AuditItemVO {
  id: number
  contentType: 'IMAGE' | 'VIDEO'
  userId: number
  userName: string
  originalUrl: string
  fileName: string
  auditStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditReason: string | null
  createTime: string
}

export interface AuditDTO {
  contentId: number
  contentType: 'IMAGE' | 'VIDEO'
  auditStatus: 'APPROVED' | 'REJECTED'
  rejectReason?: string
}

export interface AuditLogVO {
  id: number
  contentId: number
  contentType: 'IMAGE' | 'VIDEO'
  beforeStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  afterStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditorId: number
  auditorName: string
  auditReason: string | null
  operationType: string
  createTime: string
}

export interface UserState {
  isAuthenticated: boolean
  token: string | null
  userInfo: {
    nickname: string
    role: string
  } | null
}

export interface PushImageDTO {
  deviceId: number
  imageId: number
  verificationCode?: string
}

export interface PushBatchDTO {
  deviceIds: number[]
  contentId: number
  contentType: 'IMAGE' | 'VIDEO'
}

export interface ContentPushVO {
  id: number
  deviceId: number
  deviceName: string
  contentId: number
  contentType: 'IMAGE' | 'VIDEO'
  pushStatus: 'PENDING' | 'SENDING' | 'SUCCESS' | 'FAILED'
  pushTime: string
  userId: number
  userName: string
  errorMessage: string | null
}

export interface NetworkConfigDTO {
  ssid: string
  password: string
}

// ==================== 数据统计相关类型 ====================

export interface UserActivityItemVO {
  date: string
  activeUsers: number
  activeUserNames?: string[]
  yearOnYear?: number
  monthOnMonth?: number
}

export interface UserActivityVO {
  granularity: 'day' | 'week' | 'month'
  items: UserActivityItemVO[]
  totalActiveUsers: number
  avgDailyActiveUsers: number
}

export interface DeviceStatusVO {
  onlineCount: number
  offlineCount: number
  abnormalCount: number
  totalCount: number
  onlineRate: number
  offlineRate: number
  abnormalRate: number
}

export interface AnnouncementVO {
  content: string
  updatedAt: string
}