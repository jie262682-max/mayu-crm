import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import { installDynamicRoutes, routesInstalled } from '../layout/dynamic'

// 动态组件映射：后端 component 字符串 -> 页面文件
const views = import.meta.glob('../views/**/*.vue')

export function resolveComponent(component) {
  if (!component || component === 'Layout') return null
  return views[`../views/${component}.vue`] || views[`../views/${component}/index.vue`]
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/login.vue') },
    {
      path: '/',
      component: () => import('../layout/index.vue'),
      redirect: '/system/user',
      children: []
    }
  ]
})

// 登录守卫：无 token 一律回登录页；刷新后重装动态路由
router.beforeEach(to => {
  const store = useUserStore()
  if (!store.token && to.path !== '/login') return '/login'
  if (store.token && !routesInstalled() && store.menus.length) {
    installDynamicRoutes(store.menus)
    return to.fullPath // 重新解析以命中新路由
  }
})

export default router
