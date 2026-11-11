import router from '../router'
import Layout from './index.vue'

let installed = false

export function routesInstalled() {
  return installed
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
  router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/system/user' })
}

function loadView(component) {
  if (!component) return undefined
  const views = import.meta.glob('../views/**/*.vue')
  return views[`../views/${component}.vue`] || views[`../views/${component}/index.vue`]
}
