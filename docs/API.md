# 基金实时查看系统 API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token (Bearer Token)
- **数据格式**: JSON

## 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890000
}
```

## 认证相关接口

### 用户注册

- **URL**: `POST /auth/register`
- **认证**: 无需认证

**请求参数**:
```json
{
  "username": "testuser",
  "password": "123456",
  "email": "test@example.com",
  "phone": "13800138000",
  "nickname": "测试用户"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "phone": "13800138000",
    "nickname": "测试用户"
  }
}
```

### 用户登录

- **URL**: `POST /auth/login`
- **认证**: 无需认证

**请求参数**:
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 获取用户信息

- **URL**: `GET /auth/info`
- **认证**: 需要Token

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "phone": "13800138000",
    "nickname": "测试用户"
  }
}
```

## 基金相关接口

### 基金搜索

- **URL**: `GET /fund/search`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词 |
| fundType | String | 否 | 基金类型 |
| riskLevel | Integer | 否 | 风险等级(1-5) |
| minScale | BigDecimal | 否 | 最小规模 |
| maxScale | BigDecimal | 否 | 最大规模 |
| sortBy | String | 否 | 排序字段 |
| sortOrder | String | 否 | 排序方式(asc/desc) |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "fundCode": "000001",
        "fundName": "华夏成长混合",
        "fundType": "混合型",
        "nav": 1.2345,
        "dayGrowth": 1.25,
        "weekGrowth": 2.34,
        "monthGrowth": 5.67,
        "yearGrowth": 15.89,
        "fundScale": 50.23,
        "riskLevel": 3
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 关键词搜索基金

- **URL**: `GET /fund/search/keyword`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| limit | Integer | 否 | 返回数量，默认10 |

### 获取基金详情

- **URL**: `GET /fund/detail/{fundCode}`
- **认证**: 无需认证

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "fundCode": "000001",
    "fundName": "华夏成长混合",
    "fundType": "混合型",
    "fundCompany": "华夏基金管理有限公司",
    "establishDate": "2001-12-18",
    "fundScale": 50.23,
    "nav": 1.2345,
    "accNav": 3.4567,
    "navDate": "2024-01-15",
    "dayGrowth": 1.25,
    "weekGrowth": 2.34,
    "monthGrowth": 5.67,
    "threeMonthGrowth": 8.90,
    "sixMonthGrowth": 12.34,
    "yearGrowth": 15.89,
    "totalGrowth": 245.67,
    "riskLevel": 3,
    "riskLevelName": "中风险",
    "minPurchase": 1.00,
    "purchaseRate": 1.50,
    "redemptionRate": 0.50,
    "managementRate": 1.50,
    "custodyRate": 0.25
  }
}
```

### 获取热门基金

- **URL**: `GET /fund/hot`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回数量，默认10 |

### 获取涨幅榜

- **URL**: `GET /fund/top`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回数量，默认10 |

### 获取净值历史

- **URL**: `GET /fund/nav-history/{fundCode}`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| period | String | 否 | 时间周期(day/week/month/threeMonth/sixMonth/year/threeYear/fiveYear)，默认month |

## 收藏相关接口

### 获取收藏列表

- **URL**: `GET /favorite/list`
- **认证**: 需要Token

### 添加收藏

- **URL**: `POST /favorite/add`
- **认证**: 需要Token

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fundCode | String | 是 | 基金代码 |

### 取消收藏

- **URL**: `DELETE /favorite/remove`
- **认证**: 需要Token

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fundCode | String | 是 | 基金代码 |

### 检查是否收藏

- **URL**: `GET /favorite/check`
- **认证**: 需要Token

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fundCode | String | 是 | 基金代码 |

## 对比相关接口

### 基金对比

- **URL**: `POST /compare/funds`
- **认证**: 无需认证

**请求参数**:
```json
["000001", "000002", "000003"]
```

## 资讯相关接口

### 获取资讯列表

- **URL**: `GET /news/list`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |

### 获取基金相关资讯

- **URL**: `GET /news/fund/{fundCode}`
- **认证**: 无需认证

### 获取资讯详情

- **URL**: `GET /news/detail/{id}`
- **认证**: 无需认证

## 市场行情接口

### 获取市场数据

- **URL**: `GET /market/data`
- **认证**: 无需认证

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "marketType": "上证指数",
      "marketCode": "sh000001",
      "currentPoint": 3089.34,
      "changePoint": 15.67,
      "changeRatio": 0.51,
      "volume": 3.45,
      "amount": 4567.89
    }
  ]
}
```

## 基金信息接口

### 获取基金经理

- **URL**: `GET /fund-info/managers/{fundCode}`
- **认证**: 无需认证

### 获取持仓明细

- **URL**: `GET /fund-info/holdings/{fundCode}`
- **认证**: 无需认证

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回数量，默认10 |

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户已被禁用 |
| 1004 | 用户名已存在 |
| 1005 | 邮箱已被注册 |
| 1006 | 手机号已被注册 |
| 2001 | 基金不存在 |
| 3001 | 已收藏该基金 |
| 3002 | 收藏数量已达上限 |
| 4001 | Token已过期 |
| 4002 | Token无效 |
