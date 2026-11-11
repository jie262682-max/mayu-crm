/**
 * 码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
 * AGPL-3.0 许可，详见仓库 LICENSE
 */
/**
 * 网络请求封装。
 * H5：开发走 vite 代理、生产同域部署（nginx /api 反代到后端），BASE 恒为空。
 * 小程序 / APP：必须填后端绝对地址（见 PROD_BASE）。
 *
 * 真机/小程序联调：把 PROD_BASE 改成电脑的局域网 IP（ipconfig 查看），
 * 例如 http://192.168.1.100:48100，且手机与电脑同一 WiFi。
 * 微信开发者工具请勾选「不校验合法域名」；正式发布需 HTTPS 域名并在
 * 小程序后台配置 request 合法域名。
 */
const PROD_BASE = 'http://127.0.0.1:48100'
const IS_H5 = process.env.UNI_PLATFORM === 'h5'
export const BASE_URL = IS_H5 ? '' : PROD_BASE

export function request({ url, method = 'GET', data, params }) {
  // params 统一拼 query（后端 @RequestParam；PUT/POST 表单式接口也可用）
  if (params) {
    const qs = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&')
    if (qs) url += (url.includes('?') ? '&' : '?') + qs
  }
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 15000,
      header: token ? { Authorization: token } : {},
      success(res) {
        const r = res.data
        if (r && r.code === 0) return resolve(r.data)
        if (r && r.code === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('user')
          uni.reLaunch({ url: '/pages/login/login' })
          return reject(r)
        }
        uni.showToast({ icon: 'none', title: (r && r.msg) || '请求失败' })
        reject(r)
      },
      fail(err) {
        uni.showToast({ icon: 'none', title: err.errMsg || '网络异常' })
        reject(err)
      }
    })
  })
}

export default {
  get: (url, params) => request({ url, params }),
  post: (url, data) => request({ url, method: 'POST', data }),
  put: (url, params) => request({ url, method: 'PUT', params })
}
