/**
 * 码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
 * AGPL-3.0 许可，详见仓库 LICENSE
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({ baseURL: '', timeout: 15000 })

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = token
  return cfg
})

http.interceptors.response.use(resp => {
  const r = resp.data
  if (r.code === 0) return r.data
  if (r.code === 401) {
    localStorage.clear()
    router.push('/login')
  }
  ElMessage.error(r.msg || '请求失败')
  return Promise.reject(r)
}, err => {
  ElMessage.error(err.message || '网络异常')
  return Promise.reject(err)
})

export default http
