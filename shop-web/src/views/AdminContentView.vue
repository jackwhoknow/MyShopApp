<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminContentApi, type AnnouncementPayload, type BannerPayload } from '@/api/content'
import type { Announcement, Banner } from '@/types'

const activeTab = ref<'banners' | 'announcements'>('banners')

const banners = ref<Banner[]>([])
const bannerLoading = ref(false)

const announcements = ref<Announcement[]>([])
const announcementLoading = ref(false)
const total = ref(0)
const page = ref(0)
const size = ref(10)

const bannerDialogVisible = ref(false)
const bannerSubmitting = ref(false)
const bannerEditing = ref<Banner | null>(null)
const bannerForm = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  enabled: true
})

const announcementDialogVisible = ref(false)
const announcementSubmitting = ref(false)
const announcementEditing = ref<Announcement | null>(null)
const announcementForm = reactive({
  title: '',
  content: '',
  pinned: false,
  enabled: true
})

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

async function loadBanners() {
  bannerLoading.value = true
  try {
    banners.value = await adminContentApi.banners()
  } finally {
    bannerLoading.value = false
  }
}

async function loadAnnouncements() {
  announcementLoading.value = true
  try {
    const result = await adminContentApi.announcements({ page: page.value, size: size.value })
    announcements.value = result.list
    total.value = result.total
  } finally {
    announcementLoading.value = false
  }
}

function handlePage(current: number) {
  page.value = current
  loadAnnouncements()
}

function openBannerDialog(banner?: Banner) {
  bannerEditing.value = banner ?? null
  bannerForm.title = banner?.title ?? ''
  bannerForm.imageUrl = banner?.imageUrl ?? ''
  bannerForm.linkUrl = banner?.linkUrl ?? ''
  bannerForm.sortOrder = banner?.sortOrder ?? 0
  bannerForm.enabled = banner?.enabled ?? true
  bannerDialogVisible.value = true
}

async function submitBanner() {
  if (!bannerForm.title.trim()) {
    ElMessage.warning('请填写轮播标题')
    return
  }
  bannerSubmitting.value = true
  try {
    const payload: BannerPayload = {
      title: bannerForm.title.trim(),
      imageUrl: bannerForm.imageUrl.trim() || undefined,
      linkUrl: bannerForm.linkUrl.trim() || undefined,
      sortOrder: bannerForm.sortOrder,
      enabled: bannerForm.enabled
    }
    if (bannerEditing.value) {
      await adminContentApi.updateBanner(bannerEditing.value.id, payload)
      ElMessage.success('轮播已更新')
    } else {
      await adminContentApi.createBanner(payload)
      ElMessage.success('轮播已创建')
    }
    bannerDialogVisible.value = false
    loadBanners()
  } finally {
    bannerSubmitting.value = false
  }
}

async function removeBanner(banner: Banner) {
  await ElMessageBox.confirm(`确定删除轮播「${banner.title}」吗？`, '删除轮播', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await adminContentApi.deleteBanner(banner.id)
  ElMessage.success('轮播已删除')
  loadBanners()
}

function openAnnouncementDialog(announcement?: Announcement) {
  announcementEditing.value = announcement ?? null
  announcementForm.title = announcement?.title ?? ''
  announcementForm.content = announcement?.content ?? ''
  announcementForm.pinned = announcement?.pinned ?? false
  announcementForm.enabled = announcement?.enabled ?? true
  announcementDialogVisible.value = true
}

async function submitAnnouncement() {
  if (!announcementForm.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (!announcementForm.content.trim()) {
    ElMessage.warning('请填写公告内容')
    return
  }
  announcementSubmitting.value = true
  try {
    const payload: AnnouncementPayload = {
      title: announcementForm.title.trim(),
      content: announcementForm.content.trim(),
      pinned: announcementForm.pinned,
      enabled: announcementForm.enabled
    }
    if (announcementEditing.value) {
      await adminContentApi.updateAnnouncement(announcementEditing.value.id, payload)
      ElMessage.success('公告已更新')
    } else {
      await adminContentApi.createAnnouncement(payload)
      ElMessage.success('公告已创建')
    }
    announcementDialogVisible.value = false
    loadAnnouncements()
  } finally {
    announcementSubmitting.value = false
  }
}

async function removeAnnouncement(announcement: Announcement) {
  await ElMessageBox.confirm(`确定删除公告「${announcement.title}」吗？`, '删除公告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
  await adminContentApi.deleteAnnouncement(announcement.id)
  ElMessage.success('公告已删除')
  loadAnnouncements()
}

onMounted(() => {
  loadBanners()
  loadAnnouncements()
})
</script>

<template>
  <div>
    <h2 class="section-title">内容管理</h2>

    <el-tabs v-model="activeTab" class="content-tabs">
      <el-tab-pane label="首页轮播" name="banners">
        <div v-loading="bannerLoading" class="page-card">
          <div class="panel-toolbar">
            <el-button type="primary" @click="openBannerDialog()">新增轮播</el-button>
            <span class="muted">未填写图片地址时，前台使用渐变色展示</span>
          </div>
          <el-table :data="banners" empty-text="暂无轮播图">
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="标题" min-width="220">
              <template #default="{ row }">
                <div class="banner-title">{{ row.title }}</div>
                <div v-if="row.linkUrl" class="muted">跳转：{{ row.linkUrl }}</div>
              </template>
            </el-table-column>
            <el-table-column label="图片" width="110">
              <template #default="{ row }">
                <img v-if="row.imageUrl" :src="row.imageUrl" class="thumb" alt="轮播图" />
                <el-tag v-else size="small" type="info" effect="plain">渐变</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
                  {{ row.enabled ? '启用中' : '已停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openBannerDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeBanner(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="平台公告" name="announcements">
        <div v-loading="announcementLoading" class="page-card">
          <div class="panel-toolbar">
            <el-button type="primary" @click="openAnnouncementDialog()">新增公告</el-button>
            <span class="muted">置顶公告会排在公告栏最前</span>
          </div>
          <el-table :data="announcements" empty-text="暂无公告">
            <el-table-column label="标题" min-width="240">
              <template #default="{ row }">
                <el-tag v-if="row.pinned" size="small" type="danger" effect="plain" class="pin-tag">置顶</el-tag>
                {{ row.title }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
                  {{ row.enabled ? '已发布' : '已下线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAnnouncementDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeAnnouncement(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="total > size" class="pagination">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="total"
              :page-size="size"
              :current-page="page + 1"
              @current-change="(current: number) => handlePage(current - 1)"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="bannerDialogVisible" :title="bannerEditing ? '编辑轮播' : '新增轮播'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="bannerForm.title" maxlength="100" show-word-limit placeholder="例如：新品首发 · 潮流好物抢先看" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="bannerForm.imageUrl" placeholder="https://... 留空则使用渐变色" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="bannerForm.linkUrl" placeholder="站内路径，例如 /products/1" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sortOrder" :min="0" :max="9999" />
          <span class="muted form-tip">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="bannerForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bannerSubmitting" @click="submitBanner">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="announcementDialogVisible" :title="announcementEditing ? '编辑公告' : '新增公告'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="announcementForm.title" maxlength="100" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="公告正文"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="announcementForm.pinned" />
          <span class="muted form-tip">置顶后展示在公告栏最前</span>
        </el-form-item>
        <el-form-item label="发布">
          <el-switch v-model="announcementForm.enabled" />
          <span class="muted form-tip">关闭后前台不再展示</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="announcementSubmitting" @click="submitAnnouncement">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.content-tabs {
  margin-bottom: 8px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.banner-title {
  font-weight: 600;
  font-size: 14px;
}

.thumb {
  width: 72px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}

.pin-tag {
  margin-right: 6px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
}
</style>
