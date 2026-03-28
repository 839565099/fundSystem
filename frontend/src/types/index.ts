export interface User {
  id: number
  username: string
  email?: string
  phone?: string
  nickname?: string
  avatar?: string
  status: number
}

export interface LoginDTO {
  username: string
  password: string
}

export interface RegisterDTO {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

export interface UserVO {
  id: number
  username: string
  email?: string
  phone?: string
  nickname?: string
  avatar?: string
  status: number
  role: 'ADMIN' | 'USER'
  createTime?: string
}

export interface Fund {
  id: number
  fundCode: string
  fundName: string
  fundType?: string
  sector?: string
  fundCompany?: string
  fundScale?: number
  nav?: number
  accNav?: number
  navDate?: string
  dayGrowth?: number
  weekGrowth?: number
  monthGrowth?: number
  threeMonthGrowth?: number
  sixMonthGrowth?: number
  yearGrowth?: number
  totalGrowth?: number
  riskLevel?: number
}

export interface FundNavHistoryVO {
  fundCode: string
  navDate: string
  nav: number
  accNav?: number
  dayGrowth?: number
}

export interface FundSearchDTO {
  keyword?: string
  fundType?: string
  riskLevel?: number
  minScale?: number
  maxScale?: number
  sortBy?: string
  sortOrder?: string
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface FundDetailVO extends Fund {
  establishDate?: string
  minPurchase?: number
  purchaseRate?: number
  redemptionRate?: number
  managementRate?: number
  custodyRate?: number
  status?: number
  riskLevelName?: string
}

export interface UserFavorite {
  id: number
  userId: number
  fundCode: string
  groupName?: string
  createTime: string
  fundName?: string
  nav?: number
  navDate?: string
  dayGrowth?: number
  weekGrowth?: number
  monthGrowth?: number
  threeMonthGrowth?: number
  yearGrowth?: number
}

export interface FundNews {
  id: number
  title: string
  summary?: string
  content?: string
  coverImage?: string
  originalUrl?: string
  source?: string
  author?: string
  newsType?: string
  fundCode?: string
  sentiment?: 'BULLISH' | 'BEARISH' | 'NEUTRAL'
  sentimentScore?: number
  sentimentConfidence?: number
  impactLevel?: number
  publishTime?: string
  viewCount?: number
  status?: number
}

export interface NewsSearchDTO {
  keyword?: string
  type?: string
  sentiment?: string
  pageNum: number
  pageSize: number
}

export interface NewsSentimentOverview {
  bullishCount: number
  bearishCount: number
  neutralCount: number
  bullishPercent: number
  bearishPercent: number
  neutralPercent: number
}

export interface MarketDataVO {
  marketType: string
  marketCode: string
  currentPoint?: number
  changePoint?: number
  changeRatio?: number
  volume?: number
  amount?: number
  highPoint?: number
  lowPoint?: number
  openPoint?: number
  prevClose?: number
}

export interface FundManagerVO {
  managerId: string
  managerName: string
  company?: string
  workYears?: number
  startDate?: string
  totalAsset?: number
  bestReturn?: number
  education?: string
  resume?: string
  photo?: string
  investmentIdea?: string
  fundCount?: number
}

export interface FundHoldingVO {
  id: number
  fundCode: string
  reportDate: string
  stockCode: string
  stockName: string
  holdingRatio: number
  holdingShares?: number
  holdingValue?: number
  holdingType?: string
  dayGrowth?: number
  currentPrice?: number
}

export interface FundCompareVO {
  fundCode: string
  fundName: string
  fundType?: string
  fundCompany?: string
  fundScale?: number
  nav?: number
  dayGrowth?: number
  weekGrowth?: number
  monthGrowth?: number
  threeMonthGrowth?: number
  sixMonthGrowth?: number
  yearGrowth?: number
  totalGrowth?: number
  maxDrawdown?: number
  sharpeRatio?: number
  navHistory?: FundNavHistoryVO[]
}

export interface UserStats {
  favoriteCount: number
  portfolioCount: number
  alertCount: number
  registerDays: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface AlertRuleVO {
  id: number
  alertType: string
  fundCode: string
  fundName?: string
  alertName: string
  alertCondition: string
  threshold: number
  unit?: string
  notifyChannel?: string
  cooldownMinutes?: number
  status: number
  lastTriggeredTime?: string
  triggerCount?: number
  createTime?: string
}

export interface AlertHistoryVO {
  id: number
  alertType: string
  alertTitle: string
  alertMessage: string
  alertValue?: number
  fundCode?: string
  triggeredTime: string
  isRead: number
}

export interface PortfolioItemVO {
  id: number
  fundCode: string
  fundName: string
  fundType?: string
  shares?: number
  amount?: number
  targetRatio?: number
  actualRatio?: number
  buyNav?: number
  currentNav?: number
  currentValue?: number
  profit?: number
  profitRatio?: number
  dayProfit?: number
  dayGrowth?: number
  yesterdayProfit?: number
  yesterdayGrowth?: number
  buyDate?: string
  status?: number
  updateTime?: string
}

export interface AssetAllocationVO {
  type?: string
  name: string
  value?: number
  ratio?: number
  color?: string
}

export interface PortfolioVO {
  id: number
  name: string
  description?: string
  totalAmount?: number
  currentValue?: number
  totalProfit?: number
  totalReturn?: number
  dayProfit?: number
  dayReturn?: number
  yesterdayProfit?: number
  yesterdayReturn?: number
  fundCount?: number
  isDefault?: number
  createTime?: string
  updateTime?: string
  items?: PortfolioItemVO[]
  allocations?: AssetAllocationVO[]
}

export interface PortfolioItemVO {
  id: number
  fundCode: string
  fundName: string
  fundType?: string
  shares?: number
  amount?: number
  targetRatio?: number
  actualRatio?: number
  buyNav?: number
  currentNav?: number
  currentValue?: number
  profit?: number
  profitRatio?: number
  dayProfit?: number
  dayGrowth?: number
  yesterdayProfit?: number
  yesterdayGrowth?: number
  buyDate?: string
  status?: number
  updateTime?: string
}

export interface AssetAllocationVO {
  type?: string
  name: string
  value?: number
  ratio?: number
  color?: string
}
