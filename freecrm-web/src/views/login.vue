<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 style="text-align:center;margin-bottom:24px">码域CRM</h2>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useUserStore } from '../store/user'
import { installDynamicRoutes } from '../layout/dynamic'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: 'admin', password: '' })
const loading = ref(false)

async function doLogin() {
  loading.value = true
  try {
    const data = await login(form)
    userStore.setLogin(data)
    installDynamicRoutes(data.menus || [])
    router.push('/')
    ElMessage.success('欢迎，' + data.nickname)
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.login-wrap { display: flex; justify-content: center; align-items: center; height: 100vh; background: #f0f2f5; }
.login-card { width: 380px; }
</style>
