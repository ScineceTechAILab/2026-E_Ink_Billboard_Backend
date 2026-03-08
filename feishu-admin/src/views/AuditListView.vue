<template>
  <div class="min-h-screen p-6">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="card animate-slide-up">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-1">
              <i class="fas fa-check-double mr-2 text-green-500"></i>
              内容审核
            </h1>
            <p class="text-gray-500">管理所有内容审核记录</p>
          </div>
          <button @click="goBack" class="btn-secondary">
            <i class="fas fa-arrow-left mr-2"></i>
            返回
          </button>
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.1s">
        <div class="flex flex-wrap items-center gap-4">
          <div class="relative flex-1 min-w-[200px]">
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索文件名或用户名..."
              class="input-field pl-10 pr-4 py-2 w-full"
              @input="handleSearch"
            />
            <i class="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
          </div>
          <el-select
            v-model="statusFilter"
            placeholder="审核状态"
            clearable
            style="width: 150px"
            @change="loadAuditList"
          >
            <el-option label="全部" value="" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
          <el-select
            v-model="typeFilter"
            placeholder="内容类型"
            clearable
            style="width: 150px"
            @change="loadAuditList"
          >
            <el-option label="全部" value="" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="视频" value="VIDEO" />
          </el-select>
          <button @click="loadAuditList" class="btn-primary">
            <i class="fas fa-refresh mr-2"></i>
            刷新
          </button>
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.2s">
        <div v-if="loading" class="flex items-center justify-center py-12">
          <el-icon class="is-loading text-4xl text-green-500"><Loading /></el-icon>
          <span class="ml-3 text-gray-500">加载中...</span>
        </div>

        <div v-else-if="filteredAuditList.length === 0" class="text-center py-12">
          <div class="text-6xl mb-4">✅</div>
          <p class="text-gray-500 mb-4">暂无内容</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="item in filteredAuditList"
            :key="item.id"
            class="flex items-start gap-4 p-4 bg-white rounded-xl border border-gray-100 hover:border-green-200 transition-all duration-200"
          >
            <div class="w-24 h-24 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
              <img
                v-if="item.contentType === 'IMAGE'"
                :src="item.originalUrl"
                :alt="item.fileName"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center bg-gray-200">
                <i class="fas fa-video text-3xl text-gray-400"></i>
              </div>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-2">
                <h3 class="font-semibold text-gray-800 truncate">{{ item.fileName }}</h3>
                <span
                  :class="[
                    'badge',
                    item.auditStatus === 'PENDING'
                      ? 'badge-warning'
                      : item.auditStatus === 'APPROVED'
                        ? 'badge-success'
                        : 'badge-error'
                  ]"
                >
                  {{
                    item.auditStatus === 'PENDING'
                      ? '待审核'
                      : item.auditStatus === 'APPROVED'
                        ? '已通过'
                        : '已拒绝'
                  }}
                </span>
              </div>
              <div class="space-y-1 text-sm text-gray-500">
                <p><i class="fas fa-user mr-2"></i>{{ item.userName }}</p>
                <p><i class="fas fa-clock mr-2"></i>{{ formatDate(item.createTime) }}</p>
                <p v-if="item.auditReason" class="text-red-500">
                  <i class="fas fa-exclamation-circle mr-2"></i>
                  {{ item.auditReason }}
                </p>
              </div>
            </div>

            <div class="flex flex-col gap-2 flex-shrink-0">
              <button @click="viewAuditItem(item)" class="btn-secondary text-sm px-3 py-1.5">
                <i class="fas fa-eye mr-1"></i>
                详情
              </button>
              <button
                v-if="item.auditStatus === 'PENDING'"
                @click="quickApprove(item)"
                class="btn-primary text-sm px-3 py-1.5 bg-gradient-to-r from-green-500 to-emerald-500"
              >
                <i class="fas fa-check mr-1"></i>
                通过
              </button>
              <button
                v-if="item.auditStatus === 'PENDING'"
                @click="quickReject(item)"
                class="btn-secondary text-sm px-3 py-1.5 text-red-500 hover:bg-red-50"
              >
                <i class="fas fa-times mr-1"></i>
                拒绝
              </button>
              <button
                v-if="item.auditStatus !== 'PENDING'"
                @click="confirmReAudit(item)"
                class="btn-secondary text-sm px-3 py-1.5 text-orange-500 hover:bg-orange-50"
              >
                <i class="fas fa-redo mr-1"></i>
                重新审核
              </button>
            </div>
          </div>
        </div>

        <div v-if="!loading && total > size" class="flex items-center justify-center mt-6">
          <el-pagination
            v-model:current-page="current"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadAuditList"
            @current-change="loadAuditList"
          />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      title="审核详情"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedAuditItem" class="space-y-6">
        <div class="flex gap-6">
          <div class="w-64 h-64 bg-gray-100 rounded-xl overflow-hidden flex-shrink-0">
            <img
              v-if="selectedAuditItem.contentType === 'IMAGE'"
              :src="selectedAuditItem.originalUrl"
              :alt="selectedAuditItem.fileName"
              class="w-full h-full object-contain"
            />
            <div v-else class="w-full h-full flex items-center justify-center bg-gray-200">
              <i class="fas fa-video text-5xl text-gray-400"></i>
            </div>
          </div>
          <div class="flex-1 space-y-4">
            <div>
              <p class="text-xs text-gray-500 mb-1">文件名</p>
              <p class="text-gray-800 font-medium break-all">{{ selectedAuditItem.fileName }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 mb-1">内容类型</p>
              <span class="badge">{{ selectedAuditItem.contentType === 'IMAGE' ? '图片' : '视频' }}</span>
            </div>
            <div>
              <p class="text-xs text-gray-500 mb-1">上传用户</p>
              <p class="text-gray-800">{{ selectedAuditItem.userName }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500 mb-1">当前状态</p>
              <span
                :class="[
                  'badge',
                  selectedAuditItem.auditStatus === 'PENDING'
                    ? 'badge-warning'
                    : selectedAuditItem.auditStatus === 'APPROVED'
                      ? 'badge-success'
                      : 'badge-error'
                ]"
              >
                {{
                  selectedAuditItem.auditStatus === 'PENDING'
                    ? '待审核'
                    : selectedAuditItem.auditStatus === 'APPROVED'
                      ? '已通过'
                      : '已拒绝'
                }}
              </span>
            </div>
            <div>
              <p class="text-xs text-gray-500 mb-1">上传时间</p>
              <p class="text-gray-800">{{ formatDate(selectedAuditItem.createTime) }}</p>
            </div>
            <div v-if="selectedAuditItem.auditReason">
              <p class="text-xs text-gray-500 mb-1">审核原因</p>
              <p class="text-red-500">{{ selectedAuditItem.auditReason }}</p>
            </div>
          </div>
        </div>

        <div v-if="auditHistory.length > 0" class="border-t border-gray-100 pt-6">
          <h3 class="font-semibold text-gray-800 mb-4">
            <i class="fas fa-history mr-2 text-gray-500"></i>
            审核历史
          </h3>
          <div class="space-y-3">
            <div v-for="log in auditHistory" :key="log.id" class="p-4 bg-gray-50 rounded-xl">
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm font-medium text-gray-700">
                  {{ log.operationType === 'RE_AUDIT' ? '重新审核' : '审核' }}
                </span>
                <span class="text-xs text-gray-400">{{ formatDate(log.createTime) }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <span
                  :class="[
                    'badge text-xs',
                    log.beforeStatus === 'PENDING'
                      ? 'badge-warning'
                      : log.beforeStatus === 'APPROVED'
                        ? 'badge-success'
                        : 'badge-error'
                  ]"
                >
                  {{ getStatusText(log.beforeStatus) }}
                </span>
                <i class="fas fa-arrow-right text-gray-400"></i>
                <span
                  :class="[
                    'badge text-xs',
                    log.afterStatus === 'PENDING'
                      ? 'badge-warning'
                      : log.afterStatus === 'APPROVED'
                        ? 'badge-success'
                        : 'badge-error'
                  ]"
                >
                  {{ getStatusText(log.afterStatus) }}
                </span>
              </div>
              <div class="mt-2 text-sm text-gray-600">
                <i class="fas fa-user mr-1"></i>
                {{ log.auditorName }}
                <span v-if="log.auditReason" class="ml-3">
                  <i class="fas fa-comment mr-1"></i>
                  {{ log.auditReason }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="selectedAuditItem.auditStatus === 'PENDING'" class="border-t border-gray-100 pt-6">
          <h3 class="font-semibold text-gray-800 mb-4">审核操作</h3>
          <div class="space-y-4">
            <el-input
              v-if="showRejectInput"
              v-model="rejectReason"
              type="textarea"
              :rows="3"
              placeholder="请输入拒绝原因..."
              class="input-field"
            />
            <div class="flex gap-3">
              <button
                @click="handleApprove"
                :disabled="submitting"
                class="flex-1 btn-primary bg-gradient-to-r from-green-500 to-emerald-500"
              >
                <el-icon v-if="submitting" class="is-loading mr-1"><Loading /></el-icon>
                <i v-else class="fas fa-check mr-1"></i>
                通过审核
              </button>
              <button
                @click="handleReject"
                :disabled="submitting"
                class="flex-1 btn-primary bg-gradient-to-r from-red-500 to-rose-500"
              >
                <el-icon v-if="submitting" class="is-loading mr-1"><Loading /></el-icon>
                <i v-else class="fas fa-times mr-1"></i>
                {{ showRejectInput ? '确认拒绝' : '拒绝' }}
              </button>
            </div>
          </div>
        </div>

        <div
          v-if="selectedAuditItem.auditStatus !== 'PENDING'"
          class="border-t border-gray-100 pt-6"
        >
          <div class="flex gap-3">
            <button
              @click="handleReAudit"
              :disabled="submitting"
              class="flex-1 btn-primary bg-gradient-to-r from-orange-500 to-amber-500"
            >
              <el-icon v-if="submitting" class="is-loading mr-1"><Loading /></el-icon>
              <i v-else class="fas fa-redo mr-1"></i>
              重新审核
            </button>
          </div>
        </div>
      </div>
      <template #footer>
        <button @click="detailVisible = false" class="btn-secondary">关闭</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import type { AuditItemVO, AuditLogVO, AuditDTO } from '@/types'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const auditList = ref<AuditItemVO[]>([])
const auditHistory = ref<AuditLogVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedAuditItem = ref<AuditItemVO | null>(null)
const rejectReason = ref('')
const showRejectInput = ref(false)

const filteredAuditList = computed(() => {
  if (!searchKeyword.value) return auditList.value

  return auditList.value.filter((item) => {
    const keyword = searchKeyword.value.toLowerCase()
    return item.fileName?.toLowerCase().includes(keyword) || item.userName?.toLowerCase().includes(keyword)
  })
})

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusText = (status: string): string => {
  if (status === 'PENDING') return '待审核'
  if (status === 'APPROVED') return '已通过'
  return '已拒绝'
}

const loadAuditList = async () => {
  loading.value = true
  try {
    const params: any = {
      current: current.value,
      size: size.value
    }
    if (statusFilter.value) params.auditStatus = statusFilter.value
    if (typeFilter.value) params.contentType = typeFilter.value

    const res = await adminApi.getAuditList(params)

    if (res.code === 200) {
      auditList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.info || '加载失败')
    }
  } catch (error: any) {
    console.error('Load audit list failed:', error)
    ElMessage.error(error.message || '加载审核列表失败')
  } finally {
    loading.value = false
  }
}

const loadAuditHistory = async () => {
  if (!selectedAuditItem.value) return

  try {
    const res = await adminApi.getAuditHistory(selectedAuditItem.value.id, selectedAuditItem.value.contentType)
    if (res.code === 200) {
      auditHistory.value = res.data || []
    }
  } catch (error: any) {
    console.error('Load audit history failed:', error)
  }
}

const handleSearch = () => {}

const viewAuditItem = async (item: AuditItemVO) => {
  selectedAuditItem.value = item
  rejectReason.value = ''
  showRejectInput.value = false
  detailVisible.value = true
  await loadAuditHistory()
}

const quickApprove = (item: AuditItemVO) => {
  ElMessageBox.confirm(`确定要通过该内容的审核吗？`, '确认通过', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'success'
  })
    .then(async () => {
      await performAudit(item, 'APPROVED', '')
    })
    .catch(() => {})
}

const quickReject = (item: AuditItemVO) => {
  ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '请输入拒绝原因'
  })
    .then(async ({ value }) => {
      await performAudit(item, 'REJECTED', value)
    })
    .catch(() => {})
}

const performAudit = async (item: AuditItemVO, status: 'APPROVED' | 'REJECTED', reason: string) => {
  submitting.value = true
  try {
    const auditData: AuditDTO = {
      contentId: item.id,
      contentType: item.contentType,
      auditStatus: status,
      rejectReason: reason
    }
    await adminApi.auditContent(auditData)
    ElMessage.success(status === 'APPROVED' ? '审核通过' : '已拒绝')
    await loadAuditList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const confirmReAudit = (item: AuditItemVO) => {
  ElMessageBox.confirm(`确定要将该内容重置为待审核状态吗？`, '确认重新审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      submitting.value = true
      try {
        await adminApi.reAudit({
          contentId: item.id,
          contentType: item.contentType
        })
        ElMessage.success('已重置为待审核')
        await loadAuditList()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitting.value = false
      }
    })
    .catch(() => {})
}

const handleApprove = async () => {
  if (!selectedAuditItem.value) return
  await performAudit(selectedAuditItem.value, 'APPROVED', '')
  detailVisible.value = false
}

const handleReject = async () => {
  if (!selectedAuditItem.value) return

  if (!showRejectInput.value) {
    showRejectInput.value = true
    return
  }

  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  await performAudit(selectedAuditItem.value, 'REJECTED', rejectReason.value)
  detailVisible.value = false
}

const handleReAudit = async () => {
  if (!selectedAuditItem.value) return

  submitting.value = true
  try {
    await adminApi.reAudit({
      contentId: selectedAuditItem.value.id,
      contentType: selectedAuditItem.value.contentType
    })
    ElMessage.success('已重置为待审核')
    detailVisible.value = false
    await loadAuditList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadAuditList()
})
</script>