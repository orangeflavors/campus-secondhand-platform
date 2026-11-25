import { upload } from '@/utils/request'

export const fileApi = {
  upload: (file: File) => upload<{ url: string; name: string }>('/files/upload', file)
}
