import { get, post, put } from '@/utils/request'
import type {
  CampusAuthPayload,
  LoginPayload,
  LoginResult,
  PageResult,
  ProfileUpdatePayload,
  PublicProfileVO,
  RegisterPayload,
  ReviewVO,
  UserVO
} from '@/types'

export const authApi = {
  login: (data: LoginPayload) => post<LoginResult>('/auth/login', data),
  register: (data: RegisterPayload) => post<LoginResult>('/auth/register', data),
  resetPassword: (data: { username: string; studentNo: string; newPassword: string }) =>
    post<void>('/auth/password/reset', data),
  me: () => get<UserVO>('/auth/me'),
  updateProfile: (data: ProfileUpdatePayload) => put<UserVO>('/users/me', data),
  campusAuth: (data: CampusAuthPayload) => post<UserVO>('/users/me/campus-auth', data),
  publicProfile: (id: number) => get<PublicProfileVO>(`/users/${id}/profile`),
  userReviews: (id: number, params?: { page?: number; size?: number }) =>
    get<PageResult<ReviewVO>>(`/users/${id}/reviews`, params)
}
