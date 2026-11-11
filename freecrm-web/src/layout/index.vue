<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-container style="height:100vh">
    <el-aside width="210px" style="background:#001529">
      <div class="logo">码域CRM</div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#bfcbd9"
               active-text-color="#409EFF" style="border-right:none">
        <template v-for="dir in userStore.menus" :key="dir.id">
          <el-sub-menu v-if="dir.children && dir.children.length" :index="dir.path">
            <template #title>
              <el-icon><component :is="dir.icon || 'Folder'" /></el-icon>
              <span>{{ dir.name }}</span>
            </template>
            <el-menu-item v-for="m in dir.children" :key="m.id" :index="dir.path + '/' + m.path">
              <el-icon><component :is="m.icon || 'Document'" /></el-icon>
              <span>{{ m.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="dir.path">
            <el-icon><component :is="dir.icon || 'Folder'" /></el-icon>
            <span>{{ dir.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:space-between;background:#fff;border-bottom:1px solid #eee">
        <span style="font-weight:bold">{{ ($route.meta && $route.meta.title) || '' }}</span>
        <div style="display:flex;align-items:center;gap:18px">
          <el-badge :value="unread" :hidden="!unread" :max="99" style="cursor:pointer" @click="toNotice">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="onCmd">
            <span style="cursor:pointer">{{ userStore.nickname }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="notice">我的消息</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main style="background:#f0f2f5">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useUserStore } from '../store/user'
import { logout } from '../api/auth'
import http from '../api/request'
import router from '../router'

const userStore = useUserStore()
const unread = ref(0)
let timer = null

async function refreshUnread() {
  try {
    unread.value = Number(await http.get('/api/notice/unread-count')) || 0
  } catch (e) { /* 忽略 */ }
}
function toNotice() {
  router.push('/system/notice')
}
async function onCmd(cmd) {
  if (cmd === 'notice') return toNotice()
  if (cmd === 'logout') {
    await logout()
    userStore.clear()
    location.href = '/login'
  }
}
onMounted(() => {
  refreshUnread()
  timer = setInterval(refreshUnread, 30000)
})
onUnmounted(() => clearInterval(timer))
</script>
<style scoped>
.logo { height: 56px; line-height: 56px; text-align: center; color: #fff; font-size: 20px; font-weight: bold; }
</style>
