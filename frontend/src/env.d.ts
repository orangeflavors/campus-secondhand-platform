/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 无需登录即可访问 */
    public?: boolean
    /** 已登录用户不应再访问（登录/注册页） */
    guestOnly?: boolean
    /** 需要完成校园认证 */
    requiresVerified?: boolean
    /** 需要管理员角色 */
    requiresAdmin?: boolean
    title?: string
  }
}
