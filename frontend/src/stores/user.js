import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    permissions: []
  }),

  getters: {
    isAdmin: (state) => state.userInfo.role === 'ADMIN',
    username: (state) => state.userInfo.username || ''
  },

  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    async getCaptcha() {
      const res = await authApi.getCaptcha()
      return res.data
    },

    async login(loginData) {
      const res = await authApi.login(loginData)
      this.setToken(res.data.token)
      this.setUserInfo({
        username: res.data.username,
        role: res.data.role
      })
      return res
    },

    logout() {
      this.token = ''
      this.userInfo = {}
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
