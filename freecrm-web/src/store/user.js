import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    nickname: localStorage.getItem('nickname') || '',
    menus: JSON.parse(localStorage.getItem('menus') || '[]'),
    permissions: JSON.parse(localStorage.getItem('perms') || '[]')
  }),
  actions: {
    setLogin(data) {
      this.token = data.token
      this.nickname = data.nickname
      this.menus = data.menus || []
      this.permissions = data.permissions || []
      localStorage.setItem('token', data.token)
      localStorage.setItem('nickname', data.nickname)
      localStorage.setItem('menus', JSON.stringify(this.menus))
      localStorage.setItem('perms', JSON.stringify(this.permissions))
    },
    clear() {
      this.token = ''; this.nickname = ''; this.menus = []; this.permissions = []
      localStorage.clear()
    }
  }
})
