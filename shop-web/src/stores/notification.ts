import { ref } from 'vue'
import { defineStore } from 'pinia'
import { notificationApi } from '@/api/notification'

/**
 * 未读通知数：顶栏角标与通知中心共用，标记已读后调用 refresh 同步角标。
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function refresh() {
    if (!localStorage.getItem('token')) {
      unreadCount.value = 0
      return
    }
    try {
      unreadCount.value = await notificationApi.unreadCount()
    } catch {
      // 角标只是辅助信息，拉取失败不影响页面主流程
    }
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, refresh, reset }
})
