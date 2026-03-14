/**
 * 板块类型
 */
export type SectorType = 'all' | 'industry' | 'concept' | 'region'

/**
 * 涨跌排序方式
 */
export type SortOrder = 'asc' | 'desc'

/**
 * 板块数据VO
 */
export interface SectorVO {
  code: string           // 板块代码
  name: string           // 板块名称
  type: string           // 板块类型: industry/concept/region

  price: number          // 当前点位
  change: number         // 涨跌额
  changePercent: number  // 今日涨跌幅 %

  weekGrowth?: number    // 周涨跌幅
  monthGrowth?: number   // 月涨跌幅
  threeMonthGrowth?: number
  sixMonthGrowth?: number
  yearGrowth?: number

  volume: number         // 成交额（亿）
  turnover: number       // 换手率

  leadingStock: string   // 领涨股名称
  leadingStockCode: string
  leadingStockGrowth: number

  stockCount: number     // 成分股数量
  upCount: number        // 上涨家数
  downCount: number      // 下跌家数

  updateTime: string     // 数据更新时间
}

/**
 * 板块K线数据项
 */
export interface SectorKLineItem {
  date: string
  open: number
  close: number
  high: number
  low: number
  volume: number
  changePercent: number
}

/**
 * 板块历史数据VO
 */
export interface SectorHistoryVO {
  code: string
  name: string
  history: SectorKLineItem[]
}

/**
 * 板块成分股VO
 */
export interface SectorStockVO {
  code: string
  name: string
  changePercent: number
  price: number
  volume: number
  isLeading: boolean
}

/**
 * 板块查询参数
 */
export interface SectorQueryDTO {
  type?: SectorType
  sortBy?: 'dayGrowth' | 'weekGrowth' | 'monthGrowth' | 'volume' | 'turnover'
  sortOrder?: SortOrder
  keyword?: string
  pageNum?: number
  pageSize?: number
}
