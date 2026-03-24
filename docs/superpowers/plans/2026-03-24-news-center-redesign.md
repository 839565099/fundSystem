# 资讯中心重新设计 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重新设计资讯中心，实现新闻智能分类、完善详情内容、突出来源展示

**Architecture:** 后端增强爬虫服务实现智能分类和全文抓取，前端改造列表页和详情页展示

**Tech Stack:** Spring Boot + MyBatis-Plus (后端), Vue 3 + Naive UI (前端)

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `backend/.../NewsCrawlerService.java` | 修改 | 增加智能分类和全文抓取 |
| `frontend/src/views/News.vue` | 修改 | 固定分类Tab、来源图标 |
| `frontend/src/views/NewsDetail.vue` | 修改 | 来源图标、原文按钮突出 |

---

## Task 1: 后端智能分类

**Files:**
- Modify: `backend/src/main/java/com/fund/external/NewsCrawlerService.java`

- [ ] **Step 1: 添加分类常量和分类方法**

在 `NewsCrawlerService.java` 中添加：

```java
// 分类关键词映射
private static final Map<String, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();

static {
    CATEGORY_KEYWORDS.put("基金动态", new String[]{"基金", "净值", "申购", "赎回", "基金经理", "ETF", "LOF", "QDII"});
    CATEGORY_KEYWORDS.put("政策法规", new String[]{"政策", "监管", "证监会", "央行", "银保监", "国务院", "法规", "新规"});
    CATEGORY_KEYWORDS.put("市场分析", new String[]{"市场", "行情", "大盘", "指数", "A股", "沪指", "深成指", "创业板"});
    CATEGORY_KEYWORDS.put("行业动态", new String[]{"行业", "板块", "概念", "龙头", "产业链", "新能源", "半导体", "医药"});
    CATEGORY_KEYWORDS.put("资金流向", new String[]{"资金", "流入", "流出", "主力", "北向", "南向", "外资", "机构"});
}

/**
 * 智能分类新闻
 */
private String classifyNews(String title, String content) {
    String text = (title != null ? title : "") + " " + (content != null ? content : "");

    for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
        String category = entry.getKey();
        String[] keywords = entry.getValue();
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return category;
            }
        }
    }
    return "财经要闻";
}
```

- [ ] **Step 2: 在爬虫方法中应用分类**

修改 `crawlSinaFinanceNews()` 方法，将 `news.setNewsType("财经要闻");` 替换为：

```java
news.setNewsType(classifyNews(news.getTitle(), news.getSummary()));
```

修改 `crawlEastmoneyNews()` 方法，将 `news.setNewsType("市场分析");` 替换为：

```java
news.setNewsType(classifyNews(news.getTitle(), news.getSummary()));
```

- [ ] **Step 3: 编译验证**

```bash
cd /Applications/IDEAPROJECT/github/fundSystem/backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/fund/external/NewsCrawlerService.java
git commit -m "feat(news): 添加新闻智能分类功能

- 根据标题和内容关键词自动分类
- 支持基金动态、政策法规、市场分析、行业动态、资金流向、财经要闻

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 后端全文抓取增强

**Files:**
- Modify: `backend/src/main/java/com/fund/external/NewsCrawlerService.java`

- [ ] **Step 1: 添加Jsoup依赖**

检查 `pom.xml` 是否有 jsoup，如果没有则添加：

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
```

- [ ] **Step 2: 添加全文抓取方法**

在 `NewsCrawlerService.java` 中添加：

```java
/**
 * 尝试抓取新闻全文
 */
private String fetchFullContent(String url, String source) {
    if (url == null || url.isEmpty()) {
        return null;
    }

    try {
        // 设置超时和User-Agent
        org.jsoup.Connection connection = org.jsoup.Jsoup.connect(url)
                .timeout(8000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        org.jsoup.nodes.Document doc = connection.get();

        // 根据来源选择不同的提取策略
        String content = null;
        if ("新浪财经".equals(source)) {
            content = extractSinaContent(doc);
        } else if ("东方财富".equals(source)) {
            content = extractEastmoneyContent(doc);
        }

        // 通用提取：尝试获取article标签或主要内容区
        if (content == null || content.length() < 100) {
            content = extractGenericContent(doc);
        }

        return content;

    } catch (Exception e) {
        log.debug("抓取全文失败: {} - {}", url, e.getMessage());
        return null;
    }
}

private String extractSinaContent(org.jsoup.nodes.Document doc) {
    // 新浪财经文章内容通常在 #artibody 或 .article-content
    org.jsoup.nodes.Element article = doc.selectFirst("#artibody");
    if (article != null) {
        return article.text();
    }
    return null;
}

private String extractEastmoneyContent(org.jsoup.nodes.Document doc) {
    // 东方财富文章内容通常在 .Body 或 #ContentBody
    org.jsoup.nodes.Element article = doc.selectFirst(".Body");
    if (article == null) {
        article = doc.selectFirst("#ContentBody");
    }
    if (article != null) {
        return article.text();
    }
    return null;
}

private String extractGenericContent(org.jsoup.nodes.Document doc) {
    // 通用提取：尝试article标签
    org.jsoup.nodes.Element article = doc.selectFirst("article");
    if (article != null && article.text().length() > 100) {
        return article.text();
    }

    // 尝试常见的正文class
    String[] selectors = {".content", ".article-content", ".post-content", ".entry-content"};
    for (String selector : selectors) {
        org.jsoup.nodes.Element el = doc.selectFirst(selector);
        if (el != null && el.text().length() > 100) {
            return el.text();
        }
    }

    return null;
}
```

- [ ] **Step 3: 在保存新闻时尝试抓取全文**

修改 `saveNewsIfNotExists()` 方法，在 `newsSentimentAnalyzeService.analyzeAndFill(news)` 之前添加：

```java
// 尝试抓取全文
if ((news.getContent() == null || news.getContent().length() < 100) && news.getOriginalUrl() != null) {
    String fullContent = fetchFullContent(news.getOriginalUrl(), news.getSource());
    if (fullContent != null && fullContent.length() > 100) {
        news.setContent(fullContent);
        if (news.getSummary() == null || news.getSummary().isEmpty()) {
            // 截取前200字作为摘要
            news.setSummary(fullContent.length() > 200 ? fullContent.substring(0, 200) + "..." : fullContent);
        }
    }
}
```

- [ ] **Step 4: 添加必要的import**

```java
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.LinkedHashMap;
import java.util.Map;
```

- [ ] **Step 5: 编译验证**

```bash
cd /Applications/IDEAPROJECT/github/fundSystem/backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/fund/external/NewsCrawlerService.java backend/pom.xml
git commit -m "feat(news): 添加新闻全文抓取功能

- 使用Jsoup抓取原文正文
- 支持新浪财经、东方财富特定提取
- 通用提取作为fallback

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 前端资讯列表页改造

**Files:**
- Modify: `frontend/src/views/News.vue`

- [ ] **Step 1: 修改分类Tab为固定分类**

将 `category-tabs` 部分的代码替换为：

```vue
<!-- 分类标签 -->
<div class="category-tabs card">
  <div class="tabs-wrapper">
    <n-radio-group v-model:value="selectedType" @update:value="handleTypeChange">
      <n-radio-button value="">全部</n-radio-button>
      <n-radio-button value="财经要闻">财经要闻</n-radio-button>
      <n-radio-button value="基金动态">基金动态</n-radio-button>
      <n-radio-button value="市场分析">市场分析</n-radio-button>
      <n-radio-button value="政策法规">政策法规</n-radio-button>
      <n-radio-button value="行业动态">行业动态</n-radio-button>
      <n-radio-button value="资金流向">资金流向</n-radio-button>
    </n-radio-group>
  </div>
  <div class="sentiment-filter">
    <span class="filter-label">情感筛选:</span>
    <n-button
      :type="selectedSentiment === '' ? 'primary' : 'default'"
      size="small"
      @click="selectedSentiment = ''; loadNews()"
    >全部</n-button>
    <n-button
      :type="selectedSentiment === 'BULLISH' ? 'success' : 'default'"
      size="small"
      @click="selectedSentiment = 'BULLISH'; loadNews()"
    >利好</n-button>
    <n-button
      :type="selectedSentiment === 'BEARISH' ? 'error' : 'default'"
      size="small"
      @click="selectedSentiment = 'BEARISH'; loadNews()"
    >利空</n-button>
  </div>
</div>
```

- [ ] **Step 2: 删除newsTypes相关代码**

删除以下代码：
- `const newsTypes = ref<string[]>([])` 声明
- `loadTypes()` 函数
- `onMounted` 中的 `loadTypes()` 调用

- [ ] **Step 3: 添加来源图标组件**

在 script 部分添加：

```typescript
// 来源图标映射
const getSourceIcon = (source: string) => {
  if (source?.includes('新浪')) {
    return 'sina'
  } else if (source?.includes('东方财富') || source?.includes('东财')) {
    return 'eastmoney'
  }
  return 'default'
}
```

- [ ] **Step 4: 修改新闻卡片展示来源图标**

将新闻卡片的 `card-meta` 部分修改为：

```vue
<div class="card-meta">
  <span class="source">
    <span v-if="getSourceIcon(item.source) === 'sina'" class="source-icon sina">
      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
        <path d="M9.197 15.566c1.73 1.09 3.743 1.362 5.314.705 1.569-.657 2.512-2.057 2.512-3.742 0-1.684-.943-3.084-2.512-3.741-1.571-.657-3.584-.386-5.314.705-1.73 1.09-2.782 2.71-2.782 4.446 0 1.735 1.052 3.355 2.782 4.446l-.5.785zm-1.5-7.498c2.443-1.542 5.437-1.912 7.8-.924 2.362.988 3.816 3.127 3.816 5.578 0 2.45-1.454 4.59-3.816 5.578-2.363.988-5.357.618-7.8-.924-2.442-1.541-3.935-3.893-3.935-6.437 0-2.544 1.493-4.896 3.935-6.437l.5.786c-2.026 1.277-3.263 3.226-3.263 5.312s1.237 4.035 3.263 5.312c2.026 1.277 4.51 1.58 6.467.766 1.958-.814 3.16-2.578 3.16-4.54 0-1.962-1.202-3.726-3.16-4.54-1.957-.814-4.44-.51-6.467.766l-.5-.786z"/>
      </svg>
    </span>
    <span v-else-if="getSourceIcon(item.source) === 'eastmoney'" class="source-icon eastmoney">
      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
      </svg>
    </span>
    <n-icon v-else size="14"><LocationOutline /></n-icon>
    <span class="source-name">{{ item.source || '未知来源' }}</span>
  </span>
  <span class="time">
    <n-icon size="14"><TimeOutline /></n-icon>
    {{ formatTime(item.publishTime || '') }}
  </span>
  <n-tag
    v-if="item.sentiment"
    :type="getSentimentType(item.sentiment)"
    size="small"
    :bordered="false"
  >
    {{ getSentimentLabel(item.sentiment) }}
  </n-tag>
</div>
```

- [ ] **Step 5: 添加来源图标样式**

在 style 部分添加：

```css
.source {
  display: flex;
  align-items: center;
  gap: 6px;
}

.source-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
}

.source-icon.sina {
  color: #ff6600;
}

.source-icon.eastmoney {
  color: #c00;
}

.source-name {
  color: var(--primary-color);
  font-weight: 500;
}

.tabs-wrapper {
  flex: 1;
  overflow-x: auto;
}

.tabs-wrapper :deep(.n-radio-group) {
  flex-wrap: nowrap;
}
```

- [ ] **Step 6: 验证编译**

```bash
cd /Applications/IDEAPROJECT/github/fundSystem/frontend && npm run build 2>&1 | head -20
```

Expected: 构建成功，无错误

- [ ] **Step 7: 提交**

```bash
git add frontend/src/views/News.vue
git commit -m "feat(frontend): 资讯列表页改造

- 使用固定分类Tab替代动态分类
- 添加新浪/东财来源图标
- 优化来源展示样式

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 前端详情页改造

**Files:**
- Modify: `frontend/src/views/NewsDetail.vue`

- [ ] **Step 1: 添加来源图标组件**

在 script 部分添加：

```typescript
// 来源图标映射
const getSourceIcon = (source: string) => {
  if (source?.includes('新浪')) {
    return 'sina'
  } else if (source?.includes('东方财富') || source?.includes('东财')) {
    return 'eastmoney'
  }
  return 'default'
}
```

- [ ] **Step 2: 修改来源展示区域**

将 `news-meta` 部分的 source 元素修改为：

```vue
<div class="news-meta">
  <div class="meta-left">
    <span v-if="news.source" class="meta-item source-item">
      <span v-if="getSourceIcon(news.source) === 'sina'" class="source-icon sina">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
          <path d="M9.197 15.566c1.73 1.09 3.743 1.362 5.314.705 1.569-.657 2.512-2.057 2.512-3.742 0-1.684-.943-3.084-2.512-3.741-1.571-.657-3.584-.386-5.314.705-1.73 1.09-2.782 2.71-2.782 4.446 0 1.735 1.052 3.355 2.782 4.446l-.5.785zm-1.5-7.498c2.443-1.542 5.437-1.912 7.8-.924 2.362.988 3.816 3.127 3.816 5.578 0 2.45-1.454 4.59-3.816 5.578-2.363.988-5.357.618-7.8-.924-2.442-1.541-3.935-3.893-3.935-6.437 0-2.544 1.493-4.896 3.935-6.437l.5.786z"/>
        </svg>
      </span>
      <span v-else-if="getSourceIcon(news.source) === 'eastmoney'" class="source-icon eastmoney">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
        </svg>
      </span>
      <n-icon v-else size="16"><LocationOutline /></n-icon>
      <span class="source-name">{{ news.source }}</span>
    </span>
    <span v-if="news.author" class="meta-item">
      <n-icon size="14"><PersonOutline /></n-icon>
      {{ news.author }}
    </span>
    <span v-if="news.publishTime" class="meta-item">
      <n-icon size="14"><TimeOutline /></n-icon>
      {{ formatTime(news.publishTime) }}
    </span>
    <span v-if="news.viewCount" class="meta-item">
      <n-icon size="14"><EyeOutline /></n-icon>
      {{ news.viewCount }} 阅读
    </span>
  </div>
  <!-- ... 其余保持不变 -->
</div>
```

- [ ] **Step 3: 增强"查看原文"按钮样式**

将 `original-link` 部分修改为：

```vue
<!-- 原文链接 -->
<div v-if="news.originalUrl" class="original-link">
  <n-button type="primary" @click="openOriginal(news.originalUrl)">
    <template #icon><n-icon><OpenOutline /></n-icon></template>
    查看原文
  </n-button>
</div>
```

- [ ] **Step 4: 内容不足时增加提示**

将文章内容部分修改为：

```vue
<!-- 文章内容 -->
<div class="news-content card">
  <div v-if="news.content && news.content.length >= 100" class="content-text" v-html="news.content"></div>
  <div v-else-if="news.summary" class="content-text">
    {{ news.summary }}
    <div v-if="news.content && news.content.length < 100 && news.content.length > 0" class="content-note">
      <n-icon size="16"><InformationCircleOutline /></n-icon>
      正文内容较少，建议查看原文获取完整内容
    </div>
  </div>
  <n-empty v-else description="暂无详细内容">
    <template #extra>
      <p class="no-content-tip">该资讯仅提供标题信息，请查看原文获取完整内容</p>
      <n-button v-if="news.originalUrl" type="primary" @click="openOriginal(news.originalUrl)">
        查看原文
      </n-button>
    </template>
  </n-empty>

  <!-- 原文链接 -->
  <div v-if="news.originalUrl" class="original-link">
    <n-button type="primary" @click="openOriginal(news.originalUrl)">
      <template #icon><n-icon><OpenOutline /></n-icon></template>
      查看原文
    </n-button>
  </div>
</div>
```

- [ ] **Step 5: 添加InformationCircleOutline图标导入**

```typescript
import {
  ArrowBackOutline, LocationOutline, PersonOutline, TimeOutline,
  EyeOutline, StarOutline, ShareSocialOutline, OpenOutline,
  DocumentsOutline, ListOutline, InformationCircleOutline
} from '@vicons/ionicons5'
```

- [ ] **Step 6: 添加样式**

在 style 部分添加：

```css
.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-item .source-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
}

.source-item .source-icon.sina {
  color: #ff6600;
}

.source-item .source-icon.eastmoney {
  color: #c00;
}

.source-item .source-name {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 14px;
}

.content-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
}

.no-content-tip {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 8px;
  margin-bottom: 16px;
}
```

- [ ] **Step 7: 验证编译**

```bash
cd /Applications/IDEAPROJECT/github/fundSystem/frontend && npm run build 2>&1 | head -20
```

Expected: 构建成功，无错误

- [ ] **Step 8: 提交**

```bash
git add frontend/src/views/NewsDetail.vue
git commit -m "feat(frontend): 资讯详情页改造

- 添加新浪/东财来源图标
- 增强"查看原文"按钮样式
- 内容不足时显示提示引导

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 重新抓取新闻测试

**Files:**
- 无文件修改，仅测试

- [ ] **Step 1: 重启后端服务**

```bash
# 停止旧服务
pkill -f "fund-system-1.0.0.jar" 2>/dev/null || true

# 编译打包
cd /Applications/IDEAPROJECT/github/fundSystem/backend && mvn clean package -DskipTests -q

# 启动服务
nohup java -jar target/fund-system-1.0.0.jar > backend.log 2>&1 &
```

- [ ] **Step 2: 清空旧新闻并重新抓取**

```bash
# 调用API清空新闻
curl -X DELETE http://127.0.0.1:8080/api/news/all

# 调用API重新抓取
curl -X POST http://127.0.0.1:8080/api/news/crawl
```

- [ ] **Step 3: 验证分类效果**

```bash
# 查看各类别新闻数量
curl -s "http://127.0.0.1:8080/api/news/list?pageNum=1&pageSize=50" | jq '.data.records[].newsType' | sort | uniq -c
```

Expected: 各分类都有新闻分布

- [ ] **Step 4: 验证全文抓取**

```bash
# 查看有内容的新闻数量
curl -s "http://127.0.0.1:8080/api/news/list?pageNum=1&pageSize=50" | jq '[.data.records[] | select(.content != null and .content != "")] | length'
```

Expected: 大部分新闻有内容

---

## 最终提交

- [ ] **Step 1: 确认所有改动已提交**

```bash
git status
```

Expected: working tree clean

- [ ] **Step 2: 推送到远程**

```bash
git push origin master
```
