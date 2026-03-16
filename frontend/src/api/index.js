import request from '@/utils/request'

export const authApi = {
  getCaptcha() {
    return request({
      url: '/auth/captcha',
      method: 'get'
    })
  },
  login(data) {
    return request({
      url: '/auth/login',
      method: 'post',
      data
    })
  }
}

export const tradeApi = {
  getPage(params) {
    return request({
      url: '/trade/page',
      method: 'get',
      params
    })
  },
  getDetail(id) {
    return request({
      url: `/trade/${id}`,
      method: 'get'
    })
  },
  add(data) {
    return request({
      url: '/trade',
      method: 'post',
      data
    })
  },
  update(data) {
    return request({
      url: '/trade',
      method: 'put',
      data
    })
  },
  delete(id) {
    return request({
      url: `/trade/${id}`,
      method: 'delete'
    })
  },
  batchDelete(ids) {
    return request({
      url: '/trade/batch',
      method: 'delete',
      data: ids
    })
  },
  upload(data) {
    return request({
      url: '/trade/upload',
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export const newsApi = {
  getPage(params) {
    return request({
      url: '/news/page',
      method: 'get',
      params
    })
  },
  getDetail(id) {
    return request({
      url: `/news/${id}`,
      method: 'get'
    })
  },
  add(data) {
    return request({
      url: '/news',
      method: 'post',
      data
    })
  },
  update(data) {
    return request({
      url: '/news',
      method: 'put',
      data
    })
  },
  delete(id) {
    return request({
      url: `/news/${id}`,
      method: 'delete'
    })
  }
}

export const statsApi = {
  getOverview() {
    return request({
      url: '/stats/overview',
      method: 'get'
    })
  },
  getTrend(params) {
    return request({
      url: '/stats/trend',
      method: 'get',
      params
    })
  },
  getTradeModeRatio() {
    return request({
      url: '/stats/trade-mode-ratio',
      method: 'get'
    })
  },
  getCountryRatio() {
    return request({
      url: '/stats/country-ratio',
      method: 'get'
    })
  }
}

export const aiApi = {
  ask(data) {
    return request({
      url: '/ai/ask',
      method: 'post',
      data
    })
  },
  getHistory(params) {
    return request({
      url: '/ai/history',
      method: 'get',
      params
    })
  }
}

export const knowledgeGraphApi = {
  generate(data) {
    return request({
      url: '/knowledge-graph/generate',
      method: 'post',
      data
    })
  },
  saveHistory(data) {
    return request({
      url: '/knowledge-graph/history',
      method: 'post',
      data
    })
  },
  getHistory(params) {
    return request({
      url: '/knowledge-graph/history',
      method: 'get',
      params
    })
  },
  deleteHistory(id, params) {
    return request({
      url: `/knowledge-graph/history/${id}`,
      method: 'delete',
      params
    })
  },
  getGraph(historyId) {
    return request({
      url: `/knowledge-graph/${historyId}`,
      method: 'get'
    })
  },
  getProgress(historyId) {
    return request({
      url: `/knowledge-graph/progress/${historyId}`,
      method: 'get'
    })
  }
}
