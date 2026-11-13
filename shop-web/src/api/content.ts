import http from './http'
import type { Announcement, Banner, HomeContent, PageResult } from '@/types'

/** 前台首页内容 */
export const contentApi = {
  home: () => http.get<HomeContent>('/api/content/home'),
  announcement: (id: number) => http.get<Announcement>(`/api/content/announcements/${id}`)
}

export interface BannerPayload {
  title: string
  imageUrl?: string
  linkUrl?: string
  sortOrder?: number
  enabled?: boolean
}

export interface AnnouncementPayload {
  title: string
  content: string
  pinned?: boolean
  enabled?: boolean
}

/** 后台内容管理 */
export const adminContentApi = {
  banners: () => http.get<Banner[]>('/api/admin/content/banners'),
  createBanner: (data: BannerPayload) => http.post<Banner>('/api/admin/content/banners', data),
  updateBanner: (id: number, data: BannerPayload) =>
    http.put<Banner>(`/api/admin/content/banners/${id}`, data),
  deleteBanner: (id: number) => http.delete<void>(`/api/admin/content/banners/${id}`),

  announcements: (params: { page?: number; size?: number }) =>
    http.get<PageResult<Announcement>>('/api/admin/content/announcements', { params }),
  createAnnouncement: (data: AnnouncementPayload) =>
    http.post<Announcement>('/api/admin/content/announcements', data),
  updateAnnouncement: (id: number, data: AnnouncementPayload) =>
    http.put<Announcement>(`/api/admin/content/announcements/${id}`, data),
  deleteAnnouncement: (id: number) => http.delete<void>(`/api/admin/content/announcements/${id}`)
}
