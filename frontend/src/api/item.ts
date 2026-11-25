import { del, get, post, put } from '@/utils/request'
import type {
  CategoryVO,
  ItemCardVO,
  ItemDetailVO,
  ItemSearchParams,
  ItemUpsertPayload,
  MetaVO,
  PageResult
} from '@/types'

export const metaApi = {
  get: () => get<MetaVO>('/meta'),
  categories: () => get<CategoryVO[]>('/meta/categories')
}

export const itemApi = {
  search: (params: ItemSearchParams) => get<PageResult<ItemCardVO>>('/items', params as Record<string, unknown>),
  detail: (id: number) => get<ItemDetailVO>(`/items/${id}`),
  create: (data: ItemUpsertPayload) => post<ItemDetailVO>('/items', data),
  update: (id: number, data: ItemUpsertPayload) => put<ItemDetailVO>(`/items/${id}`, data),
  remove: (id: number) => del<void>(`/items/${id}`),
  submit: (id: number) => post<ItemDetailVO>(`/items/${id}/submit`),
  offShelf: (id: number) => post<ItemDetailVO>(`/items/${id}/off-shelf`),
  mine: (params: { status?: string; page?: number; size?: number }) =>
    get<PageResult<ItemCardVO>>('/me/items', params as Record<string, unknown>),
  favorites: (params: { page?: number; size?: number }) =>
    get<PageResult<ItemCardVO>>('/me/favorites', params)
}
