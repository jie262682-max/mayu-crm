/*
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
*/
import router from '../router'
import Layout from './index.vue'

let installed = false

export function routesInstalled() {
  return installed
}

/** 用户首个可访问页面路径（登录跳转/兜底重定向用，避免写死 /system/user：无该菜单的角色会死循环） */
export function firstPermittedPath(menus) {
  for (const dir of menus || []) {
    if (dir.children && dir.children.length > 0) {
      const d = String(dir.path || '').replace(/\/+$/, '')
      const m = String(dir.children[0].path || '').replace(/^\/+/, '')
      return d + '/' + m
    }
  }
  return '/login'
}

/** 按后端菜单树注册动态路由（目录一层 Layout，菜单一层 children） */
export function installDynamicRoutes(menus) {
  if (installed) return
  installed = true
  for (const dir of menus) {
    if (!dir.children || dir.children.length === 0) continue
    router.addRoute({
      path: dir.path,
      component: Layout,
      children: dir.children.map(menu => ({
        path: menu.path,
        name: 'm' + menu.id,
        component: loadView(menu.component),
        meta: { title: menu.name, icon: menu.icon }
      }))
    })
  }
  // 兜底 404 → 当前用户首页（store 持久化在 localStorage.menus）
  router.addRoute({
    path: '/:pathMatch(.*)*',
    redirect: () => firstPermittedPath(JSON.parse(localStorage.getItem('menus') || '[]'))
  })
}

function loadView(component) {
  if (!component) return undefined
  const views = import.meta.glob('../views/**/*.vue')
  return views[`../views/${component}.vue`] || views[`../views/${component}/index.vue`]
}
