import http from './http'

export type UploadCategory = 'product' | 'avatar' | 'content'

export const uploadApi = {
  image: (file: File, category: UploadCategory = 'product') => {
    const form = new FormData()
    form.append('file', file)
    form.append('category', category)
    return http.post<{ url: string }>('/api/upload/image', form)
  }
}
