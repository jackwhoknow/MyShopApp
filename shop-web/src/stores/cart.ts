import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartApi } from '@/api/cart'
import type { CartItem } from '@/types'

/**
 * 购物车状态：顶栏角标和购物车页共用，任何增删改后重新拉取保持最新。
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)

  const count = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalAmount = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  async function load() {
    if (!localStorage.getItem('token')) {
      items.value = []
      return
    }
    loading.value = true
    try {
      items.value = await cartApi.list()
    } finally {
      loading.value = false
    }
  }

  async function add(skuId: number, quantity: number) {
    const item = await cartApi.add({ skuId, quantity })
    await load()
    return item
  }

  async function updateQuantity(id: number, quantity: number) {
    await cartApi.updateQuantity(id, quantity)
    await load()
  }

  async function remove(id: number) {
    await cartApi.remove(id)
    await load()
  }

  async function clear() {
    await cartApi.clear()
    items.value = []
  }

  function reset() {
    items.value = []
  }

  return { items, loading, count, totalAmount, load, add, updateQuantity, remove, clear, reset }
})
