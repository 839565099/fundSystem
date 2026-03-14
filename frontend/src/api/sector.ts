import api from './index'
import type { Result } from '../types'
import type { SectorVO, SectorHistoryVO, SectorStockVO, SectorQueryDTO } from '../types/sector'

const handleResponse = <T>(response: { data: Result<T> }): T => {
  const res = response.data
  if (res.code !== 200) {
    throw new Error(res.message || '请求失败')
  }
  return res.data
}

export const sectorApi = {
  // 获取板块列表
  getList: async (params: SectorQueryDTO) => {
    const response = await api.get<Result<SectorVO[]>>('/sector/list', { params })
    return handleResponse(response)
  },

  // 获取板块类型列表
  getTypes: async () => {
    const response = await api.get<Result<string[]>>('/sector/types')
    return handleResponse(response)
  },

  // 获取板块详情
  getDetail: async (code: string) => {
    const response = await api.get<Result<SectorVO>>(`/sector/${code}`)
    return handleResponse(response)
  },

  // 获取板块历史K线
  getHistory: async (code: string, period: string = 'month') => {
    const response = await api.get<Result<SectorHistoryVO>>(`/sector/${code}/history`, {
      params: { period }
    })
    return handleResponse(response)
  },

  // 获取板块成分股
  getStocks: async (code: string) => {
    const response = await api.get<Result<SectorStockVO[]>>(`/sector/${code}/stocks`)
    return handleResponse(response)
  }
}
