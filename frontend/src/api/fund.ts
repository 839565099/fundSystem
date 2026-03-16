import api from './index'
import type { Fund, FundDetailVO, FundNavHistoryVO, PageResult, FundSearchDTO, UserFavorite, FundCompareVO, FundManagerVO, MarketDataVO, FundNews, NewsSentimentOverview, Result } from '../types'

const handleResponse = <T>(response: { data: Result<T> }): T => {
  const res = response.data
  if (res.code !== 200) {
    throw new Error(res.message || '请求失败')
  }
  return res.data
}

const fundBaseApi = {
  search: async (params: FundSearchDTO) => {
    const response = await api.get<Result<PageResult<Fund>>>('/fund/search', { params })
    return handleResponse(response)
  },

  searchByKeyword: async (keyword: string, limit: number = 10) => {
    const response = await api.get<Result<Fund[]>>('/fund/search/keyword', { params: { keyword, limit } })
    return handleResponse(response)
  },

  getDetail: async (fundCode: string) => {
    const response = await api.get<Result<FundDetailVO>>(`/fund/detail/${fundCode}`)
    return handleResponse(response)
  },

  getNavHistory: async (fundCode: string, period: string = 'month') => {
    const response = await api.get<Result<FundNavHistoryVO[]>>(`/fund/nav-history/${fundCode}`, { params: { period } })
    return handleResponse(response)
  },

  getRanking: async (rankingType: string, period: string, pageNum: number, pageSize: number) => {
    const response = await api.get<Result<PageResult<Fund>>>('/fund/ranking', {
      params: { rankingType, period, pageNum, pageSize }
    })
    return handleResponse(response)
  },

  getFundTypes: async () => {
    const response = await api.get<Result<string[]>>('/fund/types')
    return handleResponse(response)
  },

  getFundCompanies: async () => {
    const response = await api.get<Result<string[]>>('/fund/companies')
    return handleResponse(response)
  },

  getManager: async (fundCode: string) => {
    const response = await api.get<Result<FundManagerVO[]>>(`/fund-info/managers/${fundCode}`)
    return handleResponse(response)
  },
}

export const favoriteApi = {
  getList: async () => {
    const response = await api.get<Result<UserFavorite[]>>('/favorite/list')
    return handleResponse(response)
  },

  add: async (fundCode: string) => {
    const response = await api.post<Result<string>>('/favorite/add', null, { params: { fundCode } })
    return handleResponse(response)
  },

  remove: async (fundCode: string) => {
    const response = await api.delete<Result<string>>(`/favorite/remove/${fundCode}`)
    return handleResponse(response)
  },

  check: async (fundCode: string) => {
    const response = await api.get<Result<boolean>>('/favorite/check', { params: { fundCode } })
    return handleResponse(response)
  },
}

export const marketApi = {
  getMarketData: async () => {
    const response = await api.get<Result<MarketDataVO[]>>('/market/data')
    return handleResponse(response)
  },

  getMarketHistory: async (marketCode: string, period: string = 'month') => {
    const response = await api.get<Result<any[]>>(`/market/history/${marketCode}`, { params: { period } })
    return handleResponse(response)
  },
}

export const newsApi = {
  getList: async (pageNum: number = 1, pageSize: number = 10) => {
    const response = await api.get<Result<PageResult<FundNews>>>('/news/list', { params: { pageNum, pageSize } })
    return handleResponse(response)
  },

  getDetail: async (id: number) => {
    const response = await api.get<Result<FundNews>>(`/news/detail/${id}`)
    return handleResponse(response)
  },

  search: async (params: { keyword?: string; newsType?: string; sentiment?: string; pageNum: number; pageSize: number }) => {
    const response = await api.get<Result<PageResult<FundNews>>>('/news/search', { params })
    return handleResponse(response)
  },

  getHot: async (limit: number = 5) => {
    const response = await api.get<Result<FundNews[]>>('/news/hot', { params: { limit } })
    return handleResponse(response)
  },

  getRelated: async (id: number, limit: number = 5) => {
    const response = await api.get<Result<FundNews[]>>(`/news/related/${id}`, { params: { limit } })
    return handleResponse(response)
  },

  getTypes: async () => {
    const response = await api.get<Result<string[]>>('/news/types')
    return handleResponse(response)
  },

  getSentimentOverview: async (fundCode?: string) => {
    const response = await api.get<Result<NewsSentimentOverview>>('/news/sentiment/overview', { params: { fundCode } })
    return handleResponse(response)
  },
}

export const compareApi = {
  compare: async (fundCodes: string[]) => {
    const response = await api.post<Result<FundCompareVO[]>>('/compare/funds', fundCodes)
    return handleResponse(response)
  },
}

export const exportApi = {
  exportFavorites: (format: string) =>
    api.get('/export/favorites', { params: { format }, responseType: 'blob' }),

  exportFund: (fundCode: string, format: string) =>
    api.get(`/export/fund/${fundCode}`, { params: { format }, responseType: 'blob' }),
}

// 预警相关 API
export const alertApi = {
  getRules: async () => {
    const response = await api.get<Result<any[]>>('/alert/rules')
    return handleResponse(response)
  },

  createRule: async (data: any) => {
    const response = await api.post<Result<any>>('/alert/rule', data)
    return handleResponse(response)
  },

  updateRule: async (id: number, data: any) => {
    const response = await api.put<Result<any>>(`/alert/rule/${id}`, data)
    return handleResponse(response)
  },

  deleteRule: async (id: number) => {
    const response = await api.delete<Result<void>>(`/alert/rule/${id}`)
    return handleResponse(response)
  },

  toggleRule: async (id: number, status: number) => {
    const response = await api.put<Result<void>>(`/alert/rule/${id}/toggle`, null, { params: { status } })
    return handleResponse(response)
  },

  getHistory: async (isRead?: number, limit: number = 50) => {
    const response = await api.get<Result<any[]>>('/alert/history', { params: { isRead, limit } })
    return handleResponse(response)
  },

  getUnreadCount: async () => {
    const response = await api.get<Result<number>>('/alert/unread-count')
    return handleResponse(response)
  },

  markAsRead: async (ids: number[]) => {
    const response = await api.post<Result<void>>('/alert/mark-read', ids)
    return handleResponse(response)
  },

  markAllAsRead: async () => {
    const response = await api.post<Result<void>>('/alert/mark-all-read')
    return handleResponse(response)
  },
}

// 投资组合相关 API
export const portfolioApi = {
  getList: async () => {
    const response = await api.get<Result<any[]>>('/portfolio/list')
    return handleResponse(response)
  },

  getDetail: async (id: number) => {
    const response = await api.get<Result<any>>(`/portfolio/${id}`)
    return handleResponse(response)
  },

  create: async (data: any) => {
    const response = await api.post<Result<any>>('/portfolio', data)
    return handleResponse(response)
  },

  update: async (id: number, data: any) => {
    const response = await api.put<Result<any>>(`/portfolio/${id}`, data)
    return handleResponse(response)
  },

  delete: async (id: number) => {
    const response = await api.delete<Result<void>>(`/portfolio/${id}`)
    return handleResponse(response)
  },

  setDefault: async (id: number) => {
    const response = await api.post<Result<void>>(`/portfolio/${id}/set-default`)
    return handleResponse(response)
  },

  getDefault: async () => {
    const response = await api.get<Result<any>>('/portfolio/default')
    return handleResponse(response)
  },

  addItem: async (portfolioId: number, data: any) => {
    const response = await api.post<Result<any>>(`/portfolio/${portfolioId}/item`, data)
    return handleResponse(response)
  },

  updateItem: async (portfolioId: number, itemId: number, data: any) => {
    const response = await api.put<Result<any>>(`/portfolio/${portfolioId}/item/${itemId}`, data)
    return handleResponse(response)
  },

  deleteItem: async (portfolioId: number, itemId: number) => {
    const response = await api.delete<Result<void>>(`/portfolio/${portfolioId}/item/${itemId}`)
    return handleResponse(response)
  },

  refresh: async (id: number) => {
    const response = await api.post<Result<void>>(`/portfolio/${id}/refresh`)
    return handleResponse(response)
  },
}

// AI 助手相关 API
export const aiApi = {
  getSessions: async () => {
    const response = await api.get<Result<any[]>>('/ai/sessions')
    return handleResponse(response)
  },

  createSession: async () => {
    const response = await api.post<Result<any>>('/ai/session')
    return handleResponse(response)
  },

  deleteSession: async (sessionId: string) => {
    const response = await api.delete<Result<void>>(`/ai/session/${sessionId}`)
    return handleResponse(response)
  },

  getHistory: async (sessionId: string) => {
    const response = await api.get<Result<any[]>>(`/ai/history/${sessionId}`)
    return handleResponse(response)
  },

  sendMessage: async (sessionId: string, message: string) => {
    const response = await api.post<Result<any>>('/ai/chat', { sessionId, message })
    return handleResponse(response)
  },
}

// 高级分析相关 API
export const analyticsApi = {
  getSharpeRatio: async (fundCode: string, period: string = 'year') => {
    const response = await api.get<Result<any>>(`/analytics/sharpe/${fundCode}`, { params: { period } })
    return handleResponse(response)
  },

  getMaxDrawdown: async (fundCode: string, period: string = 'year') => {
    const response = await api.get<Result<any>>(`/analytics/drawdown/${fundCode}`, { params: { period } })
    return handleResponse(response)
  },

  getVolatility: async (fundCode: string, period: string = 'year') => {
    const response = await api.get<Result<any>>(`/analytics/volatility/${fundCode}`, { params: { period } })
    return handleResponse(response)
  },

  getCorrelation: async (data: { fundCodes: string[], period: string }) => {
    const response = await api.post<Result<any>>('/analytics/correlation', data)
    return handleResponse(response)
  },

  simulateDIP: async (data: { fundCode: string, amount: number, frequency: string, startDate: string }) => {
    const response = await api.post<Result<any>>('/analytics/dip-simulate', data)
    return handleResponse(response)
  },

  getAnalyticsReport: async (fundCode: string) => {
    const response = await api.get<Result<any>>(`/analytics/report/${fundCode}`)
    return handleResponse(response)
  },
}

// 兼容旧代码的统一导出
export const fundApi = {
  ...fundBaseApi,
  ...favoriteApi,
  ...marketApi,
  ...compareApi,
  ...exportApi,
  ...analyticsApi,

  // 新闻相关方法（单独导出，避免覆盖 fundBaseApi.getDetail）
  getNewsList: newsApi.getList,
  getNewsDetail: newsApi.getDetail,
  searchNews: newsApi.search,
  getHotNews: newsApi.getHot,
  getRelatedNews: newsApi.getRelated,
  getNewsTypes: newsApi.getTypes,
  getSentimentOverview: newsApi.getSentimentOverview,

  // 预警
  getAlertRules: alertApi.getRules,
  createAlertRule: alertApi.createRule,
  updateAlertRule: alertApi.updateRule,
  deleteAlertRule: alertApi.deleteRule,
  toggleAlertRule: alertApi.toggleRule,
  getAlertHistory: alertApi.getHistory,
  getUnreadAlertCount: alertApi.getUnreadCount,
  markAlertsAsRead: alertApi.markAsRead,
  markAllAlertsAsRead: alertApi.markAllAsRead,

  // 组合
  getPortfolios: portfolioApi.getList,
  getPortfolioDetail: portfolioApi.getDetail,
  createPortfolio: portfolioApi.create,
  updatePortfolio: portfolioApi.update,
  deletePortfolio: portfolioApi.delete,
  setDefaultPortfolio: portfolioApi.setDefault,
  getDefaultPortfolio: portfolioApi.getDefault,
  addPortfolioItem: portfolioApi.addItem,
  updatePortfolioItem: portfolioApi.updateItem,
  deletePortfolioItem: portfolioApi.deleteItem,
  refreshPortfolio: portfolioApi.refresh,

  // AI
  getAISessions: aiApi.getSessions,
  createAISession: aiApi.createSession,
  deleteAISession: aiApi.deleteSession,
  getAIChatHistory: aiApi.getHistory,
  sendAIMessage: aiApi.sendMessage,

  // 收藏（兼容旧名称）
  getFavorites: favoriteApi.getList,

  // Dashboard 需要的额外方法
  getHotFunds: async (limit: number = 10) => {
    const response = await api.get<Result<any[]>>('/recommend/hot', { params: { limit } })
    return handleResponse(response)
  },
  getHotRecommend: async (limit: number = 10) => {
    const response = await api.get<Result<any[]>>('/recommend/hot', { params: { limit } })
    return handleResponse(response)
  },
  getPersonalizedRecommend: async (limit: number = 10) => {
    const response = await api.get<Result<any[]>>('/recommend/personalized', { params: { limit } })
    return handleResponse(response)
  },
  recordBehavior: async (fundCode: string, behaviorType: string, dwellTime: number = 0) => {
    const response = await api.post<Result<void>>('/recommend/behavior', null, {
      params: { fundCode, behaviorType, dwellTime }
    })
    return handleResponse(response)
  },
}
